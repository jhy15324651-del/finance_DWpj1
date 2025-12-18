package org.zerock.finance_dwpj1.ai.client;

import org.zerock.finance_dwpj1.ai.dto.AiRequest;
import org.zerock.finance_dwpj1.ai.dto.AiResult;

/**
 * AI 클라이언트 인터페이스
 * OpenAI와 Gemini를 통합 추상화
 */
public interface AiClient {

    /**
     * AI 텍스트 생성
     *
     * @param request AI 요청 DTO
     * @return AI 응답 DTO
     */
    AiResult generate(AiRequest request);

    /**
     * Provider 이름 반환
     *
     * @return "openai" or "gemini"
     */
    String getProviderName();

    /**
     * Provider 사용 가능 여부 확인
     *
     * @return API 키가 설정되어 있으면 true
     */
    boolean isAvailable();
}