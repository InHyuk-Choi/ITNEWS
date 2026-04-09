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
 * IT 뉴스 기사를 Groq API (llama-3.3-70b)로 한국어 요약.
 * API 키 없거나 실패 시 null 반환 → 서비스 정상 동작 유지.
 */
@Service
public class GeminiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSummaryService.class);

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant"; // 무료 TPM 20,000 (70b는 6,000)
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Groq 무료: 30 RPM → 2초 간격으로 여유롭게
    private final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(1);

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiSummaryService(
            @Value("${app.groq.api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

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
            return text.length() > 1500 ? text.substring(0, 1500) : text;
        } catch (Exception e) {
            log.debug("Content fetch failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    public String summarize(String title, String url) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Groq API key not configured, skipping summarization");
            return null;
        }
        if (title == null || title.isBlank()) return null;

        String content = fetchContent(url);
        String userMessage = content != null
                ? "다음 IT 뉴스 기사를 한국어로 3~5문장으로 요약해줘:\n\n제목: " + title + "\n\n본문:\n" + content
                : "다음 IT 뉴스 제목을 한국어로 2~3문장으로 설명해줘:\n" + title;

        try {
            semaphore.acquire();
            try {
                String body = buildRequest(userMessage);
                Request request = new Request.Builder()
                        .url(GROQ_URL)
                        .header("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(body, JSON))
                        .build();

                for (int attempt = 0; attempt < 3; attempt++) {
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.code() == 429) {
                            long wait = (long) Math.pow(2, attempt) * 30_000L; // 30s, 60s, 120s
                            log.warn("Groq 429 rate limit, waiting {}s (attempt {})", wait / 1000, attempt + 1);
                            Thread.sleep(wait);
                            continue;
                        }
                        if (!response.isSuccessful()) {
                            log.warn("Groq API HTTP {}: {}", response.code(), response.message());
                            return null;
                        }
                        ResponseBody rb = response.body();
                        if (rb == null) return null;
                        return parseResponse(rb.string());
                    }
                }
                log.warn("Groq API failed after 3 attempts (rate limit)");
                return null;
            } finally {
                Thread.sleep(2100); // 30 RPM 여유
                semaphore.release();
            }
        } catch (Exception e) {
            log.warn("Groq summarization failed [{}]: {}", title, e.getMessage());
            return null;
        }
    }

    private String buildRequest(String userMessage) throws Exception {
        var root = objectMapper.createObjectNode();
        root.put("model", MODEL);
        root.put("max_tokens", 512);
        root.put("temperature", 0.3);
        var messages = objectMapper.createArrayNode();
        var system = objectMapper.createObjectNode();
        system.put("role", "system");
        system.put("content", "당신은 IT 뉴스 요약 전문가입니다. 핵심만 간결하게 한국어로 요약하세요.");
        var user = objectMapper.createObjectNode();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(system);
        messages.add(user);
        root.set("messages", messages);
        return objectMapper.writeValueAsString(root);
    }

    private String parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode text = root.path("choices").path(0).path("message").path("content");
            if (text.isMissingNode()) {
                log.warn("Unexpected Groq response structure");
                return null;
            }
            return text.asText().trim();
        } catch (IOException e) {
            log.warn("Failed to parse Groq response: {}", e.getMessage());
            return null;
        }
    }
}
