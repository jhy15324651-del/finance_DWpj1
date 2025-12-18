package org.zerock.finance_dwpj1.ai.logging;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AI 호출 트레이싱 및 로깅
 * traceId 기반으로 요청 → 응답 → 파싱 → DB 저장까지 추적
 */
@Component
@Slf4j
public class AiCallTracer {

    @Value("${ai.logging.enabled:true}")
    private boolean loggingEnabled;

    @Value("${ai.logging.max-prompt-length:500}")
    private int maxPromptLength;

    @Value("${ai.logging.max-response-length:1000}")
    private int maxResponseLength;

    /**
     * 새 Trace 시작
     */
    public TraceContext startTrace(String featureName) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        TraceContext context = TraceContext.builder()
                .traceId(traceId)
                .featureName(featureName)
                .startTime(System.currentTimeMillis())
                .build();

        if (loggingEnabled) {
            log.info("[TRACE:{}] 🎯 AI 호출 시작 - 기능: {}", traceId, featureName);
        }

        return context;
    }

    /**
     * AI 요청 로깅
     */
    public void logRequest(TraceContext context, String provider, String model,
                           String prompt, Double temperature, Integer maxTokens) {
        if (!loggingEnabled) return;

        String truncatedPrompt = truncateString(prompt, maxPromptLength);

        log.info("[TRACE:{}] 📤 요청 - Provider: {}, Model: {}, Temp: {}, MaxTokens: {}",
                context.getTraceId(), provider, model, temperature, maxTokens);
        log.info("[TRACE:{}] 📝 프롬프트 ({}자): {}{}",
                context.getTraceId(),
                prompt.length(),
                truncatedPrompt,
                prompt.length() > maxPromptLength ? "..." : "");
    }

    /**
     * AI 응답 로깅
     */
    public void logResponse(TraceContext context, String rawResponse, Integer tokensUsed) {
        if (!loggingEnabled) return;

        long elapsedMs = System.currentTimeMillis() - context.getStartTime();
        String truncatedResponse = truncateString(rawResponse, maxResponseLength);

        log.info("[TRACE:{}] 📥 응답 ({} ms) - 토큰: {}, 길이: {}자",
                context.getTraceId(), elapsedMs, tokensUsed, rawResponse.length());
        log.info("[TRACE:{}] 📄 응답 내용: {}{}",
                context.getTraceId(),
                truncatedResponse,
                rawResponse.length() > maxResponseLength ? "..." : "");

        // 응답 끊김 경고
        if (rawResponse.length() > 0 && !isResponseComplete(rawResponse)) {
            log.warn("[TRACE:{}] ⚠️ 응답 끊김 가능성 감지 - 마지막 문자: '{}'",
                    context.getTraceId(),
                    rawResponse.substring(Math.max(0, rawResponse.length() - 50)));
        }
    }

    /**
     * 파싱 성공 로깅
     */
    public void logParsingSuccess(TraceContext context, String resultType) {
        if (!loggingEnabled) return;

        log.info("[TRACE:{}] ✅ 파싱 성공 - 결과 타입: {}", context.getTraceId(), resultType);
    }

    /**
     * 파싱 실패 로깅
     */
    public void logParsingFailure(TraceContext context, String reason) {
        if (!loggingEnabled) return;

        log.error("[TRACE:{}] ❌ 파싱 실패 - 원인: {}", context.getTraceId(), reason);
    }

    /**
     * DB 저장 로깅
     */
    public void logDbSave(TraceContext context, String entityType, Long entityId) {
        if (!loggingEnabled) return;

        log.info("[TRACE:{}] 💾 DB 저장 완료 - {}: ID={}", context.getTraceId(), entityType, entityId);
    }

    /**
     * 에러 로깅
     */
    public void logError(TraceContext context, Exception e) {
        if (!loggingEnabled) return;

        long elapsedMs = System.currentTimeMillis() - context.getStartTime();
        log.error("[TRACE:{}] 💥 오류 발생 ({} ms) - {}: {}",
                context.getTraceId(), elapsedMs, e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * Trace 종료
     */
    public void endTrace(TraceContext context, boolean success) {
        if (!loggingEnabled) return;

        long elapsedMs = System.currentTimeMillis() - context.getStartTime();
        String status = success ? "✅ 성공" : "❌ 실패";

        log.info("[TRACE:{}] 🏁 AI 호출 종료 - 상태: {}, 소요시간: {} ms",
                context.getTraceId(), status, elapsedMs);
    }

    /**
     * 문자열 자르기 (앞/뒤 유지)
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }

        int halfLength = maxLength / 2;
        return str.substring(0, halfLength) + "\n...[중략]...\n" + str.substring(str.length() - halfLength);
    }

    /**
     * 응답 완성도 체크 (간단한 휴리스틱)
     */
    private boolean isResponseComplete(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        String trimmed = response.trim();

        // 마지막이 문장 종결 부호이면 완성된 것으로 간주
        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (lastChar == '.' || lastChar == '!' || lastChar == '?' || lastChar == '。' ||
                lastChar == '}' || lastChar == ']' || lastChar == ')') {
            return true;
        }

        // 너무 짧으면 끊긴 것으로 간주
        return trimmed.length() >= 10;
    }

    /**
     * Trace 컨텍스트
     */
    @Data
    @Builder
    public static class TraceContext {
        private String traceId;
        private String featureName;
        private long startTime;
    }
}