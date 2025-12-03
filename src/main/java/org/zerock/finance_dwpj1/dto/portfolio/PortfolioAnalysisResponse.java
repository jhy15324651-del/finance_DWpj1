package org.zerock.finance_dwpj1.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zerock.finance_dwpj1.service.portfolio.PortfolioMatchingService.MatchResult;

import java.util.List;

/**
 * 포트폴리오 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisResponse {

    /**
     * TOP 3 유사 투자대가 매칭 결과
     */
    private List<InvestorMatch> topMatches;

    /**
     * 더미 콘텐츠 추천 (추후 구현)
     */
    private List<ContentRecommendation> recommendedContents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestorMatch {
        private int rank; // 순위 (1, 2, 3)
        private String investorId;
        private String investorName;
        private long similarity; // 유사도 (%)
        private List<String> matchedStocks; // 겹치는 종목
        private long overlapPercentage; // 종목 겹침 비율 (%)
        private String philosophy; // 투자 철학
        private String strengths; // 장점
        private String weaknesses; // 단점
        private String suggestions; // 개선 제안
        private int totalHoldings; // 전체 보유 종목 수
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentRecommendation {
        private Long contentId;
        private String title;
        private String category;
        private double rating; // 평점
        private String thumbnailUrl;
        private String keyword; // 연관 키워드
    }

    /**
     * MatchResult를 InvestorMatch DTO로 변환
     */
    public static InvestorMatch fromMatchResult(MatchResult result, int rank) {
        return InvestorMatch.builder()
                .rank(rank)
                .investorId(result.getInvestorId())
                .investorName(result.getInvestorName())
                .similarity(result.getSimilarity())
                .matchedStocks(result.getMatchedStocks())
                .overlapPercentage(result.getOverlapPercentage())
                .philosophy(result.getPhilosophy())
                .strengths(result.getStrengths())
                .weaknesses(result.getWeaknesses())
                .suggestions(result.getSuggestions())
                .totalHoldings(result.getTotalHoldings())
                .build();
    }
}
