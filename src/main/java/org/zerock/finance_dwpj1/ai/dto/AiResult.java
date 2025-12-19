package org.zerock.finance_dwpj1.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 클라이언트 응답 DTO
 * OpenAI와 Gemini API 응답을 통합
 */
@Data
@Builder
public class AiResult {

    /**
     * 생성된 텍스트 (파싱된 메시지)
     */
    private String text;

    /**
     * 원본 응답 (디버깅용)
     */
    private String raw;

    /**
     * Provider 이름 ("openai" or "gemini")
     */
    private String provider;

    /**
     * 사용된 토큰 수 (가능한 경우)
     */
    private Integer tokensUsed;

    /**
     * 요청 성공 여부
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;
}