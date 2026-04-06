package com.itnews.backend.news;

import java.time.OffsetDateTime;

public record NewsDto(
        Long id,
        String title,
        String url,
        String source,
        String summary,
        String thumbnail,
        OffsetDateTime publishedAt
) {
    public static NewsDto from(NewsEntity entity) {
        return new NewsDto(
                entity.getId(),
                entity.getTitle(),
                entity.getUrl(),
                entity.getSource(),
                entity.getSummary(),
                entity.getThumbnail(),
                entity.getPublishedAt()
        );
    }
}
