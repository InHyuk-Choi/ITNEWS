package com.itnews.backend.news;

import com.itnews.backend.crawler.CrawlerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class NewsController {

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    private final NewsService newsService;
    private final CrawlerScheduler crawlerScheduler;

    public NewsController(NewsService newsService, CrawlerScheduler crawlerScheduler) {
        this.newsService = newsService;
        this.crawlerScheduler = crawlerScheduler;
    }

    /**
     * GET /api/news
     * Query params:
     *   source (optional) - filter by news source
     *   page   (default 0)
     *   size   (default 20)
     */
    @GetMapping("/api/news")
    public ResponseEntity<NewsPageResponse> getNews(
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (size < 1 || size > 100) size = 20;
        if (page < 0) page = 0;
        NewsPageResponse response = newsService.getNews(source, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/news/search")
    public ResponseEntity<NewsPageResponse> searchNews(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (q == null || q.isBlank()) return ResponseEntity.badRequest().build();
        if (size < 1 || size > 100) size = 20;
        return ResponseEntity.ok(newsService.searchNews(q.trim(), page, size));
    }

    /**
     * POST /api/admin/crawl
     * Manually triggers a full crawl cycle. Intended for local testing only.
     * Access is restricted to localhost via SecurityConfig.
     */
    @PostMapping("/api/admin/crawl")
    public ResponseEntity<String> triggerCrawl() {
        log.info("Manual crawl triggered via admin endpoint");
        crawlerScheduler.crawlAll();
        return ResponseEntity.ok("Crawl triggered successfully");
    }
}
