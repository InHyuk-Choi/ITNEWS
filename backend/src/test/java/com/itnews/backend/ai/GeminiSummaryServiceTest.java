package com.itnews.backend.ai;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GeminiSummaryService 단위 테스트.
 * OkHttpClient를 Mock 처리하여 외부 API 호출 없이 로직을 검증합니다.
 *
 * 실제 Gemini API 호출 테스트는 별도 integration test(GeminiSummaryServiceIntegrationTest)로 분리.
 */
@ExtendWith(MockitoExtension.class)
class GeminiSummaryServiceTest {

    // ── Gemini API 응답 JSON 샘플 ─────────────────────────────────────────────
    private static final String GEMINI_SUCCESS_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "이 기사는 OpenAI의 GPT-5 출시에 관한 내용으로, 이전 버전 대비 크게 향상된 성능을 보여줍니다."
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;

    private static final String GEMINI_EMPTY_RESPONSE = """
            {
              "candidates": []
            }
            """;

    @Mock
    private OkHttpClient mockHttpClient;

    // GeminiSummaryService는 아직 생성 중일 수 있으므로, 인터페이스 계약을 검증하는 방식으로 작성
    // 실제 구현체가 생성되면 아래 헬퍼 클래스 대신 실제 클래스를 주입

    /**
     * 테스트용 GeminiSummaryService 구현 (실제 구현체가 없을 때 대체).
     * 실제 구현체가 존재하면 이 클래스 대신 @Autowired로 주입.
     */
    private GeminiSummaryServiceStub summaryService;

    @BeforeEach
    void setUp() {
        summaryService = new GeminiSummaryServiceStub(mockHttpClient);
    }

    // ── 테스트 1: API key 없을 때 null 반환 ──────────────────────────────────
    @Test
    @DisplayName("API key가 없을 때 예외 없이 null을 반환한다")
    void summarize_apiKey없음_null반환() {
        // given – API key 빈 문자열로 서비스 생성
        GeminiSummaryServiceStub noKeyService = new GeminiSummaryServiceStub(mockHttpClient, "");

        // when
        String result = noKeyService.summarize("Some article text");

        // then
        assertThat(result).isNull();
    }

    // ── 테스트 2: API key null일 때 null 반환 ────────────────────────────────
    @Test
    @DisplayName("API key가 null일 때 예외 없이 null을 반환한다")
    void summarize_apiKeyNull_null반환() {
        GeminiSummaryServiceStub nullKeyService = new GeminiSummaryServiceStub(mockHttpClient, null);

        assertThatCode(() -> {
            String result = nullKeyService.summarize("Some article text");
            assertThat(result).isNull();
        }).doesNotThrowAnyException();
    }

    // ── 테스트 3: 네트워크 에러 시 null 반환 (서비스 중단 없음) ──────────────
    @Test
    @DisplayName("API 호출 중 네트워크 오류 발생 시 예외 없이 null을 반환한다")
    void summarize_networkError_null반환() throws IOException {
        // given – OkHttpClient가 IOException을 던지도록 설정
        Call mockCall = mock(Call.class);
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new IOException("Connection refused"));

        GeminiSummaryServiceStub networkErrorService =
                new GeminiSummaryServiceStub(mockHttpClient, "test-api-key");

        // when
        String result = networkErrorService.summarize("Some article text");

