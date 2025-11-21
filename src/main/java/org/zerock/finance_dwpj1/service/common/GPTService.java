package org.zerock.finance_dwpj1.service.common;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class GPTService {

    @Value("${openai.api.key:your-api-key-here}")
    private String apiKey;

    public String translateAndSummarizeNews(String newsText) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 금융 뉴스 번역 및 요약 전문가입니다. 영어 뉴스를 한국어로 번역하고, 중요 키워드를 추출하며, 3줄로 요약해주세요."));
            messages.add(new ChatMessage("user", String.format(
                    "다음 뉴스를 번역하고 분석해주세요:\n\n%s\n\n" +
                            "응답 형식:\n" +
                            "제목: [번역된 제목]\n" +
                            "키워드: [키워드1], [키워드2], [키워드3]\n" +
                            "요약1: [첫번째 요약]\n" +
                            "요약2: [두번째 요약]\n" +
                            "요약3: [세번째 요약]", newsText)));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();
            return response;
        } catch (Exception e) {
            log.error("GPT API 호출 오류", e);
            return null;
        }
    }

    public String translateTweet(String tweetText) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 금융 관련 트윗 번역 전문가입니다. 영어 트윗을 자연스러운 한국어로 번역해주세요."));
            messages.add(new ChatMessage("user", String.format("다음 트윗을 한국어로 번역해주세요:\n\n%s", tweetText)));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(200)
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();
            return response;
        } catch (Exception e) {
            log.error("GPT API 호출 오류", e);
            return tweetText; // 번역 실패 시 원문 반환
        }
    }

    public String analyzeInvestorPhilosophy(String investorId) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 투자 전문가입니다. 유명 투자자의 투자 철학을 분석하고 인사이트를 제공해주세요."));
            messages.add(new ChatMessage("user", String.format(
                    "%s 투자자의 투자 철학을 분석하여 한국어로 2-3문장의 핵심 인사이트를 제공해주세요.",
                    getInvestorName(investorId))));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(300)
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();
            return response;
        } catch (Exception e) {
            log.error("GPT API 호출 오류", e);
            return "투자자 분석 정보를 가져올 수 없습니다.";
        }
    }

    public String searchInvestorInfo(String investorName) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 금융 및 투자 전문가입니다. 투자자에 대한 정보를 제공해주세요."));
            messages.add(new ChatMessage("user", String.format(
                    "%s 투자자에 대해 한국어로 간단히 설명해주세요. " +
                            "투자 스타일(예: 가치투자, 성장주 투자 등)과 주요 투자 철학을 2-3문장으로 요약해주세요.",
                    investorName)));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(300)
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();
            return response;
        } catch (Exception e) {
            log.error("GPT API 호출 오류", e);
            return "투자자 정보를 가져올 수 없습니다. GPT API 키를 확인해주세요.";
        }
    }

    public String generatePortfolioRecommendation(List<String> investorIds) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(90));

            // 투자자 이름 리스트 생성
            StringBuilder investorNames = new StringBuilder();
            for (String id : investorIds) {
                investorNames.append(getInvestorName(id)).append(", ");
            }
            String investorsStr = investorNames.substring(0, investorNames.length() - 2);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system",
                "당신은 세계적인 투자 전문가입니다. 여러 투자자의 투자 철학을 분석하고 이를 균등하게 혼합한 포트폴리오를 추천해주세요."));

            messages.add(new ChatMessage("user", String.format(
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
                investorsStr)));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(messages)
                    .temperature(0.8)
                    .maxTokens(1500)
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();
            return response;
        } catch (Exception e) {
            log.error("GPT API 호출 오류 (포트폴리오 생성)", e);
            return "포트폴리오 추천을 생성할 수 없습니다. API 키를 확인하거나 나중에 다시 시도해주세요.";
        }
    }

    /**
     * 범용 GPT 응답 생성 메서드
     * @param prompt 프롬프트
     * @return GPT 응답
     */
    public String generateResponse(String prompt) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("user", prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();
            return response;
        } catch (Exception e) {
            log.error("GPT API 호출 오류", e);
            return "GPT 응답을 생성할 수 없습니다.";
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