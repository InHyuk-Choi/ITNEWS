package com.itnews.backend.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itnews.backend.news.NewsEntity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Crawls top stories from the Hacker News Firebase API.
 * Fetches the top story IDs, then retrieves details for the first N articles.
 */
@Component
public class HackerNewsCrawler implements Crawler {

    private static final Logger log = LoggerFactory.getLogger(HackerNewsCrawler.class);

    private static final String TOP_STORIES_URL =
            "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM_URL =
            "https://hacker-news.firebaseio.com/v0/item/%d.json";
    private static final String SOURCE = "hackernews";

    private final int articlesPerSource;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HackerNewsCrawler(
            @Value("${app.crawler.articles-per-source:8}") int articlesPerSource
    ) {
        this.articlesPerSource = articlesPerSource;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "HackerNews";
    }

    @Override
    public List<NewsEntity> crawl() {
        List<NewsEntity> articles = new ArrayList<>();
        try {
            List<Long> topIds = fetchTopIds();
            int limit = Math.min(topIds.size(), articlesPerSource);

            for (int i = 0; i < limit; i++) {
                try {
                    NewsEntity article = fetchItem(topIds.get(i));
                    if (article != null) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    log.warn("[HackerNews] Failed to fetch item {}: {}", topIds.get(i), e.getMessage());
                }
            }
            log.info("[HackerNews] Crawled {} articles", articles.size());
        } catch (Exception e) {
            log.error("[HackerNews] Crawl failed: {}", e.getMessage());
        }
        return articles;
    }

    private List<Long> fetchTopIds() throws Exception {
        String json = get(TOP_STORIES_URL);
        JsonNode array = objectMapper.readTree(json);
        List<Long> ids = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode node : array) {
                ids.add(node.asLong());
            }
        }
        return ids;
    }

    private NewsEntity fetchItem(long id) throws Exception {
        String url = String.format(ITEM_URL, id);
        String json = get(url);
        JsonNode item = objectMapper.readTree(json);

        // Skip non-story types and items without a URL
        String type = item.path("type").asText("");
        if (!"story".equals(type)) return null;

        String itemUrl = item.path("url").asText("");
        if (itemUrl.isBlank()) return null;

        String title = item.path("title").asText("").trim();
        if (title.isBlank()) return null;

        long timeEpoch = item.path("time").asLong(0);
        OffsetDateTime publishedAt = timeEpoch > 0
                ? OffsetDateTime.ofInstant(Instant.ofEpochSecond(timeEpoch), ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        return NewsEntity.builder()
                .title(title)
                .url(itemUrl)
                .source(SOURCE)
                .publishedAt(publishedAt)
                .build();
    }

    private String get(String url) throws Exception {
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("HTTP " + response.code() + " for " + url);
            }
            ResponseBody body = response.body();
            if (body == null) throw new RuntimeException("Empty body for " + url);
            return body.string();
        }
    }
}
