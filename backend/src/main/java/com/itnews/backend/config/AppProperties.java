package com.itnews.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for application-level configuration properties.
 * Values are read from application.yml / environment variables.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Gemini gemini = new Gemini();
    private Naver naver = new Naver();
    private Cors cors = new Cors();
    private Crawler crawler = new Crawler();

    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }

    public Naver getNaver() { return naver; }
    public void setNaver(Naver naver) { this.naver = naver; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    public Crawler getCrawler() { return crawler; }
    public void setCrawler(Crawler crawler) { this.crawler = crawler; }

    public static class Gemini {
        private String apiKey = "";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Naver {
        private String clientId = "";
        private String clientSecret = "";
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    public static class Cors {
        private String allowedOrigin = "http://localhost:3000";
        public String getAllowedOrigin() { return allowedOrigin; }
        public void setAllowedOrigin(String allowedOrigin) { this.allowedOrigin = allowedOrigin; }
    }

    public static class Crawler {
        private int articlesPerSource = 8;
        public int getArticlesPerSource() { return articlesPerSource; }
        public void setArticlesPerSource(int articlesPerSource) { this.articlesPerSource = articlesPerSource; }
    }
}
