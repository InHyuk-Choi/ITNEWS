package com.itnews.backend.news;

import com.itnews.backend.ai.GeminiSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * NewsService 통합 테스트.
 * TestContainers로 실제 PostgreSQL 컨테이너를 띄워 테스트합니다.
 *
 * !! 주의: Docker가 실행 중이어야 합니다 !!
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class NewsServiceTest {

    // ── TestContainers: 실제 PostgreSQL 16 컨테이너 ──────────────────────────
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itnews_test")
            .withUsername("itnews")
            .withPassword("itnews");

    @DynamicPropertySource
    static void overrideDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ── 의존성 ────────────────────────────────────────────────────────────────
    @Autowired
    private NewsService newsService;

    @Autowired
    private NewsRepository newsRepository;

    // GeminiSummaryService Mock – 테스트에서 실제 API 호출 방지
    @MockBean
    private GeminiSummaryService geminiSummaryService;

    // ── 공통 설정 ─────────────────────────────────────────────────────────────
    @BeforeEach
    void setUp() {
        newsRepository.deleteAll();
        // Gemini mock: 항상 null 반환 (API key 없는 상태 시뮬레이션)
        when(geminiSummaryService.summarize(anyString())).thenReturn(null);
    }

    private NewsEntity buildEntity(String title, String url, String source) {
        return NewsEntity.builder()
                .title(title)
                .url(url)
                .source(source)
                .publishedAt(OffsetDateTime.now())
                .build();
    }

    // ── 테스트 1: 새 기사 저장 ───────────────────────────────────────────────
    @Test
    @DisplayName("saveIfNotExists: 새 URL 기사가 DB에 저장된다")
    void saveIfNotExists_새기사_저장됨() {
        // given
        NewsEntity entity = buildEntity(
                "OpenAI releases GPT-5",
                "https://news.ycombinator.com/item?id=1234",
                "hackernews"
        );

        // when
        boolean saved = newsService.saveIfNotExists(entity);

        // then
        assertThat(saved).isTrue();
        assertThat(newsRepository.existsByUrl(entity.getUrl())).isTrue();

        List<NewsEntity> all = newsRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("OpenAI releases GPT-5");
        assertThat(all.get(0).getSource()).isEqualTo("hackernews");
    }

    // ── 테스트 2: 중복 URL 스킵 ──────────────────────────────────────────────
    @Test
    @DisplayName("saveIfNotExists: 동일 URL 2회 저장 시 DB에 1건만 존재한다")
    void saveIfNotExists_중복URL_스킵됨() {
        // given
        String url = "https://news.ycombinator.com/item?id=9999";
        NewsEntity first = buildEntity("First Insert", url, "hackernews");
        NewsEntity second = buildEntity("Second Insert (duplicate)", url, "hackernews");

        // when
        boolean firstResult = newsService.saveIfNotExists(first);
        boolean secondResult = newsService.saveIfNotExists(second);

        // then
        assertThat(firstResult).isTrue();
        assertThat(secondResult).isFalse();

        List<NewsEntity> all = newsRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("First Insert");
    }

    // ── 테스트 3: 소스 필터 ───────────────────────────────────────────────────
    @Test
    @DisplayName("getNews: source=hackernews 필터링 시 해당 소스 기사만 반환된다")
    void getNews_소스필터_정상동작() {
        // given – hackernews 2개, geeknews 1개 저장
        newsService.saveIfNotExists(buildEntity(
                "HN Article 1", "https://hn.com/1", "hackernews"
        ));
        newsService.saveIfNotExists(buildEntity(
                "HN Article 2", "https://hn.com/2", "hackernews"
        ));
        newsService.saveIfNotExists(buildEntity(
                "GN Article 1", "https://gn.com/1", "geeknews"
        ));

        // when
        NewsPageResponse hnPage = newsService.getNews("hackernews", 0, 10);
        NewsPageResponse allPage = newsService.getNews(null, 0, 10);

        // then
        assertThat(hnPage.content()).hasSize(2);
        assertThat(hnPage.content()).allMatch(dto -> dto.source().equals("hackernews"));
        assertThat(hnPage.totalElements()).isEqualTo(2);

        assertThat(allPage.totalElements()).isEqualTo(3);
    }

    // ── 테스트 4: existsByUrl ─────────────────────────────────────────────────
    @Test
    @DisplayName("existsByUrl: 존재하지 않는 URL이면 false를 반환한다")
    void existsByUrl_존재하지않으면_false() {
        assertThat(newsRepository.existsByUrl("https://nonexistent.example.com")).isFalse();
    }

    // ── 테스트 5: 페이지네이션 ──────────────────────────────────────────────
    @Test
    @DisplayName("getNews: 페이지 사이즈대로 결과가 잘린다")
    void getNews_페이지네이션_정상동작() {
        // given – 기사 7개 저장
        for (int i = 0; i < 7; i++) {
            newsService.saveIfNotExists(buildEntity(
                    "Article " + i,
                    "https://example.com/" + i,
                    "hackernews"
            ));
        }

        // when
        NewsPageResponse page0 = newsService.getNews(null, 0, 5);
        NewsPageResponse page1 = newsService.getNews(null, 1, 5);

        // then
        assertThat(page0.content()).hasSize(5);
        assertThat(page1.content()).hasSize(2);
        assertThat(page0.totalElements()).isEqualTo(7);
        assertThat(page0.totalPages()).isEqualTo(2);
    }

    // ── 테스트 6: NewsPageResponse 필드 검증 ─────────────────────────────────
    @Test
    @DisplayName("getNews: 반환된 NewsPageResponse에 currentPage가 올바르게 설정된다")
    void getNews_currentPage_정상설정() {
        for (int i = 0; i < 5; i++) {
            newsService.saveIfNotExists(buildEntity(
                    "Article " + i,
                    "https://example.com/page/" + i,
                    "hackernews"
            ));
        }

        NewsPageResponse page1 = newsService.getNews(null, 1, 3);

        assertThat(page1.currentPage()).isEqualTo(1);
        assertThat(page1.totalElements()).isEqualTo(5);
    }

    // ── 테스트 7: Repository upsert 직접 검증 ─────────────────────────────────
    @Test
    @DisplayName("upsertIgnoreDuplicate: 신규 삽입 시 1 반환, 중복 시 0 반환")
    void upsertIgnoreDuplicate_신규1_중복0() {
        OffsetDateTime now = OffsetDateTime.now();

        int first = newsRepository.upsertIgnoreDuplicate(
                "Direct Insert Test",
                "https://direct.example.com/1",
                "hackernews",
                null, null,
                now, now
        );
        int second = newsRepository.upsertIgnoreDuplicate(
                "Direct Insert Test Duplicate",
                "https://direct.example.com/1", // 같은 URL
                "hackernews",
                null, null,
                now, now
        );

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(newsRepository.findAll()).hasSize(1);
    }

    // ── 테스트 8: 최신순 정렬 확인 ──────────────────────────────────────────
    @Test
    @DisplayName("getNews: 기사가 publishedAt 기준 최신순으로 정렬된다")
    void getNews_최신순정렬() {
        OffsetDateTime now = OffsetDateTime.now();
        newsRepository.upsertIgnoreDuplicate(
                "Old Article", "https://example.com/old", "hackernews",
                null, null, now.minusDays(2), now
        );
        newsRepository.upsertIgnoreDuplicate(
                "New Article", "https://example.com/new", "hackernews",
                null, null, now.minusHours(1), now
        );
        newsRepository.upsertIgnoreDuplicate(
                "Middle Article", "https://example.com/mid", "hackernews",
                null, null, now.minusDays(1), now
        );

        NewsPageResponse response = newsService.getNews(null, 0, 10);

        assertThat(response.content().get(0).title()).isEqualTo("New Article");
        assertThat(response.content().get(1).title()).isEqualTo("Middle Article");
        assertThat(response.content().get(2).title()).isEqualTo("Old Article");
    }
}
