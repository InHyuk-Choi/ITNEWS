package com.itnews.backend.crawler;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RSS 파싱 로직 단위 테스트.
 * Rome 라이브러리를 이용해 mock RSS XML 파싱 결과를 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class RssCrawlerTest {

    // ── 테스트용 RSS XML ──────────────────────────────────────────────────────
    private static final String SAMPLE_RSS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Hacker News RSS</title>
                <link>https://news.ycombinator.com</link>
                <description>Hacker News RSS feed</description>
                <item>
                  <title>OpenAI releases GPT-5</title>
                  <link>https://news.ycombinator.com/item?id=1001</link>
                  <description>OpenAI has released GPT-5 with major improvements over GPT-4.</description>
                  <pubDate>Mon, 07 Apr 2025 10:00:00 +0000</pubDate>
                </item>
                <item>
                  <title>Rust 2.0 announced at RustConf</title>
                  <link>https://news.ycombinator.com/item?id=1002</link>
                  <description>The Rust programming language version 2.0 was officially announced.</description>
                  <pubDate>Mon, 07 Apr 2025 08:00:00 +0000</pubDate>
                </item>
              </channel>
            </rss>
            """;

    private static final String EMPTY_RSS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Empty Feed</title>
                <link>https://example.com</link>
                <description>No articles</description>
              </channel>
            </rss>
            """;

    // ── 헬퍼: RSS XML 파싱 ────────────────────────────────────────────────────
    private SyndFeed parseFeed(String xml) throws Exception {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        SyndFeedInput input = new SyndFeedInput();
        return input.build(new XmlReader(is));
    }

    /**
     * SyndEntry → NewsEntity-like DTO 변환 헬퍼 (크롤러 로직 인라인 시뮬레이션)
     */
    private ParsedArticle toArticle(SyndEntry entry, String source) {
        String title = entry.getTitle() != null ? entry.getTitle().trim() : "";
        String url = entry.getLink() != null ? entry.getLink().trim() : "";
        String description = "";
        if (entry.getDescription() != null) {
            description = entry.getDescription().getValue();
        } else if (!entry.getContents().isEmpty()) {
            description = entry.getContents().get(0).getValue();
        }
        // Jsoup으로 HTML 태그 제거 시뮬레이션
        String plainText = org.jsoup.Jsoup.parse(description).text();
        java.time.OffsetDateTime publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant().atOffset(java.time.ZoneOffset.UTC)
                : java.time.OffsetDateTime.now();

        return new ParsedArticle(title, url, source, plainText, publishedAt);
    }

    // ── 테스트 1: RSS XML 파싱 결과 검증 ────────────────────────────────────
    @Test
    @DisplayName("RSS XML 파싱: 기사 목록이 올바르게 파싱된다")
    void rssXml_파싱_기사목록_정상반환() throws Exception {
        // when
        SyndFeed feed = parseFeed(SAMPLE_RSS_XML);
        List<SyndEntry> entries = feed.getEntries();

        // then – 피드 메타 정보
        assertThat(feed.getTitle()).isEqualTo("Hacker News RSS");
        assertThat(entries).hasSize(2);

        // 첫 번째 기사
        SyndEntry first = entries.get(0);
        assertThat(first.getTitle()).isEqualTo("OpenAI releases GPT-5");
        assertThat(first.getLink()).isEqualTo("https://news.ycombinator.com/item?id=1001");
        assertThat(first.getDescription().getValue())
                .contains("OpenAI has released GPT-5");
        assertThat(first.getPublishedDate()).isNotNull();
    }

    // ── 테스트 2: NewsEntity 형태로 변환 ─────────────────────────────────────
    @Test
    @DisplayName("RSS 파싱 결과가 NewsEntity 형태(DTO)로 올바르게 변환된다")
    void rssEntry_NewsEntity_변환_정상동작() throws Exception {
        // given
        SyndFeed feed = parseFeed(SAMPLE_RSS_XML);
        SyndEntry entry = feed.getEntries().get(0);

        // when
        ParsedArticle article = toArticle(entry, "hackernews");

        // then
        assertThat(article.title()).isEqualTo("OpenAI releases GPT-5");
        assertThat(article.url()).isEqualTo("https://news.ycombinator.com/item?id=1001");
        assertThat(article.source()).isEqualTo("hackernews");
        assertThat(article.description()).isNotBlank();
        assertThat(article.publishedAt()).isNotNull();
    }

    // ── 테스트 3: 빈 RSS 피드 → 빈 리스트 반환 ──────────────────────────────
    @Test
    @DisplayName("빈 RSS 피드 처리: 예외 없이 빈 리스트를 반환한다")
    void emptyRssFeed_예외없이_빈리스트반환() {
        assertThatCode(() -> {
            SyndFeed feed = parseFeed(EMPTY_RSS_XML);
            List<SyndEntry> entries = feed.getEntries();
            assertThat(entries).isEmpty();
        }).doesNotThrowAnyException();
    }

    // ── 테스트 4: title/link null 방어 처리 ───────────────────────────────────
    @Test
    @DisplayName("RSS 항목에 title이 없을 때 빈 문자열로 처리된다")
    void rssEntry_title없음_빈문자열처리() {
        SyndEntry entry = new SyndEntryImpl();
        entry.setLink("https://example.com/no-title");
        // title 미설정

        assertThatCode(() -> {
            ParsedArticle article = toArticle(entry, "hackernews");
            assertThat(article.title()).isEmpty();
            assertThat(article.url()).isEqualTo("https://example.com/no-title");
        }).doesNotThrowAnyException();
    }

    // ── 테스트 5: HTML description의 태그 제거 ────────────────────────────────
    @Test
    @DisplayName("RSS description의 HTML 태그가 제거된 평문으로 변환된다")
    void rssEntry_htmlDescription_태그제거() {
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle("HTML Test Article");
        entry.setLink("https://example.com/html-test");
        entry.setPublishedDate(new Date());

        SyndContent content = new SyndContentImpl();
        content.setValue("<p>This is <strong>important</strong> news about <a href='#'>tech</a>.</p>");
        entry.setDescription(content);

        ParsedArticle article = toArticle(entry, "hackernews");

        assertThat(article.description()).isEqualTo("This is important news about tech.");
        assertThat(article.description()).doesNotContain("<p>", "<strong>", "<a");
    }

    // ── 테스트 6: pubDate 누락 시 현재 시각으로 대체 ─────────────────────────
    @Test
    @DisplayName("RSS 항목에 pubDate가 없으면 현재 시각을 publishedAt으로 사용한다")
    void rssEntry_pubDate없음_현재시각_대체() {
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle("No Date Article");
        entry.setLink("https://example.com/no-date");
        // publishedDate 미설정

        java.time.OffsetDateTime before = java.time.OffsetDateTime.now().minusSeconds(1);
        ParsedArticle article = toArticle(entry, "hackernews");
        java.time.OffsetDateTime after = java.time.OffsetDateTime.now().plusSeconds(1);

        assertThat(article.publishedAt()).isAfter(before);
        assertThat(article.publishedAt()).isBefore(after);
    }

    // ── 내부 DTO ─────────────────────────────────────────────────────────────
    record ParsedArticle(
            String title,
            String url,
            String source,
            String description,
            java.time.OffsetDateTime publishedAt
    ) {}
}