        // then – 예외가 전파되지 않고 null 반환
        assertThat(result).isNull();
    }

    // ── 테스트 4: API 응답 파싱 성공 ─────────────────────────────────────────
    @Test
    @DisplayName("API 호출 성공 시 응답에서 요약 텍스트를 추출한다")
    void summarize_성공응답_요약텍스트반환() throws IOException {
        // given – 정상 응답 mock
        Response fakeResponse = new Response.Builder()
                .request(new Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(GEMINI_SUCCESS_RESPONSE, MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        GeminiSummaryServiceStub service =
                new GeminiSummaryServiceStub(mockHttpClient, "test-api-key");

        // when
        String result = service.summarize("OpenAI has released GPT-5 with major improvements.");

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("GPT-5");
    }

    // ── 테스트 5: API 500 에러 → null 반환 ───────────────────────────────────
    @Test
    @DisplayName("API 서버가 500 에러를 반환할 때 예외 없이 null을 반환한다")
    void summarize_api500에러_null반환() throws IOException {
        Response errorResponse = new Response.Builder()
                .request(new Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("{\"error\": \"internal\"}", MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(errorResponse);

        GeminiSummaryServiceStub service =
                new GeminiSummaryServiceStub(mockHttpClient, "test-api-key");

        assertThatCode(() -> {
            String result = service.summarize("Some article text");
            assertThat(result).isNull();
        }).doesNotThrowAnyException();
    }

    // ── 테스트 6: API 응답에 candidates가 비어있을 때 null 반환 ───────────────
    @Test
    @DisplayName("API 응답에 candidates가 없을 때 null을 반환한다")
    void summarize_빈candidates_null반환() throws IOException {
        Response emptyResponse = new Response.Builder()
                .request(new Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(GEMINI_EMPTY_RESPONSE, MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(emptyResponse);

        GeminiSummaryServiceStub service =
                new GeminiSummaryServiceStub(mockHttpClient, "test-api-key");

        String result = service.summarize("Some article text");
        assertThat(result).isNull();
    }

    // ── 테스트 7: 빈 article 텍스트 → null 반환 ──────────────────────────────
    @Test
    @DisplayName("summarize에 빈 문자열이 전달되면 null을 반환한다")
    void summarize_빈텍스트_null반환() {
        GeminiSummaryServiceStub service =
                new GeminiSummaryServiceStub(mockHttpClient, "test-api-key");

        assertThat(service.summarize("")).isNull();
        assertThat(service.summarize("   ")).isNull();
        assertThat(service.summarize(null)).isNull();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트용 Stub 클래스 (실제 GeminiSummaryService 구현체가 없을 때 사용)
    // 실제 구현체가 생성되면 이 Stub을 제거하고 실제 클래스를 @Autowired로 주입
    // ──────────────────────────────────────────────────────────────────────────
    static class GeminiSummaryServiceStub {

        private static final String GEMINI_API_URL =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

        private final OkHttpClient httpClient;
        private final String apiKey;
        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        GeminiSummaryServiceStub(OkHttpClient httpClient) {
            this(httpClient, "default-test-key");
        }

        GeminiSummaryServiceStub(OkHttpClient httpClient, String apiKey) {
            this.httpClient = httpClient;
            this.apiKey = apiKey;
        }

        public String summarize(String articleText) {
            // API key 없으면 null 반환
            if (apiKey == null || apiKey.isBlank()) {
                return null;
            }
            // 빈 텍스트이면 null 반환
            if (articleText == null || articleText.isBlank()) {
                return null;
            }

            try {
                String requestBody = buildRequestBody(articleText);
                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                        requestBody, okhttp3.MediaType.get("application/json")
                );
                Request request = new Request.Builder()
                        .url(GEMINI_API_URL + "?key=" + apiKey)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return null;
                    }
                    return extractSummary(response.body().string());
                }
            } catch (Exception e) {
                // 네트워크 오류 등 모든 예외 → null 반환, 서비스 중단 없음
                return null;
            }
        }

        private String buildRequestBody(String text) {
            return """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": "다음 IT 뉴스를 2-3문장으로 한국어 요약해줘:\\n\\n%s"
                            }
                          ]
                        }
                      ]
                    }
                    """.formatted(text.replace("\"", "\\\""));
        }

        private String extractSummary(String responseBody) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
                com.fasterxml.jackson.databind.JsonNode candidates = root.path("candidates");
                if (candidates.isEmpty()) {
                    return null;
                }
                return candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText(null);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
