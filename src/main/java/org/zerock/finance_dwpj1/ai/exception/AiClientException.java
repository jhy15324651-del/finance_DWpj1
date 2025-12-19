package org.zerock.finance_dwpj1.ai.exception;

/**
 * AI 클라이언트 공통 예외
 * OpenAI, Gemini API 호출 중 발생하는 모든 예외를 래핑
 */
public class AiClientException extends RuntimeException {

    private final String provider;
    private final ErrorType errorType;

    public AiClientException(String message, String provider, ErrorType errorType) {
        super(message);
        this.provider = provider;
        this.errorType = errorType;
    }

    public AiClientException(String message, Throwable cause, String provider, ErrorType errorType) {
        super(message, cause);
        this.provider = provider;
        this.errorType = errorType;
    }

    public String getProvider() {
        return provider;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * AI 클라이언트 에러 타입
     */
    public enum ErrorType {
        INVALID_API_KEY("API 키가 유효하지 않습니다"),
        RATE_LIMIT("API 호출 한도 초과 (429)"),
        TIMEOUT("API 호출 타임아웃"),
        NETWORK_ERROR("네트워크 오류"),
        PARSING_ERROR("응답 파싱 오류"),
        UNKNOWN_ERROR("알 수 없는 오류");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}