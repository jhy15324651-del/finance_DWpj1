package org.zerock.finance_dwpj1.ai.client;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.zerock.finance_dwpj1.ai.dto.AiRequest;
import org.zerock.finance_dwpj1.ai.dto.AiResult;
import org.zerock.finance_dwpj1.ai.exception.AiClientException;
import org.zerock.finance_dwpj1.ai.logging.AiCallTracer;
import org.zerock.finance_dwpj1.ai.logging.AiCallTracer.TraceContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI API 클라이언트 구현체
 * 기존 GPTService 로직을 AiClient 인터페이스로 통합
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiClient implements AiClient {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String defaultModel;

    private final AiCallTracer tracer;

    @Override
    public AiResult generate(AiRequest request) {
        // Trace 시작
        TraceContext context = tracer.startTrace("OpenAI-" + (request.getModel() != null ? request.getModel() : defaultModel));

        if (!isAvailable()) {
            tracer.endTrace(context, false);
            throw new AiClientException(
                    "OpenAI API 키가 설정되지 않았습니다",
                    "openai",
                    AiClientException.ErrorType.INVALID_API_KEY
            );
        }

        OpenAiService service = null;
        try {
            // 타임아웃 설정
            int timeoutSeconds = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 60;
            service = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));

            // 메시지 구성
            List<ChatMessage> messages = new ArrayList<>();

            // 시스템 메시지 추가
            if (request.getSystem() != null && !request.getSystem().isEmpty()) {
                messages.add(new ChatMessage("system", request.getSystem()));
            }

            // 사용자 메시지 추가
            messages.add(new ChatMessage("user", request.getUser()));

            // 모델 선택 (요청에 모델이 지정되어 있으면 우선 사용)
            String model = request.getModel() != null ? request.getModel() : defaultModel;

            // 프롬프트 조합 (로깅용)
            String combinedPrompt = (request.getSystem() != null ? request.getSystem() + "\n\n" : "") + request.getUser();

            // 요청 로깅
            tracer.logRequest(context, "OpenAI", model, combinedPrompt,
                    request.getTemperature(), request.getMaxTokens());

            // ChatCompletionRequest 생성
            ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(request.getTemperature())
                    .maxTokens(request.getMaxTokens());

            ChatCompletionRequest completionRequest = builder.build();

            // API 호출
            log.debug("[OpenAI] API 호출 시작 - 모델: {}, 온도: {}, 최대토큰: {}",
                    model, request.getTemperature(), request.getMaxTokens());

            var completion = service.createChatCompletion(completionRequest);

            String responseText = completion.getChoices().get(0).getMessage().getContent();

            // getTotalTokens()는 primitive long을 반환하므로 int로 캐스팅
            Integer tokensUsed = null;
            if (completion.getUsage() != null) {
                tokensUsed = (int) completion.getUsage().getTotalTokens();
            }

            log.debug("[OpenAI] API 호출 성공 - 사용 토큰: {}", tokensUsed);

            // 응답 로깅
            tracer.logResponse(context, responseText, tokensUsed);

            // 파싱 성공 (OpenAI는 일반 텍스트 반환)
            tracer.logParsingSuccess(context, "Text");

            // Trace 종료
            tracer.endTrace(context, true);

            return AiResult.builder()
                    .text(responseText)
                    .raw(responseText)
                    .provider("openai")
                    .tokensUsed(tokensUsed)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[OpenAI] API 호출 실패", e);

            // 에러 로깅
            tracer.logError(context, e);
            tracer.endTrace(context, false);

            // 에러 타입 분류
            AiClientException.ErrorType errorType = classifyError(e);

            throw new AiClientException(
                    "OpenAI API 호출 실패: " + e.getMessage(),
                    e,
                    "openai",
                    errorType
            );

        } finally {
            // 리소스 정리
            if (service != null) {
                try {
                    service.shutdownExecutor();
                } catch (Exception e) {
                    log.warn("[OpenAI] Executor 종료 실패", e);
                }
            }
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-openai-api-key-here");
    }

    /**
     * 예외를 분류하여 ErrorType 반환
     */
    private AiClientException.ErrorType classifyError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("unauthorized") || message.contains("invalid api key")) {
            return AiClientException.ErrorType.INVALID_API_KEY;
        } else if (message.contains("429") || message.contains("rate limit")) {
            return AiClientException.ErrorType.RATE_LIMIT;
        } else if (message.contains("timeout")) {
            return AiClientException.ErrorType.TIMEOUT;
        } else if (message.contains("network") || message.contains("connection")) {
            return AiClientException.ErrorType.NETWORK_ERROR;
        } else {
            return AiClientException.ErrorType.UNKNOWN_ERROR;
        }
    }
}