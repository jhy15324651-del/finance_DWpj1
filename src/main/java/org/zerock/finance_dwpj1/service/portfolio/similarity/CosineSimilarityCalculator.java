package org.zerock.finance_dwpj1.service.portfolio.similarity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 코사인 유사도(Cosine Similarity)를 계산하는 구현체
 *
 * <h2>코사인 유사도란?</h2>
 * <p>두 벡터 사이의 <b>각도</b>를 측정하여 방향의 유사성을 판단하는 지표입니다.</p>
 *
 * <h3>수식</h3>
 * <pre>
 * cos(θ) = (A · B) / (||A|| × ||B||)
 *
 * A · B = 내적 (dot product) = Σ(A_i × B_i)
 * ||A|| = A의 크기 (magnitude) = sqrt(Σ(A_i²))
 * ||B|| = B의 크기 (magnitude) = sqrt(Σ(B_i²))
 * </pre>
 *
 * <h3>값의 범위</h3>
 * <ul>
 *   <li><b>1.0 (100%)</b>: 완전히 같은 방향 → 동일한 비중 분포</li>
 *   <li><b>0.5 (50%)</b>: 60도 각도 → 부분적으로 유사</li>
 *   <li><b>0.0 (0%)</b>: 90도 직교 → 겹치는 종목 없음</li>
 *   <li><b>-1.0 (-100%)</b>: 정반대 방향 (long-only에서는 발생 안 함)</li>
 * </ul>
 *
 * <h2>왜 코사인 유사도가 포트폴리오 비교에 적합한가?</h2>
 *
 * <h3>1. 크기에 무관 (Scale Invariant)</h3>
 * <p>비중의 절대값이 아닌 <b>상대적 분포</b>를 비교합니다.</p>
 * <pre>{@code
 * 포트폴리오 A: { AAPL: 0.3, MSFT: 0.7 }
 * 포트폴리오 B: { AAPL: 30, MSFT: 70 }
 *
 * 코사인 유사도 = 100% (비율이 같으므로)
 * 유클리드 거리 = 큼 (크기가 다르므로 부적합)
 * }</pre>
 *
 * <h3>2. 겹치는 종목에 집중</h3>
 * <p>공통 종목의 비중이 비슷할수록 점수가 높아집니다.</p>
 * <pre>{@code
 * 사용자: { AAPL: 40%, MSFT: 30%, GOOGL: 30% }
 * 투자대가: { AAPL: 45%, MSFT: 25%, GOOGL: 30% }
 *
 * → 높은 유사도 (비중 패턴이 유사)
 * }</pre>
 *
 * <h3>3. 희소 벡터(Sparse Vector)에 강함</h3>
 * <p>13F는 수백 종목, 사용자는 10종목이어도 정확히 비교합니다.</p>
 * <pre>{@code
 * 사용자: { AAPL: 50%, MSFT: 50% }
 * 투자대가: { AAPL: 10%, MSFT: 10%, ... 100개 더 }
 *
 * → 겹치는 AAPL, MSFT의 비중 패턴만 비교
 * }</pre>
 *
 * <h3>4. 양수 포트폴리오에 최적화</h3>
 * <p>long-only 포지션(비중 ≥ 0)에서 직관적인 결과를 제공합니다.</p>
 *
 * <h2>계산 예시</h2>
 * <pre>{@code
 * 포트폴리오 1: { AAPL: 0.5, MSFT: 0.5 }
 * 포트폴리오 2: { AAPL: 0.6, MSFT: 0.4 }
 *
 * 1. 내적 (dot product)
 *    = (0.5 × 0.6) + (0.5 × 0.4)
 *    = 0.3 + 0.2
 *    = 0.5
 *
 * 2. 크기 (magnitude)
 *    ||A|| = sqrt(0.5² + 0.5²) = sqrt(0.5) = 0.707
 *    ||B|| = sqrt(0.6² + 0.4²) = sqrt(0.52) = 0.721
 *
 * 3. 코사인 유사도
 *    cos(θ) = 0.5 / (0.707 × 0.721) = 0.98
 *
 * 결과: 98% 유사도
 * }</pre>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>입력 포트폴리오는 정규화되어 있어야 정확함 (합=1 권장)</li>
 *   <li>음수 비중이 있으면 해석이 복잡해짐 (long/short 혼합)</li>
 *   <li>모든 비중이 0이면 유사도 0 반환</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @Autowired
 * private CosineSimilarityCalculator calculator;
 *
 * Map<String, Double> user = Map.of("AAPL", 0.4, "MSFT", 0.6);
 * Map<String, Double> guru = Map.of("AAPL", 0.5, "MSFT", 0.5);
 *
 * double similarity = calculator.calculate(user, guru);
 * // 결과: ~99.8%
 * }</pre>
 *
 * @author finance_dwpj1 team
 * @since 2025-12-15
 */
