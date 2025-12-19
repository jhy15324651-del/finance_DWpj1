package org.zerock.finance_dwpj1.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.ai.client.AiClient;
import org.zerock.finance_dwpj1.ai.dto.AiRequest;
import org.zerock.finance_dwpj1.ai.dto.AiResult;
import org.zerock.finance_dwpj1.ai.exception.AiClientException;

import java.util.List;

/**
 * GPT 서비스 (리팩토링 버전)
 * 기존 메서드 시그니처 유지하며 내부적으로 AiClient 사용
 * OpenAI ↔ Gemini 스위칭 지원
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class GPTService {

    private final AiClient aiClient;

    public String translateAndSummarizeNews(String newsText) {
        try {
            AiRequest request = AiRequest.builder()
                    .system("당신은 금융 뉴스 번역 및 요약 전문가입니다. 영어 뉴스를 한국어로 번역하고, 중요 키워드를 추출하며, 3줄로 요약해주세요.")
                    .user(String.format(
                            "다음 뉴스를 번역하고 분석해주세요:\n\n%s\n\n" +
                                    "응답 형식:\n" +
                                    "제목: [번역된 제목]\n" +
                                    "키워드: [키워드1], [키워드2], [키워드3]\n" +
                                    "요약1: [첫번째 요약]\n" +
                                    "요약2: [두번째 요약]\n" +
                                    "요약3: [세번째 요약]", newsText))
                    .temperature(0.7)
                    .maxTokens(500)
                    .timeoutSeconds(60)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류: {}", e.getProvider(), e.getMessage());
            return null;
        }
    }

    public String translateTweet(String tweetText) {
        try {
            AiRequest request = AiRequest.builder()
                    .system("당신은 금융 관련 트윗 번역 전문가입니다. 영어 트윗을 자연스러운 한국어로 번역해주세요.")
                    .user(String.format("다음 트윗을 한국어로 번역해주세요:\n\n%s", tweetText))
                    .temperature(0.7)
                    .maxTokens(200)
                    .timeoutSeconds(60)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류: {}", e.getProvider(), e.getMessage());
            return tweetText; // 번역 실패 시 원문 반환
        }
    }

    public String analyzeInvestorPhilosophy(String investorId) {
        try {
            AiRequest request = AiRequest.builder()
                    .system("당신은 투자 전문가입니다. 유명 투자자의 투자 철학을 분석하고 인사이트를 제공해주세요.")
                    .user(String.format(
                            "%s 투자자의 투자 철학을 분석하여 한국어로 2-3문장의 핵심 인사이트를 제공해주세요.",
                            getInvestorName(investorId)))
                    .temperature(0.7)
                    .maxTokens(300)
                    .timeoutSeconds(60)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류: {}", e.getProvider(), e.getMessage());
            return "투자자 분석 정보를 가져올 수 없습니다.";
        }
    }

    public String searchInvestorInfo(String investorName) {
        try {
            AiRequest request = AiRequest.builder()
                    .system("당신은 금융 및 투자 전문가입니다. 투자자에 대한 정보를 제공해주세요.")
                    .user(String.format(
                            "%s 투자자에 대해 한국어로 간단히 설명해주세요. " +
                                    "투자 스타일(예: 가치투자, 성장주 투자 등)과 주요 투자 철학을 2-3문장으로 요약해주세요.",
                            investorName))
                    .temperature(0.7)
                    .maxTokens(300)
                    .timeoutSeconds(60)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류: {}", e.getProvider(), e.getMessage());
            return "투자자 정보를 가져올 수 없습니다. AI API 키를 확인해주세요.";
        }
    }

    /**
     * 포트폴리오 추천 생성 (Gemini 최적화)
     * Gemini에서는 충분한 토큰으로 완전한 포트폴리오 생성
     */
    public String generatePortfolioRecommendation(List<String> investorIds) {
        try {
            // 투자자 이름 리스트 생성
            StringBuilder investorNames = new StringBuilder();
            for (String id : investorIds) {
                investorNames.append(getInvestorName(id)).append(", ");
            }
            String investorsStr = investorNames.substring(0, investorNames.length() - 2);

            // Gemini는 프리미엄 모델 사용, OpenAI는 gpt-4 사용
            String model = null;
            int maxTokens = 2500; // Gemini 기준으로 충분히 증가

            if (aiClient.getProviderName().equals("openai")) {
                model = "gpt-4";
                maxTokens = 2000; // OpenAI gpt-4 기준
            } else if (aiClient.getProviderName().equals("gemini")) {
                model = "gemini-1.5-pro"; // Gemini 프리미엄 모델 명시
            }

            AiRequest request = AiRequest.builder()
                    .system("당신은 세계적인 투자 전문가입니다. 여러 투자자의 투자 철학을 분석하고 이를 균등하게 혼합한 포트폴리오를 추천해주세요. 모든 응답을 완전히 마무리하고, 끊기지 않도록 해주세요.")
                    .user(String.format(
                            "다음 4명의 투자자 철학을 각각 25%%씩 반영한 추천 포트폴리오를 만들어주세요:\n\n" +
                                    "투자자: %s\n\n" +
                                    "다음 형식으로 응답해주세요:\n\n" +
                                    "1. 통합 투자 철학:\n" +
                                    "[4명의 투자자 철학을 25%%씩 혼합한 전체적인 투자 접근법을 2-3문장으로 설명]\n\n" +
                                    "2. 추천 종목 (5-7개):\n" +
                                    "각 종목별로 다음 정보를 제공:\n" +
                                    "- 종목명 (티커)\n" +
                                    "- 섹터\n" +
                                    "- 비중 (%%)\n" +
                                    "- 선정 이유 (어떤 투자자의 철학이 반영되었는지 포함)\n\n" +
                                    "3. 포트폴리오 설명:\n" +
                                    "[전체 포트폴리오의 특징과 기대 효과를 2-3문장으로 설명]\n\n" +
                                    "4. 리스크 프로필:\n" +
                                    "[이 포트폴리오의 리스크 수준과 주의사항을 1-2문장으로 설명]",
                            investorsStr))
                    .temperature(0.8)
                    .maxTokens(maxTokens)
                    .timeoutSeconds(120) // 타임아웃 증가
                    .model(model)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류 (포트폴리오 생성): {}", e.getProvider(), e.getMessage());
            return "포트폴리오 추천을 생성할 수 없습니다. API 키를 확인하거나 나중에 다시 시도해주세요.";
        }
    }

    /**
     * 합의형 포트폴리오 생성 (JSON ONLY)
     * 4명의 투자자가 회의실에서 토론 후 모두 동의했을 법한 합의 종목 10개 선정
     * - 단순 25% 혼합 금지
     * - JSON 형식으로만 응답
     * - ticker 필수, weightPercent 합계 100
     */
    public String generateConsensusPortfolio(List<String> investorIds) {
        try {
            // 투자자 이름 리스트 생성
            StringBuilder investorNames = new StringBuilder();
            for (String id : investorIds) {
                investorNames.append(getInvestorName(id)).append(", ");
            }
            String investorsStr = investorNames.substring(0, investorNames.length() - 2);

            // Gemini 프리미엄 모델 사용 (더 정확한 JSON 생성)
            String model = null;
            int maxTokens = 3000;

            if (aiClient.getProviderName().equals("openai")) {
                model = "gpt-4";
                maxTokens = 2500;
            } else if (aiClient.getProviderName().equals("gemini")) {
                model = "gemini-2.0-flash-exp"; // Gemini 2.0 사용
            }

            String systemPrompt = "당신은 세계적인 투자 전문가입니다. 반드시 JSON 형식으로만 응답하세요. 다른 텍스트나 마크다운 없이 순수 JSON만 출력하세요.";

            String userPrompt = String.format(
                    "다음 4명의 투자자 (%s)가 한 회의실에 모여 토론했습니다.\n\n" +
                            "**핵심 가정: 이들이 토론 끝에 '모두가 동의한 합의 종목 10개'를 선정합니다.**\n\n" +
                            "- 단순히 각자 포트폴리오를 25%%씩 섞는 것이 아닙니다.\n" +
                            "- 4명이 모두 '이 종목은 투자할 가치가 있다'고 동의해야 선정됩니다.\n" +
                            "- 각 종목은 4명의 투자 철학이 동시에 반영되어야 합니다.\n\n" +
                            "**JSON 스키마 (이 형식만 출력):**\n" +
                            "```json\n" +
                            "{\n" +
                            "  \"investmentCommitteePhilosophy\": \"3~4문장으로 설명\",\n" +
                            "  \"stocks\": [\n" +
                            "    {\n" +
                            "      \"company\": \"회사명\",\n" +
                            "      \"ticker\": \"AAPL\",\n" +
                            "      \"sector\": \"Technology\",\n" +
                            "      \"weightPercent\": 15.0,\n" +
                            "      \"consensusReason\": \"4명이 모두 동의한 이유 (각 투자자 철학 언급)\",\n" +
                            "      \"longTermView\": \"5~10년 전망\"\n" +
                            "    }\n" +
                            "  ],\n" +
                            "  \"portfolioCharacteristics\": [\"특징1\", \"특징2\", \"특징3\"],\n" +
                            "  \"riskNotes\": [\"리스크1\", \"리스크2\", \"리스크3\"]\n" +
                            "}\n" +
                            "```\n\n" +
                            "**필수 조건:**\n" +
                            "1. stocks 배열은 정확히 10개\n" +
                            "2. ticker는 반드시 포함 (실제 미국 주식 ticker)\n" +
                            "3. weightPercent 합계 = 100 (오차 ±0.1 허용)\n" +
                            "4. consensusReason은 4명의 철학이 모두 반영되어야 함\n\n" +
                            "**응답 형식:** 순수 JSON만 출력하고, ```json``` 마크다운이나 다른 텍스트는 절대 포함하지 마세요.",
                    investorsStr
            );

            AiRequest request = AiRequest.builder()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .temperature(0.3) // 낮은 temperature로 일관성 확보
                    .maxTokens(maxTokens)
                    .timeoutSeconds(120)
                    .model(model)
                    .build();

            AiResult result = aiClient.generate(request);
            String response = result.getText().trim();

            // 마크다운 코드 블록 제거 (```json ... ```)
            if (response.startsWith("```json")) {
                response = response.substring(7);
            }
            if (response.startsWith("```")) {
                response = response.substring(3);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }

            return response.trim();

        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류 (합의형 포트폴리오 생성): {}", e.getProvider(), e.getMessage());
            throw new RuntimeException("포트폴리오 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 뉴스 요약 생성 (더 이상 사용 안 함 - summary 필드 제거로 deprecated)
     * @param content 뉴스 본문
     * @return AI 요약 (3-4문장)
     * @deprecated summary 필드를 사용하지 않으므로 더 이상 필요 없음. 번역된 content만 사용.
     */
    @Deprecated
    public String summarizeNews(String content) {
        try {
            if (content == null || content.isEmpty()) {
                return "요약을 생성할 수 없습니다.";
            }

            // 내용이 너무 길면 앞부분만 사용 (토큰 제한)
            String truncatedContent = content.length() > 2000 ?
                    content.substring(0, 2000) + "..." : content;

            AiRequest request = AiRequest.builder()
                    .system("당신은 금융 뉴스 요약 전문가입니다. 핵심 내용과 시장 영향을 중심으로 요약해주세요.")
                    .user(String.format("다음 금융 뉴스를 3-4문장으로 요약해주세요:\n\n%s", truncatedContent))
                    .temperature(0.7)
                    .maxTokens(400) // Gemini 기준 증가
                    .timeoutSeconds(60)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] 요약 생성 중 오류: {}", e.getProvider(), e.getMessage());
            return "요약 생성에 실패했습니다.";
        }
    }

    /**
     * 뉴스 한국어 번역 (Gemini 최적화)
     * @param englishContent 영어 원문
     * @return 한국어 번역본
     */
    public String translateNewsToKorean(String englishContent) {
        try {
            if (englishContent == null || englishContent.isEmpty()) {
                return "번역할 내용이 없습니다.";
            }

            // 내용이 너무 길면 앞부분만 사용 (Gemini는 더 긴 입력 처리 가능)
            String truncatedContent = englishContent.length() > 5000 ?
                    englishContent.substring(0, 5000) + "..." : englishContent;

            AiRequest request = AiRequest.builder()
                    .system("당신은 금융 뉴스 번역 전문가입니다. 영어 뉴스를 자연스러운 한국어로 번역해주세요. 원문의 모든 내용을 빠짐없이 번역하되, 자연스러운 한국어 표현을 사용하세요.")
                    .user(String.format("다음 금융 뉴스를 한국어로 번역해주세요:\n\n%s", truncatedContent))
                    .temperature(0.3) // 번역은 더 정확하게
                    .maxTokens(2000) // Gemini는 긴 번역 출력 가능
                    .timeoutSeconds(90)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] 번역 중 오류: {}", e.getProvider(), e.getMessage());
            return englishContent; // 번역 실패 시 원문 반환
        }
    }

    /**
     * 범용 AI 응답 생성 메서드
     * @param prompt 프롬프트
     * @return AI 응답
     */
    public String generateResponse(String prompt) {
        try {
            AiRequest request = AiRequest.builder()
                    .user(prompt)
                    .temperature(0.7)
                    .maxTokens(500)
                    .timeoutSeconds(60)
                    .build();

            AiResult result = aiClient.generate(request);
            return result.getText();
        } catch (AiClientException e) {
            log.error("[{}] API 호출 오류: {}", e.getProvider(), e.getMessage());
            return "AI 응답을 생성할 수 없습니다.";
        }
    }

    private String getInvestorName(String investorId) {
        return switch (investorId) {
            case "wood" -> "캐시 우드 (Cathie Wood)";
            case "soros" -> "조지 소로스 (George Soros)";
            case "thiel" -> "피터 틸 (Peter Thiel)";
            case "fink" -> "래리 핑크 (Larry Fink)";
            case "buffett" -> "워렌 버핏 (Warren Buffett)";
            case "lynch" -> "피터 린치 (Peter Lynch)";
            case "dalio" -> "레이 달리오 (Ray Dalio)";
            case "graham" -> "벤저민 그레이엄 (Benjamin Graham)";
            case "simons" -> "짐 사이먼스 (Jim Simons)";
            default -> investorId;
        };
    }
}