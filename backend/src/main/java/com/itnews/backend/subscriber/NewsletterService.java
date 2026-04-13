package com.itnews.backend.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itnews.backend.news.NewsEntity;
import com.itnews.backend.news.NewsRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("M월 d일").withZone(java.time.ZoneId.of("Asia/Seoul"));

    private final SubscriberRepository subscriberRepository;
    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final String resendApiKey;
    private final String groqApiKey;
    private final String frontendUrl;

    public NewsletterService(
            SubscriberRepository subscriberRepository,
            NewsRepository newsRepository,
            @Value("${app.resend.api-key:}") String resendApiKey,
            @Value("${app.groq.api-key:}") String groqApiKey,
            @Value("${app.cors.allowed-origin:http://localhost:3000}") String frontendUrl
    ) {
        this.subscriberRepository = subscriberRepository;
        this.newsRepository = newsRepository;
        this.resendApiKey = resendApiKey;
        this.groqApiKey = groqApiKey;
        this.frontendUrl = frontendUrl;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /** 매주 월요일 오전 9시 KST (0시 UTC) */
    @Scheduled(cron = "0 0 0 * * MON")
    public void sendWeeklyNewsletter() {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY not set, skipping newsletter");
            return;
        }

        List<SubscriberEntity> subscribers = subscriberRepository.findAllByActiveTrue();
        if (subscribers.isEmpty()) {
            log.info("No active subscribers, skipping newsletter");
            return;
        }

        // 지난 7일 기사 50개 가져와서 Groq이 중요도 선별
        List<NewsEntity> candidates = newsRepository.findTop10ForNewsletter(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(7),
                PageRequest.of(0, 50)
        );

        if (candidates.isEmpty()) {
            log.info("No articles for newsletter");
            return;
        }

        List<NewsEntity> articles = selectImportantArticles(candidates);
        String html = buildHtml(articles);
        int sent = 0;

        for (SubscriberEntity subscriber : subscribers) {
            try {
                sendEmail(subscriber.getEmail(), html, subscriber.getUnsubscribeToken());
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send newsletter to {}: {}", subscriber.getEmail(), e.getMessage());
            }
        }
        log.info("Weekly newsletter sent to {}/{} subscribers", sent, subscribers.size());
    }

    /** Groq에게 기사 목록 주고 중요한 10개 ID 선택 */
    private List<NewsEntity> selectImportantArticles(List<NewsEntity> candidates) {
        if (groqApiKey == null || groqApiKey.isBlank() || candidates.size() <= 10) {
            return candidates.stream().limit(10).collect(Collectors.toList());
        }

        try {
            // 번호:제목 목록 생성
            StringBuilder prompt = new StringBuilder(
                "다음은 이번 주 IT 뉴스 기사 목록이야. 개발자와 IT 종사자에게 가장 중요하고 임팩트 있는 기사 10개의 번호를 골라줘.\n" +
                "JSON 배열 형식으로만 답해줘. 예: [1, 5, 12, ...]\n\n"
            );
            Map<Integer, NewsEntity> indexMap = new LinkedHashMap<>();
            int idx = 1;
            for (NewsEntity a : candidates) {
                prompt.append(idx).append(". [").append(a.getSource()).append("] ").append(a.getTitle()).append("\n");
                indexMap.put(idx, a);
                idx++;
            }

            String response = callGroq(prompt.toString());
            if (response == null) return candidates.stream().limit(10).collect(Collectors.toList());

            // JSON 배열 파싱
            String json = response.replaceAll("(?s).*?(\\[.*?]).*", "$1");
            JsonNode arr = objectMapper.readTree(json);
            List<NewsEntity> selected = new ArrayList<>();
            for (JsonNode node : arr) {
                int i = node.asInt();
                if (indexMap.containsKey(i)) selected.add(indexMap.get(i));
                if (selected.size() == 10) break;
            }
            log.info("Groq selected {}/{} articles for newsletter", selected.size(), candidates.size());
            return selected.isEmpty() ? candidates.stream().limit(10).collect(Collectors.toList()) : selected;

        } catch (Exception e) {
            log.warn("Groq article selection failed, using latest 10: {}", e.getMessage());
            return candidates.stream().limit(10).collect(Collectors.toList());
        }
    }

    private String callGroq(String userMessage) throws Exception {
        var root = objectMapper.createObjectNode();
        root.put("model", "llama-3.1-8b-instant");
        root.put("max_tokens", 100);
        root.put("temperature", 0.1);
        var messages = objectMapper.createArrayNode();
        var user = objectMapper.createObjectNode();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);
        root.set("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + groqApiKey)
                .post(RequestBody.create(objectMapper.writeValueAsString(root), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            JsonNode json = objectMapper.readTree(response.body().string());
            return json.path("choices").path(0).path("message").path("content").asText();
        }
    }

    private void sendEmail(String to, String html, String unsubscribeToken) throws Exception {
        String unsubUrl = frontendUrl + "/unsubscribe?token=" + unsubscribeToken;
        String fullHtml = html + "<p style='color:#999;font-size:12px;margin-top:32px;'>" +
                "<a href='" + unsubUrl + "' style='color:#999;'>수신거부</a></p>";

        var body = objectMapper.createObjectNode();
        body.put("from", "IT뉴스 <onboarding@resend.dev>");
        body.put("to", to);
        body.put("subject", "📰 이번 주 IT 뉴스 모아보기");
        body.put("html", fullHtml);

        Request request = new Request.Builder()
                .url("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Resend API error: " + response.code());
            }
        }
    }

    private String buildHtml(List<NewsEntity> articles) {
        String date = DATE_FMT.format(java.time.Instant.now());
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:sans-serif;max-width:600px;margin:0 auto;padding:24px;'>")
          .append("<h1 style='font-size:22px;font-weight:bold;margin-bottom:4px;'>📰 이번 주 IT 뉴스</h1>")
          .append("<p style='color:#888;font-size:14px;margin-bottom:32px;'>")
          .append(date).append(" 기준 주요 뉴스</p>");

        for (NewsEntity a : articles) {
            sb.append("<div style='margin-bottom:28px;padding-bottom:28px;border-bottom:1px solid #eee;'>")
              .append("<p style='color:#888;font-size:12px;margin:0 0 6px;'>")
              .append(a.getSource().toUpperCase()).append("</p>")
              .append("<a href='").append(a.getUrl())
              .append("' style='font-size:16px;font-weight:bold;color:#111;text-decoration:none;'>")
              .append(a.getTitle()).append("</a>");
            if (a.getSummary() != null) {
                sb.append("<p style='color:#555;font-size:14px;margin:8px 0 0;line-height:1.6;'>")
                  .append(a.getSummary()).append("</p>");
            }
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }
}
