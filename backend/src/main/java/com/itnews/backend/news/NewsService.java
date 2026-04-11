package com.itnews.backend.news;

import com.itnews.backend.ai.GeminiSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final NewsRepository newsRepository;
    private final GeminiSummaryService geminiSummaryService;

    public NewsService(NewsRepository newsRepository, GeminiSummaryService geminiSummaryService) {
        this.newsRepository = newsRepository;
        this.geminiSummaryService = geminiSummaryService;
    }

    /**
     * Saves the given news entity if the URL has not been seen before.
     * Uses ON CONFLICT DO NOTHING at the DB level to prevent race conditions.
     * After a successful insert, triggers async Gemini summarization.
     *
     * @return true if a new record was inserted, false if it was a duplicate.
     */
    @CacheEvict(value = "news", allEntries = true)
    @Transactional
    public boolean saveIfNotExists(NewsEntity news) {
        try {
            String summary = geminiSummaryService.summarize(news.getTitle(), news.getUrl());

            int inserted = newsRepository.upsertIgnoreDuplicate(
                    news.getTitle(),
                    news.getUrl(),
                    news.getSource(),
                    summary,
                    news.getThumbnail(),
                    news.getPublishedAt(),
                    OffsetDateTime.now()
            );
            if (inserted > 0) {
                log.debug("Saved new article [{}]: {}", news.getSource(), news.getTitle());
                return true;
            } else {
                log.trace("Skipped duplicate article: {}", news.getUrl());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to save article [{}]: {}", news.getUrl(), e.getMessage());
            return false;
        }
    }

    @Cacheable(value = "news", key = "'search_' + #q + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public NewsPageResponse searchNews(String q, int page, int size) {
        Page<NewsEntity> result = newsRepository.searchByTitleOrSummary(
                q, PageRequest.of(page, size));
        return new NewsPageResponse(
                result.getContent().stream().map(NewsDto::from).toList(),
                result.getTotalPages(),
                result.getNumber(),
                result.getTotalElements()
        );
    }

    /** 요약 없는 기사 최대 limit개 재시도 */
    @CacheEvict(value = "news", allEntries = true)
    @Transactional
    public void retrySummarization(int limit) {
        List<NewsEntity> unsummarized = newsRepository.findUnsummarized(
                org.springframework.data.domain.PageRequest.of(0, limit));
        for (NewsEntity news : unsummarized) {
            String summary = geminiSummaryService.summarize(news.getTitle(), news.getUrl());
            if (summary != null) {
                news.setSummary(summary);
                newsRepository.save(news);
                log.info("Retry summary OK: {}", news.getTitle());
            }
        }
    }

    /**
     * Returns a paginated list of news, optionally filtered by source.
     * Results are cached under the "news" cache with a 30-minute TTL.
     */
    @Cacheable(value = "news", key = "#source + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public NewsPageResponse getNews(String source, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsEntity> result;

        if (source != null && !source.isBlank()) {
            result = newsRepository.findBySourceOrderByPublishedAtDesc(source, pageable);
        } else {
            result = newsRepository.findAllByOrderByPublishedAtDesc(pageable);
        }

        return new NewsPageResponse(
                result.getContent().stream().map(NewsDto::from).toList(),
                result.getTotalPages(),
                result.getNumber(),
                result.getTotalElements()
        );
    }
}
