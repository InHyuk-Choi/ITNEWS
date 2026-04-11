package com.itnews.backend.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.time.OffsetDateTime;

@Repository
public interface NewsRepository extends JpaRepository<NewsEntity, Long> {

    Page<NewsEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<NewsEntity> findBySourceOrderByPublishedAtDesc(String source, Pageable pageable);

    boolean existsByUrl(String url);

    @Query("SELECT n FROM NewsEntity n WHERE n.summary IS NULL ORDER BY n.publishedAt DESC")
    List<NewsEntity> findUnsummarized(Pageable pageable);

    @Query(value = """
            SELECT * FROM news
            WHERE
              title ILIKE '%' || :q || '%'
              OR summary ILIKE '%' || :q || '%'
              OR similarity(title, :q) > 0.15
              OR similarity(summary, :q) > 0.15
            ORDER BY
              GREATEST(similarity(title, :q), similarity(summary, :q)) DESC,
              published_at DESC
            """, nativeQuery = true)
    Page<NewsEntity> searchByTitleOrSummary(@Param("q") String q, Pageable pageable);

    /**
     * Insert a news record, silently ignoring duplicate URLs via ON CONFLICT DO NOTHING.
     * Returns the number of rows inserted (0 if duplicate, 1 if new).
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO news (title, url, source, summary, thumbnail, published_at, created_at)
            VALUES (:title, :url, :source, :summary, :thumbnail, :publishedAt, :createdAt)
            ON CONFLICT (url) DO NOTHING
            """, nativeQuery = true)
    int upsertIgnoreDuplicate(
            @Param("title") String title,
            @Param("url") String url,
            @Param("source") String source,
            @Param("summary") String summary,
            @Param("thumbnail") String thumbnail,
            @Param("publishedAt") OffsetDateTime publishedAt,
            @Param("createdAt") OffsetDateTime createdAt
    );
}
