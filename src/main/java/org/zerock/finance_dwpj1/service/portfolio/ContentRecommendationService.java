package org.zerock.finance_dwpj1.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.portfolio.ContentRecommendationDTO;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;
import org.zerock.finance_dwpj1.service.portfolio.PortfolioMatchingService.MatchResult;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 콘텐츠 추천 서비스
 * 포트폴리오 분석 결과를 기반으로 사용자에게 맞춤형 콘텐츠를 추천합니다.
 *
 * <h2>추천 알고리즘</h2>
 * <ul>
 *   <li>해시태그 티커 매칭: 유저 보유 종목이 콘텐츠 해시태그에 포함되면 가산점</li>
 *   <li>투자대가 연관: TOP 투자대가 이름/스타일이 콘텐츠에 포함되면 강한 가산점</li>
 *   <li>종목 겹침: 투자대가 매칭 종목과 콘텐츠 해시태그 티커가 겹치면 가산점</li>
 *   <li>평점/최신성/인기도: 기본 품질 점수</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentRecommendationService {

    private final ContentReviewRepository contentReviewRepository;
    private final ContentReviewService contentReviewService;

    // 가중치 설정값 (application.properties)
    @Value("${recommendation.weight.tag-match:10}")
    private double weightTagMatch;

    @Value("${recommendation.weight.investor-boost-top1:30}")
    private double weightInvestorTop1;

    @Value("${recommendation.weight.investor-boost-top2:20}")
    private double weightInvestorTop2;

    @Value("${recommendation.weight.investor-boost-top3:10}")
    private double weightInvestorTop3;

    @Value("${recommendation.weight.overlap-ticker:15}")
    private double weightOverlapTicker;

    @Value("${recommendation.weight.rating:2}")
    private double weightRating;

    @Value("${recommendation.weight.recency-week:20}")
    private double weightRecencyWeek;

    @Value("${recommendation.weight.recency-month:10}")
    private double weightRecencyMonth;

    @Value("${recommendation.weight.view-month:0.1}")
    private double weightViewMonth;

    @Value("${recommendation.weight.view-total:0.01}")
    private double weightViewTotal;

    @Value("${recommendation.candidate-size:200}")
    private int candidateSize;

    @Value("${recommendation.result-size:5}")
    private int resultSize;

    @Value("${recommendation.debug-mode:false}")
    private boolean debugMode;

    /**
     * 포트폴리오 분석 결과 기반 콘텐츠 추천
     *
     * @param userHoldingsTickers 유저 보유 종목 티커 Set
     * @param topInvestors        TOP 3 투자대가 매칭 결과
     * @return 추천 콘텐츠 리스트 (최대 5개)
     */
    public List<ContentRecommendationDTO> recommendContents(
            Set<String> userHoldingsTickers,
            List<MatchResult> topInvestors) {

        log.info("===== 콘텐츠 추천 시작 =====");
        log.info("유저 보유 종목: {}", userHoldingsTickers);
        log.info("TOP 투자대가: {}", topInvestors.stream()
                .map(MatchResult::getInvestorName)
                .collect(Collectors.toList()));

        // 1) 추천 후보 조회 (최근 N개)
        List<ContentReview> candidates = contentReviewRepository
                .findRecentReviewsForRecommendation(PageRequest.of(0, candidateSize));

        log.info("추천 후보 개수: {}", candidates.size());

        if (candidates.isEmpty()) {
            log.warn("추천 후보가 없습니다. 빈 리스트 반환");
            return Collections.emptyList();
        }

        // 2) 각 콘텐츠별 점수 계산
        List<ScoredContent> scoredContents = candidates.stream()
                .map(content -> scoreContent(content, userHoldingsTickers, topInvestors))
                .filter(scored -> scored.score > 0) // 점수가 0보다 큰 것만
                .sorted((a, b) -> Double.compare(b.score, a.score)) // 점수 내림차순
                .limit(resultSize) // 상위 N개
                .collect(Collectors.toList());

        log.info("점수 계산 완료: {} → {} 개 추천", candidates.size(), scoredContents.size());

        // 3) DTO 변환
        List<ContentRecommendationDTO> recommendations = scoredContents.stream()
                .map(scored -> {
                    // 평점 계산
                    double rating = contentReviewService.getAverageRating(scored.content.getId());

                    ContentRecommendationDTO dto = ContentRecommendationDTO.fromEntity(
                            scored.content, rating);

                    // 디버그 모드인 경우 추천 사유 포함
                    if (debugMode) {
                        dto.setDebugInfo(
                                scored.score,
                                scored.matchedTags,
                                scored.matchedTickers,
                                scored.matchedInvestor
                        );
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        log.info("===== 콘텐츠 추천 완료: {} 개 =====", recommendations.size());

        return recommendations;
    }

    /**
     * 콘텐츠 점수 계산
     */
    private ScoredContent scoreContent(
            ContentReview content,
            Set<String> userHoldingsTickers,
            List<MatchResult> topInvestors) {

        double score = 0.0;
        List<String> matchedTags = new ArrayList<>();
        List<String> matchedTickers = new ArrayList<>();
        String matchedInvestor = null;

        // 해시태그에서 티커 추출
        Set<String> contentTickers = extractTickersFromHashtags(content.getHashtags());

        // 1️⃣ 해시태그 티커 매칭 점수
        Set<String> commonTickers = new HashSet<>(userHoldingsTickers);
        commonTickers.retainAll(contentTickers);
        double tagMatchScore = commonTickers.size() * weightTagMatch;
        score += tagMatchScore;

        if (!commonTickers.isEmpty()) {
            matchedTickers.addAll(commonTickers);
            if (debugMode) {
                log.debug("[{}] 티커 매칭: {} → +{}", content.getId(), commonTickers, tagMatchScore);
            }
        }

        // 2️⃣ 투자대가 연관 점수
        double investorBoostScore = 0.0;

        for (int i = 0; i < topInvestors.size(); i++) {
            MatchResult investor = topInvestors.get(i);
            double boost = (i == 0) ? weightInvestorTop1 :
                    (i == 1) ? weightInvestorTop2 : weightInvestorTop3;

            String hashtags = content.getHashtags() != null ? content.getHashtags() : "";
            String contentText = content.getContent() != null ? content.getContent() : "";

            // 투자대가 이름이 해시태그나 본문에 포함되면 가산
            if (hashtags.contains(investor.getInvestorName()) ||
                    contentText.contains(investor.getInvestorName())) {

                investorBoostScore += boost;
                matchedInvestor = investor.getInvestorName();

                if (debugMode) {
                    log.debug("[{}] 투자대가 매칭: {} → +{}", content.getId(),
                            investor.getInvestorName(), boost);
                }
            }

            // 투자 스타일이 해시태그에 포함되면 가산 (절반)
            // InvestorProfile에 investmentStyle이 있다고 가정
            // (현재 MatchResult에는 없으므로, 필요시 확장)
            // 임시로 주석 처리
            /*
            if (investor.getInvestmentStyle() != null &&
                hashtags.contains(investor.getInvestmentStyle())) {
                investorBoostScore += boost / 2;
            }
            */
        }

        score += investorBoostScore;

        // 3️⃣ 종목 겹침 추가 점수 (투자대가 매칭 종목과 콘텐츠 티커 겹침)
        double overlapTickerScore = 0.0;

        for (MatchResult investor : topInvestors) {
            List<String> investorStocks = investor.getMatchedStocks();
            Set<String> overlap = new HashSet<>(investorStocks);
            overlap.retainAll(contentTickers);

            overlapTickerScore += overlap.size() * weightOverlapTicker;

            if (!overlap.isEmpty() && debugMode) {
                log.debug("[{}] 투자대가({}) 종목 겹침: {} → +{}",
                        content.getId(), investor.getInvestorName(),
                        overlap, overlap.size() * weightOverlapTicker);
            }
        }

        score += overlapTickerScore;

        // 4️⃣ 평점 점수
        double ratingAvg = contentReviewService.getAverageRating(content.getId());
        double ratingScore = ratingAvg * weightRating;
        score += ratingScore;

        // 5️⃣ 최신성 점수
        long daysSinceCreated = ChronoUnit.DAYS.between(
                content.getCreatedDate(), LocalDateTime.now());

        double recencyScore = (daysSinceCreated <= 7) ? weightRecencyWeek :
                (daysSinceCreated <= 30) ? weightRecencyMonth : 0;
        score += recencyScore;

        // 6️⃣ 인기도 점수
        // viewCountMonth는 primitive int이므로 null 체크 불필요
        int viewMonth = content.getViewCountMonth();
        // viewCount는 Integer이므로 null 체크 필요
        int viewTotal = content.getViewCount() != null ? content.getViewCount() : 0;

        double popularityScore = (viewMonth * weightViewMonth) + (viewTotal * weightViewTotal);
        score += popularityScore;

        // 디버그 로그
        if (debugMode && score > 0) {
            log.debug("[{}] \"{}\" 최종 점수: {} (티커:{}, 투자대가:{}, 겹침:{}, 평점:{}, 최신성:{}, 인기:{})",
                    content.getId(), content.getTitle(), Math.round(score * 10) / 10.0,
                    Math.round(tagMatchScore), Math.round(investorBoostScore),
                    Math.round(overlapTickerScore), Math.round(ratingScore),
                    Math.round(recencyScore), Math.round(popularityScore));
        }

        return new ScoredContent(content, score, matchedTags, matchedTickers, matchedInvestor);
    }

    /**
     * 해시태그에서 티커 추출
     * "#TSLA", "#AAPL", "$NVDA" 같은 형식 파싱
     *
     * @param hashtags "#태그1 #태그2 #TSLA" 형식
     * @return {"TSLA", "AAPL"} 같은 티커 Set
     */
    private Set<String> extractTickersFromHashtags(String hashtags) {
        Set<String> tickers = new HashSet<>();

        if (hashtags == null || hashtags.isEmpty()) {
            return tickers;
        }

        // "#" 또는 "$" 기준으로 분리
        String[] tags = hashtags.split("[#$]");

        for (String tag : tags) {
            tag = tag.trim().toUpperCase();

            // 영어 대문자만 2-5자: 티커일 가능성 (AAPL, TSLA, NVDA)
            if (tag.matches("^[A-Z]{2,5}$")) {
                tickers.add(tag);
            }
        }

        return tickers;
    }

    /**
     * 점수가 매겨진 콘텐츠 (내부 클래스)
     */
    private static class ScoredContent {
        ContentReview content;
        double score;
        List<String> matchedTags;
        List<String> matchedTickers;
        String matchedInvestor;

        ScoredContent(ContentReview content, double score,
                      List<String> matchedTags, List<String> matchedTickers,
                      String matchedInvestor) {
            this.content = content;
            this.score = score;
            this.matchedTags = matchedTags;
            this.matchedTickers = matchedTickers;
            this.matchedInvestor = matchedInvestor;
        }
    }
}