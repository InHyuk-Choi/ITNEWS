package com.itnews.backend.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itnews.backend.news.NewsEntity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Crawls IT news from the Naver Search API.
 * If the API credentials are not configured, {@link #crawl()} returns an empty list
 * so the application continues to work without Naver integration.
 */
@Component
public class NaverCrawler implements Crawler {

    private static final Logger log = LoggerFactory.getLogger(NaverCrawler.class);

    private static final String NAVER_SEARCH_URL =
            "https://openapi.naver.com/v1/search/news.json?query=IT+기술+개발자+인공지능+소프트웨어&display=%d&sort=date";
    private static final String SOURCE = "naver";

    // Naver RSS date format: "Mon, 07 Apr 2025 10:00:00 +0900"
    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final String clientId;
    private final String clientSecret;
    private final int articlesPerSource;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NaverCrawler(
            @Value("${app.naver.client-id:}") String clientId,
            @Value("${app.naver.client-secret:}") String clientSecret,
            @Value("${app.crawler.articles-per-source:8}") int articlesPerSource
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.articlesPerSource = articlesPerSource;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Naver";
    }

    @Override
    public List<NewsEntity> crawl() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.debug("[Naver] API credentials not configured, skipping crawl");
            return List.of();
        }

        List<NewsEntity> articles = new ArrayList<>();
        try {
            String url = String.format(NAVER_SEARCH_URL, articlesPerSource);
            Request request = new Request.Builder()
                    .url(url)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("[Naver] API returned HTTP {}", response.code());
                    return articles;
                }
                ResponseBody body = response.body();
                if (body == null) return articles;

                JsonNode root = objectMapper.readTree(body.string());
                JsonNode items = root.path("items");

                if (items.isArray()) {
                    for (JsonNode item : items) {
                        try {
                            NewsEntity entity = toEntity(item);
                            if (entity != null) articles.add(entity);
                        } catch (Exception e) {
                            log.warn("[Naver] Skipping item: {}", e.getMessage());
                        }
                    }
                }
            }
            log.info("[Naver] Crawled {} articles", articles.size());
        } catch (Exception e) {
            log.error("[Naver] Crawl failed: {}", e.getMessage());
        }
        return articles;
    }

    private NewsEntity toEntity(JsonNode item) {
        String link = item.path("link").asText("").trim();
        if (link.isBlank()) return null;

        // Naver wraps titles/descriptions in HTML entities
        String title = Jsoup.parse(item.path("title").asText("")).text().trim();
        if (title.isBlank()) return null;

        String pubDateStr = item.path("pubDate").asText("");
        OffsetDateTime publishedAt;
        try {
            publishedAt = OffsetDateTime.parse(pubDateStr, NAVER_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }

        return NewsEntity.builder()
                .title(title)
                .url(link)
                .source(SOURCE)
                .publishedAt(publishedAt)
                .build();
    }
}
