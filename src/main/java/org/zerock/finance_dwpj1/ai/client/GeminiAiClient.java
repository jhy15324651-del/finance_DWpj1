package org.zerock.finance_dwpj1.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.zerock.finance_dwpj1.ai.dto.AiRequest;
import org.zerock.finance_dwpj1.ai.dto.AiResult;
import org.zerock.finance_dwpj1.ai.exception.AiClientException;
import org.zerock.finance_dwpj1.ai.logging.AiCallTracer;
import org.zerock.finance_dwpj1.ai.logging.AiCallTracer.TraceContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini API 클라이언트 구현체
 * WebClient를 사용하여 REST API 호출
 */
@Slf4j
public class GeminiAiClient implements AiClient {

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String defaultModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiCallTracer tracer;

    public GeminiAiClient(AiCallTracer tracer) {
        this.tracer = tracer;
        this.webClient = WebClient.builder()
                .baseUrl(GEMINI_API_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiResult generate(AiRequest request) {
        // Trace 시작
        String model = request.getModel() != null ? request.getModel() : defaultModel;
        TraceContext context = tracer.startTrace("Gemini-" + model);

        if (!isAvailable()) {
            tracer.endTrace(context, false);
            throw new AiClientException(
                    "Gemini API 키가 설정되지 않았습니다",
                    "gemini",
                    AiClientException.ErrorType.INVALID_API_KEY
            );
        }

        try {
            // 프롬프트 조합 (system + user)
            String combinedPrompt = buildPrompt(request);

            // 요청 로깅
            tracer.logRequest(context, "Gemini", model, combinedPrompt,
                    request.getTemperature(), request.getMaxTokens());

            // 요청 바디 생성
            Map<String, Object> requestBody = buildRequestBody(combinedPrompt, request);

            // 타임아웃 설정
            int timeoutSeconds = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 60;

            log.debug("[Gemini] API 호출 시작 - 모델: {}, 온도: {}, 최대토큰: {}",
                    model, request.getTemperature(), request.getMaxTokens());

            // API 호출
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            // 응답 파싱 (로깅 포함)
            return parseResponse(response, context);

        } catch (WebClientResponseException e) {
            log.error("[Gemini] API 호출 실패 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            // 에러 로깅
            tracer.logError(context, e);
            tracer.endTrace(context, false);

            AiClientException.ErrorType errorType = classifyHttpError(e.getStatusCode());

            throw new AiClientException(
                    "Gemini API 호출 실패: " + e.getMessage(),
                    e,
                    "gemini",
                    errorType
            );

        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패", e);

            // 에러 로깅
            tracer.logError(context, e);
            tracer.endTrace(context, false);

            AiClientException.ErrorType errorType = classifyError(e);

            throw new AiClientException(
                    "Gemini API 호출 실패: " + e.getMessage(),
                    e,
                    "gemini",
                    errorType
            );
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("your-");
    }

    /**
     * System 프롬프트와 User 프롬프트를 결합
     */
    private String buildPrompt(AiRequest request) {
        StringBuilder prompt = new StringBuilder();

        if (request.getSystem() != null && !request.getSystem().isEmpty()) {
            prompt.append(request.getSystem()).append("\n\n");
        }

        prompt.append(request.getUser());

        return prompt.toString();
    }

    /**
     * Gemini API 요청 바디 생성
     */
    private Map<String, Object> buildRequestBody(String prompt, AiRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // contents 배열
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        // generationConfig
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", request.getTemperature());
        generationConfig.put("maxOutputTokens", request.getMaxTokens());
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Gemini API 응답 파싱
     */
    private AiResult parseResponse(String response, TraceContext context) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // candidates[0].content.parts[0].text 추출
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                tracer.logParsingFailure(context, "Gemini 응답에 candidates가 없습니다");
                throw new AiClientException(
                        "Gemini 응답에 candidates가 없습니다",
                        "gemini",
                        AiClientException.ErrorType.PARSING_ERROR
                );
            }

            String text = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // 토큰 사용량 추출 (선택적)
            Integer tokensUsed = null;
            JsonNode usageMetadata = root.path("usageMetadata");
            if (!usageMetadata.isMissingNode()) {
                tokensUsed = usageMetadata.path("totalTokenCount").asInt();
            }

            log.debug("[Gemini] API 호출 성공 - 사용 토큰: {}", tokensUsed);

            // 응답 로깅
            tracer.logResponse(context, text, tokensUsed);

            // 파싱 성공
            tracer.logParsingSuccess(context, "Text");

            // Trace 종료
            tracer.endTrace(context, true);

            return AiResult.builder()
                    .text(text)
                    .raw(response)
                    .provider("gemini")
                    .tokensUsed(tokensUsed)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[Gemini] 응답 파싱 실패", e);
            tracer.logParsingFailure(context, e.getMessage());
            tracer.endTrace(context, false);
            throw new AiClientException(
                    "Gemini 응답 파싱 실패: " + e.getMessage(),
                    e,
                    "gemini",
                    AiClientException.ErrorType.PARSING_ERROR
            );
        }
    }

    /**
     * HTTP 상태 코드로 ErrorType 분류
     */
    private AiClientException.ErrorType classifyHttpError(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 401, 403 -> AiClientException.ErrorType.INVALID_API_KEY;
            case 429 -> AiClientException.ErrorType.RATE_LIMIT;
            default -> AiClientException.ErrorType.UNKNOWN_ERROR;
        };
    }

    /**
     * 예외를 분류하여 ErrorType 반환
     */
    private AiClientException.ErrorType classifyError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("timeout") || e.getClass().getName().contains("Timeout")) {
            return AiClientException.ErrorType.TIMEOUT;
        } else if (message.contains("connection") || message.contains("network")) {
            return AiClientException.ErrorType.NETWORK_ERROR;
        } else {
            return AiClientException.ErrorType.UNKNOWN_ERROR;
        }
    }
}