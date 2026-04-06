package com.itnews.backend.crawler;

import com.itnews.backend.news.NewsEntity;
import java.util.List;

/**
 * Common interface for all news source crawlers.
 */
public interface Crawler {

    /**
     * Fetches a list of news articles from the source.
     * Implementations must be fault-tolerant and never throw unchecked exceptions.
     */
    List<NewsEntity> crawl();

    /**
     * Human-readable name for logging purposes.
     */
    String getName();
}
