package com.itnews.backend.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NewsController MockMvc 테스트.
 * NewsRepository를 Mock 처리하여 컨트롤러 레이어만 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsRepository newsRepository;

    private NewsEntity sampleEntity1;
    private NewsEntity sampleEntity2;

    @BeforeEach
    void setUp() {
        sampleEntity1 = NewsEntity.builder()
                .title("OpenAI releases GPT-5")
                .url("https://news.ycombinator.com/item?id=1001")
                .source("hackernews")
                .summary("OpenAI has released GPT-5 with significant improvements.")
                .publishedAt(OffsetDateTime.now().minusHours(1))
                .build();

        sampleEntity2 = NewsEntity.builder()
                .title("Rust 2.0 announced")
                .url("https://news.ycombinator.com/item?id=1002")
                .source("hackernews")
                .summary("Rust programming language version 2.0 announced.")
                .publishedAt(OffsetDateTime.now().minusHours(2))
                .build();
    }

    // ── GET /api/news → 200 OK, JSON 배열 ────────────────────────────────────
    @Test
    @DisplayName("GET /api/news → 200 OK와 JSON 배열을 반환한다")
    @WithMockUser
    void getNews_기본조회_200반환() throws Exception {
        List<NewsEntity> articles = List.of(sampleEntity1, sampleEntity2);
        PageImpl<NewsEntity> page = new PageImpl<>(articles, PageRequest.of(0, 20), 2);

        when(newsRepository.findAllByOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/news")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("OpenAI releases GPT-5"))
                .andExpect(jsonPath("$[0].source").value("hackernews"))
                .andExpect(jsonPath("$[1].title").value("Rust 2.0 announced"));
    }

    // ── GET /api/news?source=hackernews → 소스 필터 ──────────────────────────
    @Test
    @DisplayName("GET /api/news?source=hackernews → 해당 소스 기사만 반환한다")
    @WithMockUser
    void getNews_소스파라미터_필터동작() throws Exception {
        List<NewsEntity> filtered = List.of(sampleEntity1);
        PageImpl<NewsEntity> page = new PageImpl<>(filtered, PageRequest.of(0, 20), 1);

        when(newsRepository.findBySourceOrderByPublishedAtDesc(eq("hackernews"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/news")
                        .param("source", "hackernews")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].source").value("hackernews"));
    }

    // ── GET /api/news?page=0&size=5 → 페이지네이션 파라미터 ──────────────────
    @Test
    @DisplayName("GET /api/news?page=0&size=5 → 페이지네이션 파라미터가 올바르게 처리된다")
    @WithMockUser
    void getNews_페이지네이션파라미터_정상처리() throws Exception {
        List<NewsEntity> articles = List.of(sampleEntity1);
        PageImpl<NewsEntity> page = new PageImpl<>(articles, PageRequest.of(0, 5), 15);

        when(newsRepository.findAllByOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/news")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /api/news → 빈 목록 ──────────────────────────────────────────────
    @Test
    @DisplayName("GET /api/news → 기사가 없으면 빈 배열을 반환한다")
    @WithMockUser
    void getNews_기사없음_빈배열반환() throws Exception {
        PageImpl<NewsEntity> emptyPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 20), 0
        );

        when(newsRepository.findAllByOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/news")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Rate Limit: 429 반환 ──────────────────────────────────────────────────
    @Test
    @DisplayName("Rate limit 초과(60 req/min) 시 429 Too Many Requests를 반환한다")
    @WithMockUser
    void getNews_rateLimit초과_429반환() throws Exception {
        // bucket4j 설정이 60 req/min이므로 61번 이상 호출 시 429
        PageImpl<NewsEntity> page = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 20), 0
        );
        when(newsRepository.findAllByOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        // 60번까지는 성공
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/news").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // 61번째 요청 → 429
        mockMvc.perform(get("/api/news")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());
    }

    // ── GET /api/news → summary null 허용 ────────────────────────────────────
    @Test
    @DisplayName("GET /api/news → summary가 null인 기사도 정상 직렬화된다")
    @WithMockUser
    void getNews_summary없는기사_정상직렬화() throws Exception {
        NewsEntity noSummary = NewsEntity.builder()
                .title("Article without summary")
                .url("https://example.com/no-summary")
                .source("geeknews")
                .publishedAt(OffsetDateTime.now())
                .build();

        PageImpl<NewsEntity> page = new PageImpl<>(
                List.of(noSummary), PageRequest.of(0, 20), 1
        );
        when(newsRepository.findAllByOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/news").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Article without summary"))
                .andExpect(jsonPath("$[0].summary").doesNotExist());
    }
}
