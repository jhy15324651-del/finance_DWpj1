package org.zerock.finance_dwpj1.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zerock.finance_dwpj1.ai.client.AiClient;
import org.zerock.finance_dwpj1.ai.client.GeminiAiClient;
import org.zerock.finance_dwpj1.ai.client.OpenAiClient;
import org.zerock.finance_dwpj1.ai.logging.AiCallTracer;

/**
 * AI 클라이언트 빈 구성
 * application.properties의 ai.provider 값에 따라 구현체 스위칭
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class AiClientConfig {

    private final AiCallTracer tracer;

    /**
     * OpenAI 클라이언트 빈
     * ai.provider=openai일 때 활성화
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public AiClient openAiClient() {
        log.info("========================================");
        log.info("AI Provider: OpenAI");
        log.info("========================================");
        return new OpenAiClient(tracer);
    }

    /**
     * Gemini 클라이언트 빈
     * ai.provider=gemini일 때 활성화 (기본값)
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
    public AiClient geminiAiClient() {
        log.info("========================================");
        log.info("AI Provider: Google Gemini");
        log.info("========================================");
        return new GeminiAiClient(tracer);
    }
}