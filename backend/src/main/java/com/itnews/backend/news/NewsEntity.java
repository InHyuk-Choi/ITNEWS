package com.itnews.backend.news;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "news")
public class NewsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String thumbnail;

    @Column(nullable = false)
    private OffsetDateTime publishedAt;

    @Column
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected NewsEntity() {}

    private NewsEntity(Builder builder) {
        this.title = builder.title;
        this.url = builder.url;
        this.source = builder.source;
        this.summary = builder.summary;
        this.thumbnail = builder.thumbnail;
        this.publishedAt = builder.publishedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSource() { return source; }
    public String getSummary() { return summary; }
    public String getThumbnail() { return thumbnail; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setSource(String source) { this.source = source; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static class Builder {
        private String title;
        private String url;
        private String source;
        private String summary;
        private String thumbnail;
        private OffsetDateTime publishedAt;

        public Builder title(String title) { this.title = title; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder thumbnail(String thumbnail) { this.thumbnail = thumbnail; return this; }
        public Builder publishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; return this; }

        public NewsEntity build() {
            if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
            if (url == null || url.isBlank()) throw new IllegalArgumentException("url is required");
            if (source == null || source.isBlank()) throw new IllegalArgumentException("source is required");
            if (publishedAt == null) throw new IllegalArgumentException("publishedAt is required");
            return new NewsEntity(this);
        }
    }
}