@Component
@Slf4j
public class CosineSimilarityCalculator implements SimilarityCalculator {

    /**
     * 코사인 유사도를 계산합니다.
     *
     * <p>알고리즘:</p>
     * <ol>
     *   <li>두 포트폴리오의 합집합 종목 목록 생성</li>
     *   <li>내적(dot product) 계산: Σ(weight1 × weight2)</li>
     *   <li>각 포트폴리오의 크기(magnitude) 계산: sqrt(Σ(weight²))</li>
     *   <li>코사인 = 내적 / (크기1 × 크기2)</li>
     *   <li>결과를 퍼센트(0~100)로 변환</li>
     * </ol>
     *
     * <p><b>처리 예시</b>:</p>
     * <pre>{@code
     * portfolio1 = { "AAPL": 0.3, "MSFT": 0.7 }
     * portfolio2 = { "AAPL": 0.4, "MSFT": 0.6 }
     *
     * 합집합: [AAPL, MSFT]
     *
     * 내적:
     *   AAPL: 0.3 × 0.4 = 0.12
     *   MSFT: 0.7 × 0.6 = 0.42
     *   dotProduct = 0.54
     *
     * 크기:
     *   magnitude1 = sqrt(0.3² + 0.7²) = sqrt(0.58) = 0.762
     *   magnitude2 = sqrt(0.4² + 0.6²) = sqrt(0.52) = 0.721
     *
     * 코사인:
     *   cos = 0.54 / (0.762 × 0.721) = 0.983
     *
     * 결과: 98.3%
     * }</pre>
     *
     * @param portfolio1 첫 번째 포트폴리오 (정규화 권장)
     * @param portfolio2 두 번째 포트폴리오 (정규화 권장)
     * @return 코사인 유사도 (0~100%)
     *
     * @throws IllegalArgumentException portfolio가 null이거나 비어있는 경우
     */
    @Override
    public double calculate(Map<String, Double> portfolio1, Map<String, Double> portfolio2) {
        // 입력 검증
        if (portfolio1 == null || portfolio1.isEmpty()) {
            throw new IllegalArgumentException("portfolio1이 null이거나 비어있습니다");
        }
        if (portfolio2 == null || portfolio2.isEmpty()) {
            throw new IllegalArgumentException("portfolio2이 null이거나 비어있습니다");
        }

        log.debug("=== 코사인 유사도 계산 시작 ===");
        log.debug("Portfolio 1 크기: {}", portfolio1.size());
        log.debug("Portfolio 2 크기: {}", portfolio2.size());

        // 1. 모든 종목의 합집합 생성
        Set<String> allTickers = new HashSet<>();
        allTickers.addAll(portfolio1.keySet());
        allTickers.addAll(portfolio2.keySet());

        log.debug("전체 종목 수 (합집합): {}", allTickers.size());

        // 2. 내적(dot product)과 크기(magnitude) 계산
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (String ticker : allTickers) {
            double weight1 = portfolio1.getOrDefault(ticker, 0.0);
            double weight2 = portfolio2.getOrDefault(ticker, 0.0);

            dotProduct += weight1 * weight2;
            magnitude1 += weight1 * weight1;
            magnitude2 += weight2 * weight2;

            // 공통 종목 로깅
            if (weight1 > 0 && weight2 > 0) {
                log.debug("공통 종목 {}: p1={}, p2={}, contribution={}",
                    ticker, weight1, weight2, weight1 * weight2);
            }
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        log.debug("내적 (dot product): {}", dotProduct);
        log.debug("크기 1 (magnitude1): {}", magnitude1);
        log.debug("크기 2 (magnitude2): {}", magnitude2);

        // 3. 0으로 나누기 방지
        if (magnitude1 == 0 || magnitude2 == 0) {
            log.warn("포트폴리오 크기가 0입니다. 유사도 0 반환");
            return 0.0;
        }

        // 4. 코사인 유사도 계산 (0~1)
        double cosineSimilarity = dotProduct / (magnitude1 * magnitude2);

        log.debug("코사인 유사도 (0~1): {}", cosineSimilarity);

        // 5. 퍼센트로 변환 (0~100)
        double percentage = cosineSimilarity * 100.0;

        log.debug("코사인 유사도 (퍼센트): {}%", percentage);
        log.debug("=== 코사인 유사도 계산 완료 ===");

        return percentage;
    }

    /**
     * 알고리즘 이름을 반환합니다.
     *
     * @return "Cosine Similarity"
     */
    @Override
    public String getName() {
        return "Cosine Similarity";
    }
}