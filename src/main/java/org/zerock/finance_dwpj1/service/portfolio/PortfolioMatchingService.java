package org.zerock.finance_dwpj1.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.entity.portfolio.Investor13FHolding;
import org.zerock.finance_dwpj1.entity.portfolio.InvestorProfile;
import org.zerock.finance_dwpj1.repository.portfolio.Investor13FHoldingRepository;
import org.zerock.finance_dwpj1.repository.portfolio.InvestorProfileRepository;
import org.zerock.finance_dwpj1.service.portfolio.similarity.SimilarityCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오 매칭 서비스
 * 사용자 포트폴리오와 투자대가 13F 포트폴리오 간의 유사도를 계산합니다
 *
 * <h2>주요 변경사항 (2025-12-15)</h2>
 * <ul>
 *   <li>레버리지/인버스 ETF를 기초자산 노출(exposure) 기준으로 환산</li>
 *   <li>유사도 계산을 SimilarityCalculator로 위임 (전략 패턴)</li>
 *   <li>정규화 모드 지원 (LONG_ONLY / LONG_SHORT)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioMatchingService {

    private final InvestorProfileRepository profileRepository;
    private final Investor13FHoldingRepository holdingRepository;
    private final PortfolioExposureNormalizer exposureNormalizer;
    private final SimilarityCalculator similarityCalculator;
    /**
     * 사용자의 상위 N개 종목이 투자대가와 얼마나 겹치는지 점수화 (0~100)
     */
    private double calculateTopOverlapScore(Map<String, Double> userPortfolio,
                                            Map<String, Double> investorPortfolio,
                                            int topN) {

        // 1) 사용자의 상위 N개 종목 추출
        List<String> topUserTickers = userPortfolio.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 비중 내림차순
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 2) 상위 종목이 얼마나 겹치는지 계산 (1개당 동일한 점수)
        double perStockScore = 100.0 / topN;
        double score = 0.0;

        for (String ticker : topUserTickers) {
            if (investorPortfolio.containsKey(ticker)) {
                score += perStockScore;
            }
        }

        return score; // 0 ~ 100
    }
    /**
     * 최종 매칭 점수 계산
     * - similarity: 가중치 코사인 유사도 (0~100)
     * - overlapPercentage: 종목 겹침 비율 (0~100)
     * - topOverlapScore: 상위 종목 겹침 점수 (0~100)
     */
    private double calculateMatchScore(double similarity,
                                       double overlapPercentage,
                                       Map<String, Double> userPortfolio,
                                       Map<String, Double> investorPortfolio) {

        // 사용자의 상위 5개 종목 기준
        double topOverlapScore = calculateTopOverlapScore(userPortfolio, investorPortfolio, 5);

        // 가중치 조합 (사람이 보는 감각에 가깝게)
        return similarity * 0.4
                + overlapPercentage * 0.4
                + topOverlapScore * 0.2;
    }


    /**
     * 사용자 포트폴리오와 투자대가를 매칭하여 TOP 3를 반환합니다.
     *
     * <p><b>핵심 변경사항 (2025-12-15)</b>:</p>
     * <ul>
     *   <li>레버리지/인버스 ETF를 기초자산 노출 기준으로 환산</li>
     *   <li>사용자 포트폴리오와 투자대가 포트폴리오 모두 노출 환산 적용</li>
     *   <li>환산된 포트폴리오 기준으로 유사도 계산</li>
     * </ul>
     *
     * <h3>처리 흐름</h3>
     * <pre>
     * 1. 사용자 포트폴리오 노출 환산
     *    { "TSLL": 10% } → { "TSLA": 20% }
     *
     * 2. 투자대가별 반복:
     *    a. 13F 데이터 조회
     *    b. 투자대가 포트폴리오 노출 환산
     *    c. 유사도 계산 (환산된 포트폴리오 기준)
     *    d. 종목 겹침 분석 (환산된 티커 기준)
     *    e. 최종 점수 계산
     *
     * 3. TOP 3 정렬 및 반환
     * </pre>
     *
     * @param userPortfolio 사용자 포트폴리오 (원본, 레버리지 상품 포함 가능)
     * @return TOP 3 매칭 결과 리스트
     */
    public List<MatchResult> findTopMatches(Map<String, Double> userPortfolio) {
        log.info("=== 포트폴리오 매칭 시작 ===");
        log.info("사용자 포트폴리오 (원본): {}", userPortfolio);

        // ★ 핵심 변경: 사용자 포트폴리오를 노출 기준으로 환산 + 정규화
        Map<String, Double> userExposure = exposureNormalizer.normalize(userPortfolio);
        log.info("사용자 포트폴리오 (노출 환산 후): {}", userExposure);

        List<MatchResult> results = new ArrayList<>();

        // 활성화된 모든 투자대가 조회
        List<InvestorProfile> profiles = profileRepository.findByActiveTrue();
        log.info("비교 대상 투자대가: {}명", profiles.size());

        for (InvestorProfile profile : profiles) {
            try {
                // 투자대가의 최신 13F 보유 종목 조회
                List<Investor13FHolding> holdings = holdingRepository
                        .findLatestHoldingsByInvestor(profile.getInvestorId());

                if (holdings.isEmpty()) {
                    log.warn("{}의 13F 데이터가 없습니다. 건너뜁니다.", profile.getName());
                    continue;
                }

                // 투자대가 포트폴리오를 Map으로 변환 (티커 → 비중, 중복 티커 합산)
                Map<String, Double> investorPortfolioRaw = holdings.stream()
                        .collect(Collectors.toMap(
                                Investor13FHolding::getTicker,
                                Investor13FHolding::getPortfolioWeight,
                                Double::sum
                        ));

                // ★ 핵심 변경: 투자대가 포트폴리오도 노출 기준으로 환산 + 정규화
                Map<String, Double> investorExposure = exposureNormalizer.normalize(investorPortfolioRaw);
                log.debug("{} 포트폴리오 (노출 환산 후): {}", profile.getName(), investorExposure);

                // 1) 코사인 유사도 계산 (환산된 포트폴리오 기준)
                // ★ 핵심 변경: SimilarityCalculator로 위임
                double similarity = similarityCalculator.calculate(userExposure, investorExposure);

                // 2) 종목 겹침 분석 (환산된 티커 기준)
                List<String> matchedStocks = findMatchedStocks(userExposure, investorExposure);
                double overlapPercentage = calculateOverlapPercentage(userExposure, investorExposure);

                // 3) 최종 매칭 점수 (사람 느낌)
                double matchScore = calculateMatchScore(
                        similarity,
                        overlapPercentage,
                        userExposure,
                        investorExposure
                );

                // 4) 개선 제안 생성 (환산된 포트폴리오 기준)
                String suggestions = generateSuggestions(userExposure, investorExposure, profile);

                MatchResult result = MatchResult.builder()
                        .investorId(profile.getInvestorId())
                        .investorName(profile.getName())
                        // similarity 필드에는 "최종 점수"를 저장 (퍼센트)
                        .similarity(Math.round(matchScore))
                        .matchedStocks(matchedStocks)
                        .overlapPercentage(Math.round(overlapPercentage))
                        .philosophy(profile.getPhilosophy())
                        .strengths(profile.getStrengths())
                        .weaknesses(profile.getWeaknesses())
                        .suggestions(suggestions)
                        .totalHoldings(holdings.size())
                        .build();

                results.add(result);

                log.info("{}: 코사인={}%, 종목겹침={}%, 상위5겹침Score={}%, 최종매칭Score={}%, 겹치는종목 {}개",
                        profile.getName(),
                        Math.round(similarity),
                        Math.round(overlapPercentage),
                        Math.round(calculateTopOverlapScore(userExposure, investorExposure, 5)),
                        result.getSimilarity(),
                        matchedStocks.size()
                );

            } catch (Exception e) {
                log.error("{}와의 매칭 중 오류", profile.getName(), e);
            }
        }

        // 유사도 내림차순 정렬 후 TOP 3 반환
        results.sort((a, b) -> Long.compare(b.getSimilarity(), a.getSimilarity()));

        List<MatchResult> top3 = results.stream()
                .limit(3)
                .collect(Collectors.toList());

        log.info("=== TOP 3 매칭 결과 ===");
        for (int i = 0; i < top3.size(); i++) {
            MatchResult r = top3.get(i);
            log.info("{}위: {} ({}%)", i + 1, r.getInvestorName(), r.getSimilarity());
        }

        return top3;
    }

    /**
     * 가중치 코사인 유사도 계산
     *
     * 코사인 유사도 = (A · B) / (||A|| × ||B||)
     * A와 B는 비중 벡터
     */
    private double calculateWeightedSimilarity(Map<String, Double> portfolio1,
                                               Map<String, Double> portfolio2) {
        // 모든 종목의 합집합
        Set<String> allTickers = new HashSet<>();
        allTickers.addAll(portfolio1.keySet());
        allTickers.addAll(portfolio2.keySet());

        // 벡터 내적 (dot product)
        double dotProduct = 0.0;

        // 벡터 크기 (magnitude)
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (String ticker : allTickers) {
            double weight1 = portfolio1.getOrDefault(ticker, 0.0);
            double weight2 = portfolio2.getOrDefault(ticker, 0.0);

            dotProduct += weight1 * weight2;
            magnitude1 += weight1 * weight1;
            magnitude2 += weight2 * weight2;
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        // 0으로 나누기 방지
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }

        // 코사인 유사도 (0~1) → 퍼센트 (0~100)
        double cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
        return cosineSimilarity * 100.0;
    }

    /**
     * 겹치는 종목 찾기
     */
    private List<String> findMatchedStocks(Map<String, Double> portfolio1,
                                           Map<String, Double> portfolio2) {
        return portfolio1.keySet().stream()
                .filter(portfolio2::containsKey)
                .collect(Collectors.toList());
    }

    /**
     * 종목 겹침 비율 계산
     */
    private double calculateOverlapPercentage(Map<String, Double> portfolio1,
                                              Map<String, Double> portfolio2) {
        int totalStocks = portfolio1.size();
        if (totalStocks == 0) {
            return 0.0;
        }

        long matchedCount = portfolio1.keySet().stream()
                .filter(portfolio2::containsKey)
                .count();

        return (matchedCount * 100.0) / totalStocks;
    }

    /**
     * 개선 제안 생성
     */
    private String generateSuggestions(Map<String, Double> userPortfolio,
                                       Map<String, Double> investorPortfolio,
                                       InvestorProfile profile) {
        StringBuilder suggestions = new StringBuilder();

        // 1. 분산 투자 분석
        int userStockCount = userPortfolio.size();
        if (userStockCount < 5) {
            suggestions.append("• 종목 수가 적습니다 (").append(userStockCount)
                    .append("개). 10개 이상으로 분산 투자를 권장합니다.\n");
        } else if (userStockCount > 30) {
            suggestions.append("• 종목이 너무 많습니다 (").append(userStockCount)
                    .append("개). 관리가 어려울 수 있습니다.\n");
        }

        // 2. 집중도 분석
        double maxWeight = userPortfolio.values().stream()
                .max(Double::compare)
                .orElse(0.0);

        if (maxWeight > 40) {
            suggestions.append("• 특정 종목 비중이 과도합니다 (최대 ")
                    .append(Math.round(maxWeight))
                    .append("%). 리스크 분산을 고려하세요.\n");
        }

        // 3. 투자대가 스타일 반영
        List<String> missingStocks = investorPortfolio.keySet().stream()
                .filter(ticker -> !userPortfolio.containsKey(ticker))
                .limit(3)
                .collect(Collectors.toList());

        if (!missingStocks.isEmpty()) {
            suggestions.append("• ").append(profile.getName())
                    .append("가 보유한 주요 종목 추가 고려: ")
                    .append(String.join(", ", missingStocks))
                    .append("\n");
        }

        return suggestions.toString();
    }

    /**
     * 매칭 결과 DTO
     */
    public static class MatchResult {
        private String investorId;
        private String investorName;
        private long similarity; // 유사도 (%)
        private List<String> matchedStocks; // 겹치는 종목
        private long overlapPercentage; // 종목 겹침 비율 (%)
        private String philosophy; // 투자 철학
        private String strengths; // 장점
        private String weaknesses; // 단점
        private String suggestions; // 개선 제안
        private int totalHoldings; // 투자대가 전체 보유 종목 수

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String investorId;
            private String investorName;
            private long similarity;
            private List<String> matchedStocks;
            private long overlapPercentage;
            private String philosophy;
            private String strengths;
            private String weaknesses;
            private String suggestions;
            private int totalHoldings;

            public Builder investorId(String investorId) {
                this.investorId = investorId;
                return this;
            }

            public Builder investorName(String investorName) {
                this.investorName = investorName;
                return this;
            }

            public Builder similarity(long similarity) {
                this.similarity = similarity;
                return this;
            }

            public Builder matchedStocks(List<String> matchedStocks) {
                this.matchedStocks = matchedStocks;
                return this;
            }

            public Builder overlapPercentage(long overlapPercentage) {
                this.overlapPercentage = overlapPercentage;
                return this;
            }

            public Builder philosophy(String philosophy) {
                this.philosophy = philosophy;
                return this;
            }

            public Builder strengths(String strengths) {
                this.strengths = strengths;
                return this;
            }

            public Builder weaknesses(String weaknesses) {
                this.weaknesses = weaknesses;
                return this;
            }

            public Builder suggestions(String suggestions) {
                this.suggestions = suggestions;
                return this;
            }

            public Builder totalHoldings(int totalHoldings) {
                this.totalHoldings = totalHoldings;
                return this;
            }

            public MatchResult build() {
                MatchResult result = new MatchResult();
                result.investorId = this.investorId;
                result.investorName = this.investorName;
                result.similarity = this.similarity;
                result.matchedStocks = this.matchedStocks;
                result.overlapPercentage = this.overlapPercentage;
                result.philosophy = this.philosophy;
                result.strengths = this.strengths;
                result.weaknesses = this.weaknesses;
                result.suggestions = this.suggestions;
                result.totalHoldings = this.totalHoldings;
                return result;
            }
        }

        // Getters
        public String getInvestorId() { return investorId; }
        public String getInvestorName() { return investorName; }
        public long getSimilarity() { return similarity; }
        public List<String> getMatchedStocks() { return matchedStocks; }
        public long getOverlapPercentage() { return overlapPercentage; }
        public String getPhilosophy() { return philosophy; }
        public String getStrengths() { return strengths; }
        public String getWeaknesses() { return weaknesses; }
        public String getSuggestions() { return suggestions; }
        public int getTotalHoldings() { return totalHoldings; }
    }
}
