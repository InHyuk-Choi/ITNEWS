package com.itnews.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Calls the Gemini 1.5 Flash REST API to generate a Korean summary for a news article.
 * If the API key is absent or the call fails, returns null gracefully so the rest
 * of the application continues to function normally.
 */
@Service
public class GeminiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSummaryService.class);

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=%s";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    // 무료 티어: 15 RPM → 최소 4초 간격. Semaphore로 동시 호출 1개 제한
    private final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(1);

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiSummaryService(
            @Value("${app.gemini.api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches article body from URL using Jsoup, truncated to 3000 chars.
     * Returns null if fetch fails (caller falls back to title-only).
     */
    private String fetchContent(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; ITNewsBot/1.0)")
                    .timeout(8000)
                    .get();
            String text = doc.select("p").stream()
                    .map(e -> e.text().trim())
                    .filter(t -> t.length() > 30)
                    .collect(Collectors.joining("\n"));
            if (text.isBlank()) return null;
            return text.length() > 3000 ? text.substring(0, 3000) : text;
        } catch (Exception e) {
            log.debug("Content fetch failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Generates a Korean summary using the article's actual content (fetched from URL).
     * Falls back to title-only if content cannot be fetched.
     *
     * @param title article title
     * @param url   article URL to fetch content from
     * @return Korean summary string, or null if API key is missing / call fails
     */
    public String summarize(String title, String url) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Gemini API key not configured, skipping summarization");
            return null;
        }
        if (title == null || title.isBlank()) {
            return null;
        }

        String content = fetchContent(url);
        String prompt = content != null
                ? "다음 IT 뉴스 기사를 한국어로 3~5문장으로 요약해줘:\n\n제목: " + title + "\n\n본문:\n" + content
                : "다음 IT 뉴스 제목을 바탕으로 한국어로 2~3문장으로 설명해줘:\n" + title;

        try {
            semaphore.acquire();
            try {
                String requestBody = buildRequestBody(prompt);

                Request request = new Request.Builder()
                        .url(String.format(GEMINI_URL, apiKey))
                        .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("Gemini API returned HTTP {}: {}", response.code(), response.message());
                        return null;
                    }
                    ResponseBody body = response.body();
                    if (body == null) return null;
                    return parseResponse(body.string());
                }
            } finally {
                Thread.sleep(4200); // 15 RPM 무료 한도: 4초 간격 유지
                semaphore.release();
            }
        } catch (Exception e) {
            log.warn("Gemini summarization failed for title [{}]: {}", title, e.getMessage());
            return null;
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        // Build minimal Gemini generateContent request JSON
        var root = objectMapper.createObjectNode();
        var contentsArray = objectMapper.createArrayNode();
        var contentNode = objectMapper.createObjectNode();
        var partsArray = objectMapper.createArrayNode();
        var partNode = objectMapper.createObjectNode();
        partNode.put("text", prompt);
        partsArray.add(partNode);
        contentNode.set("parts", partsArray);
        contentsArray.add(contentNode);
        root.set("contents", contentsArray);
        return objectMapper.writeValueAsString(root);
    }

    private String parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode text = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");
            if (text.isMissingNode()) {
                log.warn("Unexpected Gemini response structure");
                return null;
            }
            return text.asText().trim();
        } catch (IOException e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
            return null;
        }
    }
}
