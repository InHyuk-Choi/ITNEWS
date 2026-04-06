package com.itnews.backend.crawler;

import com.itnews.backend.news.NewsEntity;
import com.itnews.backend.news.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schedules periodic crawling of all registered news sources.
 * Each crawler runs in parallel; failures in one do not affect the others.
 * Scheduling can be disabled via the {@code scheduling.enabled} property.
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CrawlerScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrawlerScheduler.class);

    private final List<Crawler> crawlers;
    private final NewsService newsService;
    private final Executor crawlerExecutor;

    public CrawlerScheduler(
            List<Crawler> crawlers,
            NewsService newsService
    ) {
        this.crawlers = crawlers;
        this.newsService = newsService;
        // Virtual threads (Java 21+) for lightweight concurrency
        this.crawlerExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Runs at the top of every hour.
     * All crawlers execute in parallel; the method waits for all to finish before returning.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void crawlAll() {
        log.info("Starting scheduled crawl across {} sources", crawlers.size());
        AtomicInteger totalSaved = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = crawlers.stream()
                .map(crawler -> CompletableFuture.runAsync(() -> {
                    try {
                        List<NewsEntity> articles = crawler.crawl();
                        int saved = 0;
                        for (NewsEntity article : articles) {
                            if (newsService.saveIfNotExists(article)) {
                                saved++;
                            }
                        }
                        totalSaved.addAndGet(saved);
                        log.info("[{}] Saved {}/{} articles", crawler.getName(), saved, articles.size());
                    } catch (Exception e) {
                        // This catch is a safety net; individual crawlers should handle their own exceptions
                        log.error("[{}] Unexpected error during crawl: {}", crawler.getName(), e.getMessage(), e);
                    }
                }, crawlerExecutor))
                .toList();

        // Wait for all crawlers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Crawl complete. Total new articles saved: {}", totalSaved.get());
    }
}
