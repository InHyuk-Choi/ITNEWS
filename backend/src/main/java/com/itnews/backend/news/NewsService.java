package com.itnews.backend.news;

import com.itnews.backend.ai.GroqSummaryService;
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
    private final GroqSummaryService groqSummaryService;

    public NewsService(NewsRepository newsRepository, GroqSummaryService groqSummaryService) {
        this.newsRepository = newsRepository;
        this.groqSummaryService = groqSummaryService;
    }

    /**
     * 크롤링 배치 완료 시 한 번만 호출 (개별 insert마다 캐시를 지우지 않음).
     */
    @CacheEvict(value = "news", allEntries = true)
    public void evictNewsCache() {
        log.debug("News cache evicted");
    }

    @Transactional
    public boolean saveIfNotExists(NewsEntity news) {
        try {
            String summary = groqSummaryService.summarize(news.getTitle(), news.getUrl());

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
    @Transactional
    public int retrySummarization(int limit) {
        List<NewsEntity> unsummarized = newsRepository.findUnsummarized(
                PageRequest.of(0, limit));
        int succeeded = 0;
        for (NewsEntity news : unsummarized) {
            String summary = groqSummaryService.summarize(news.getTitle(), news.getUrl());
            if (summary != null) {
                news.setSummary(summary);
                newsRepository.save(news);
                succeeded++;
                log.info("Retry summary OK: {}", news.getTitle());
            }
        }
        return succeeded;
    }

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
