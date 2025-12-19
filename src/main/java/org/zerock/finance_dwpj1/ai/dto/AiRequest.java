package org.zerock.finance_dwpj1.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * AI 클라이언트 요청 DTO
 * OpenAI와 Gemini API 호출을 추상화
 */
@Data
@Builder
public class AiRequest {

    /**
     * 시스템 프롬프트 (AI의 역할/행동 지침)
     */
    private String system;

    /**
     * 사용자 프롬프트 (실제 질문/요청)
     */
    private String user;

    /**
     * 프롬프트 템플릿 치환용 변수
     */
    private Map<String, Object> variables;

    /**
     * 생성 온도 (0.0 ~ 1.0)
     * 낮을수록 결정적, 높을수록 창의적
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * 최대 생성 토큰 수
     */
    @Builder.Default
    private Integer maxTokens = 500;

    /**
     * 사용할 모델 (선택적)
     * null이면 기본 모델 사용
     */
    private String model;

    /**
     * 타임아웃 (초 단위, 선택적)
     */
    @Builder.Default
    private Integer timeoutSeconds = 60;
}