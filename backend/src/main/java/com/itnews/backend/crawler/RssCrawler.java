package com.itnews.backend.crawler;

import com.itnews.backend.news.NewsEntity;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Crawls multiple RSS/Atom feeds using the Rome library.
 * Fetches up to N articles per feed. If a feed fails, the error is logged
 * and other feeds continue to be processed.
 */
@Component
public class RssCrawler implements Crawler {

    private static final Logger log = LoggerFactory.getLogger(RssCrawler.class);

    /** Map of source name → RSS feed URL */
    private static final Map<String, String> FEEDS = new LinkedHashMap<>();

    static {
        FEEDS.put("techcrunch",  "https://techcrunch.com/feed/");
        FEEDS.put("venturebeat", "https://venturebeat.com/feed/");
        FEEDS.put("etnews",      "https://rss.etnews.com/Section901.xml");
        FEEDS.put("aitimes",     "https://www.aitimes.com/rss/allArticle.xml");
    }

    private final int articlesPerSource;
    private final OkHttpClient httpClient;

    public RssCrawler(
            @Value("${app.crawler.articles-per-source:8}") int articlesPerSource
    ) {
        this.articlesPerSource = articlesPerSource;
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getName() {
        return "RSS";
    }

    @Override
    public List<NewsEntity> crawl() {
        List<NewsEntity> all = new ArrayList<>();
        for (Map.Entry<String, String> entry : FEEDS.entrySet()) {
            try {
                List<NewsEntity> articles = crawlFeed(entry.getKey(), entry.getValue());
                all.addAll(articles);
                log.info("[RSS][{}] Crawled {} articles", entry.getKey(), articles.size());
            } catch (Exception e) {
                log.error("[RSS][{}] Failed to crawl {}: {}", entry.getKey(), entry.getValue(), e.getMessage());
            }
        }
        return all;
    }

    private List<NewsEntity> crawlFeed(String source, String feedUrl) throws Exception {
        List<NewsEntity> articles = new ArrayList<>();

        Request request = new Request.Builder()
                .url(feedUrl)
                .header("User-Agent", "Mozilla/5.0 ITNewsAggregator/1.0")
                .build();

        byte[] bytes;
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("HTTP " + response.code());
            }
            bytes = response.body().bytes();
        }

        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(bytes))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            List<SyndEntry> entries = feed.getEntries();
            int limit = Math.min(entries.size(), articlesPerSource);

            for (int i = 0; i < limit; i++) {
                SyndEntry entry = entries.get(i);
                try {
                    NewsEntity article = toEntity(entry, source);
                    if (article != null) articles.add(article);
                } catch (Exception e) {
                    log.warn("[RSS][{}] Skipping entry: {}", source, e.getMessage());
                }
            }
        }
        return articles;
    }

    private NewsEntity toEntity(SyndEntry entry, String source) {
        String link = entry.getLink();
        if (link == null || link.isBlank()) return null;

        String title = entry.getTitle();
        if (title == null || title.isBlank()) return null;
        title = Jsoup.parse(title).text(); // strip any HTML in title

        Date pubDate = entry.getPublishedDate() != null
                ? entry.getPublishedDate()
                : entry.getUpdatedDate();
        OffsetDateTime publishedAt = pubDate != null
                ? pubDate.toInstant().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        // Try to extract a thumbnail from enclosures
        String thumbnail = null;
        if (entry.getEnclosures() != null) {
            for (SyndEnclosure enc : entry.getEnclosures()) {
                if (enc.getType() != null && enc.getType().startsWith("image/")) {
                    thumbnail = enc.getUrl();
                    break;
                }
            }
        }
        // Fallback: try og:image from the description HTML
        if (thumbnail == null && entry.getDescription() != null) {
            String desc = entry.getDescription().getValue();
            if (desc != null) {
                var doc = Jsoup.parse(desc);
                var img = doc.selectFirst("img");
                if (img != null) thumbnail = img.attr("src");
            }
        }

        return NewsEntity.builder()
                .title(title)
                .url(link.trim())
                .source(source)
                .thumbnail(thumbnail)
                .publishedAt(publishedAt)
                .build();
    }
}
