package org.zerock.finance_dwpj1.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.finance_dwpj1.service.portfolio.LeveragedProductMapper.ProductMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오를 노출(Exposure) 기준으로 환산하고 정규화하는 컴포넌트
 *
 * <h2>핵심 개념: 노출(Exposure)이란?</h2>
 * <p><b>노출</b>은 특정 기초자산의 가격 변동에 대한 포트폴리오의 민감도를 의미합니다.</p>
 *
 * <p>예시:</p>
 * <ul>
 *   <li>TSLA 현물 10% 보유 → TSLA 10% 노출</li>
 *   <li>TSLL(TSLA 2배 레버리지) 10% 보유 → <b>TSLA 20% 노출</b></li>
 *   <li>TQQQ(QQQ 3배 레버리지) 15% 보유 → <b>QQQ 45% 노출</b></li>
 * </ul>
 *
 * <h2>왜 필요한가?</h2>
 * <p>기존 방식은 티커를 있는 그대로 비교하여 왜곡이 발생합니다:</p>
 * <pre>
 * 사용자: { "TSLL": 10% }  // TSLA 2배 레버리지
 * 투자대가: { "TSLA": 20% }
 *
 * 기존 방식:
 * - 겹치는 종목 = 0개 (TSLL ≠ TSLA)
 * - 코사인 유사도 = 0% (완전히 다른 포트폴리오로 인식)
 *
 * 노출 환산 후:
 * - 사용자: { "TSLA": 20% }  (TSLL 10% × 2.0)
 * - 투자대가: { "TSLA": 20% }
 * - 코사인 유사도 = 100% (동일한 노출)
 * </pre>
 *
 * <h2>처리 과정</h2>
 * <ol>
 *   <li><b>노출 환산</b>: 레버리지 상품을 기초자산 노출로 변환
 *     <ul>
 *       <li>TSLL 5% → TSLA 10% (5% × 2.0)</li>
 *       <li>TQQQ 10% → QQQ 30% (10% × 3.0)</li>
 *       <li>SQQQ 10% → QQQ -30% (10% × -3.0) - 인버스</li>
 *     </ul>
 *   </li>
 *   <li><b>동일 기초자산 합산</b>: 같은 baseTicker는 노출 합산</li>
 *   <li><b>재정규화</b>: 합이 1(100%)이 되도록 조정
 *     <ul>
 *       <li>LONG_ONLY 모드: 음수 제거 후 양수만 정규화</li>
 *       <li>LONG_SHORT 모드: 음수 유지하고 L1/L2 norm으로 정규화</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>정규화 모드</h2>
 * <ul>
 *   <li><b>LONG_ONLY</b> (기본 권장):
 *     <ul>
 *       <li>음수 노출(인버스)을 0으로 클리핑</li>
 *       <li>13F 보고서(long-only)와 직접 비교 가능</li>
 *       <li>대부분의 투자대가는 long 중심이므로 적합</li>
 *     </ul>
 *   </li>
 *   <li><b>LONG_SHORT</b> (고급):
 *     <ul>
 *       <li>음수 노출 유지 (헤지 전략 반영)</li>
 *       <li>L1 norm (절대값 합) 기준 정규화</li>
 *       <li>마켓 뉴트럴 전략 사용자에게 적합</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * @Autowired
 * private PortfolioExposureNormalizer normalizer;
 *
 * // 원본 포트폴리오
 * Map<String, Double> portfolio = Map.of(
 *     "TSLL", 5.0,   // TSLA 2배 레버리지
 *     "TQQQ", 10.0,  // QQQ 3배 레버리지
 *     "AAPL", 20.0   // 일반 종목
 * );
 *
 * // 노출 기준 환산 + 정규화
 * Map<String, Double> normalized = normalizer.normalize(portfolio);
 * // 결과: { "TSLA": 16.67%, "QQQ": 50%, "AAPL": 33.33% }
 * // (원본 총합 60% → 정규화 후 100%)
 * }</pre>
 *
 * @author finance_dwpj1 team
 * @since 2025-12-15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioExposureNormalizer {

    private final LeveragedProductMapper leveragedProductMapper;

    /**
     * 정규화 모드
     */
    private NormalizationMode mode = NormalizationMode.LONG_ONLY;

    /**
     * 정규화 모드를 설정합니다.
     *
     * @param mode 정규화 모드
     */
    public void setNormalizationMode(NormalizationMode mode) {
        this.mode = mode;
        log.info("정규화 모드 변경: {}", mode);
    }

    /**
     * 포트폴리오를 노출 기준으로 환산하고 정규화합니다.
     *
     * <p>전체 프로세스:</p>
     * <ol>
     *   <li>레버리지 상품 → 기초자산 노출 환산</li>
     *   <li>동일 기초자산 노출 합산</li>
     *   <li>재정규화 (합=1)</li>
     * </ol>
     *
     * <h3>입력 예시</h3>
     * <pre>{@code
     * Map<String, Double> input = Map.of(
     *     "TSLL", 5.0,   // TSLA 2배 레버리지
     *     "TQQQ", 10.0,  // QQQ 3배 레버리지
     *     "AAPL", 20.0
     * );
     * }</pre>
     *
     * <h3>처리 과정</h3>
     * <pre>
     * 1단계: 노출 환산
     *   TSLL 5% → TSLA 10% (5 × 2.0)
     *   TQQQ 10% → QQQ 30% (10 × 3.0)
     *   AAPL 20% → AAPL 20% (20 × 1.0)
     *   환산 결과: { TSLA: 10%, QQQ: 30%, AAPL: 20% }
     *   총합 = 60%
     *
     * 2단계: 정규화 (LONG_ONLY 모드)
     *   TSLA: 10/60 = 16.67%
     *   QQQ: 30/60 = 50%
     *   AAPL: 20/60 = 33.33%
     *   총합 = 100% ✓
     * </pre>
     *
     * @param portfolio 원본 포트폴리오 (ticker → weight, weight는 % 단위 또는 비율)
     * @return 노출 기준으로 환산 및 정규화된 포트폴리오 (합=1 또는 100% 기준)
     *
     * @throws IllegalArgumentException portfolio가 null이거나 비어있는 경우
     */
    public Map<String, Double> normalize(Map<String, Double> portfolio) {
        if (portfolio == null || portfolio.isEmpty()) {
            log.warn("빈 포트폴리오 입력됨");
            return new HashMap<>();
        }

        log.info("=== 포트폴리오 노출 환산 시작 ===");
        log.info("원본 포트폴리오: {}", portfolio);

        // 1단계: 노출 환산
        Map<String, Double> exposure = convertToExposure(portfolio);
        log.info("환산 후 노출: {}", exposure);

        double sumBeforeNorm = exposure.values().stream()
            .mapToDouble(Math::abs)
            .sum();
        log.info("정규화 전 총합(절대값): {}", sumBeforeNorm);

        // 2단계: 정규화
        Map<String, Double> normalized = normalizeWeights(exposure);
        log.info("정규화 후: {}", normalized);

        double sumAfterNorm = normalized.values().stream()
            .mapToDouble(Math::abs)
            .sum();
        log.info("정규화 후 총합(절대값): {}", sumAfterNorm);

        log.info("=== 포트폴리오 노출 환산 완료 ===");

        return normalized;
    }

    /**
     * 레버리지 상품을 기초자산 노출로 환산합니다.
     *
     * <p>변환 수식:</p>
     * <pre>
     * exposure[baseTicker] += weight × factor
     * </pre>
     *
     * <h3>처리 예시</h3>
     * <pre>{@code
     * 입력: { "TSLL": 5.0, "TSLA": 10.0 }
     *
     * 처리 과정:
     * 1. TSLL 5.0 처리
     *    - mapping = (TSLA, 2.0)
     *    - exposure["TSLA"] += 5.0 × 2.0 = 10.0
     *
     * 2. TSLA 10.0 처리
     *    - mapping = (TSLA, 1.0) // 자기 자신
     *    - exposure["TSLA"] += 10.0 × 1.0 = 10.0
     *
     * 결과: { "TSLA": 20.0 } // 합산됨
     * }</pre>
     *
     * <h3>주의사항</h3>
     * <ul>
     *   <li>동일한 baseTicker는 노출이 합산됩니다</li>
     *   <li>인버스 상품(factor < 0)은 음수 노출을 생성합니다</li>
     *   <li>로그에 모든 변환 내역이 기록됩니다</li>
     * </ul>
     *
     * @param portfolio 원본 포트폴리오
     * @return 기초자산별 노출 맵
     */
    private Map<String, Double> convertToExposure(Map<String, Double> portfolio) {
        Map<String, Double> exposure = new HashMap<>();

        for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
            String ticker = entry.getKey();
            double weight = entry.getValue();

            // 매핑 조회 (없으면 자기 자신 + factor=1.0)
            ProductMapping mapping = leveragedProductMapper.getMapping(ticker);
            String baseTicker = mapping.getBaseTicker();
            double factor = mapping.getFactor();

            // 노출 계산
            double exposureValue = weight * factor;

            // 동일 baseTicker 합산
            exposure.merge(baseTicker, exposureValue, Double::sum);

            // 로깅
            if (leveragedProductMapper.isLeveragedProduct(ticker)) {
                log.info("레버리지 환산: {} {}% → {} {}% (factor={})",
                    ticker, weight, baseTicker, exposureValue, factor);
            } else {
                log.debug("일반 종목: {} {}%", ticker, weight);
            }
        }

        return exposure;
    }

    /**
     * 노출을 정규화합니다 (합=1 또는 100%).
     *
     * <p>정규화가 필요한 이유:</p>
     * <ul>
     *   <li>레버리지 환산 후 총합이 1을 초과하거나 미만일 수 있음</li>
     *   <li>코사인 유사도는 벡터 크기에 영향받으므로 정규화 필요</li>
     *   <li>정규화 = "비중 분포 패턴"만 순수하게 비교</li>
     * </ul>
     *
     * <h3>모드별 처리</h3>
     *
     * <h4>LONG_ONLY 모드 (권장)</h4>
     * <pre>{@code
     * 입력: { "QQQ": -30, "AAPL": 40, "MSFT": 20 }
     *
     * 1. 음수 제거 (클리핑)
     *    { "AAPL": 40, "MSFT": 20 }
     *
     * 2. 양수 합 계산
     *    sum = 40 + 20 = 60
     *
     * 3. 정규화
     *    AAPL: 40/60 = 0.667 (66.7%)
     *    MSFT: 20/60 = 0.333 (33.3%)
     *
     * 결과: { "AAPL": 66.7, "MSFT": 33.3 }
     * }</pre>
     *
     * <h4>LONG_SHORT 모드 (고급)</h4>
     * <pre>{@code
     * 입력: { "QQQ": -30, "AAPL": 40, "MSFT": 20 }
     *
     * 1. L1 norm 계산
     *    L1 = |-30| + |40| + |20| = 90
     *
     * 2. 정규화 (음수 유지)
     *    QQQ: -30/90 = -0.333
     *    AAPL: 40/90 = 0.444
     *    MSFT: 20/90 = 0.222
     *
     * 결과: { "QQQ": -33.3, "AAPL": 44.4, "MSFT": 22.2 }
     * }</pre>
     *
     * @param exposure 환산된 노출 맵
     * @return 정규화된 포트폴리오 (합=1 기준)
     */
    private Map<String, Double> normalizeWeights(Map<String, Double> exposure) {
        if (mode == NormalizationMode.LONG_ONLY) {
            return normalizeLongOnly(exposure);
        } else {
            return normalizeLongShort(exposure);
        }
    }

    /**
     * LONG_ONLY 모드 정규화
     *
     * <p>처리 방식:</p>
     * <ol>
     *   <li>음수 노출 제거 (인버스 포지션 무시)</li>
     *   <li>남은 양수 노출만으로 합=1 정규화</li>
     * </ol>
     *
     * <p>이유:</p>
     * <ul>
     *   <li>13F 보고서는 long-only 포지션만 보고</li>
     *   <li>투자대가와 직접 비교 가능</li>
     *   <li>인버스 베팅은 "다른 투자 성향"으로 간주</li>
     * </ul>
     *
     * @param exposure 환산된 노출
     * @return 정규화된 포트폴리오 (long-only)
     */
    private Map<String, Double> normalizeLongOnly(Map<String, Double> exposure) {
        // 1. 양수만 필터링
        Map<String, Double> positiveOnly = exposure.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (positiveOnly.isEmpty()) {
            log.warn("모든 노출이 음수입니다. 빈 포트폴리오 반환");
            return new HashMap<>();
        }

        // 음수가 제거된 경우 로깅
        if (positiveOnly.size() < exposure.size()) {
            log.info("음수 노출 제거됨 (LONG_ONLY 모드): {} → {}",
                exposure.size(), positiveOnly.size());

            exposure.entrySet().stream()
                .filter(e -> e.getValue() <= 0)
                .forEach(e -> log.info("  제거: {} = {}", e.getKey(), e.getValue()));
        }

        // 2. 양수 합 계산
        double sum = positiveOnly.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        if (sum == 0) {
            log.warn("양수 노출 합이 0입니다");
            return new HashMap<>();
        }

        // 3. 정규화
        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : positiveOnly.entrySet()) {
            double normalizedWeight = entry.getValue() / sum;
            normalized.put(entry.getKey(), normalizedWeight);
        }

        return normalized;
    }

    /**
     * LONG_SHORT 모드 정규화
     *
     * <p>처리 방식:</p>
     * <ol>
     *   <li>음수 노출 유지 (인버스 포지션 반영)</li>
     *   <li>L1 norm (절대값 합) 기준 정규화</li>
     * </ol>
     *
     * <p>이유:</p>
     * <ul>
     *   <li>헤지 전략, 마켓 뉴트럴 전략 반영</li>
     *   <li>정보 손실 없음</li>
     *   <li>고급 사용자 대상</li>
     * </ul>
     *
     * <p><b>주의</b>: 코사인 유사도에서 음수의 의미</p>
     * <ul>
     *   <li>양수 × 양수 = 양수 → 유사도 증가</li>
     *   <li>음수 × 음수 = 양수 → 둘 다 숏이면 유사도 증가</li>
     *   <li>양수 × 음수 = 음수 → 유사도 감소</li>
     * </ul>
     *
     * @param exposure 환산된 노출
     * @return 정규화된 포트폴리오 (long/short 혼합)
     */
    private Map<String, Double> normalizeLongShort(Map<String, Double> exposure) {
        // L1 norm 계산 (절대값 합)
        double l1Norm = exposure.values().stream()
            .mapToDouble(Math::abs)
            .sum();

        if (l1Norm == 0) {
            log.warn("L1 norm이 0입니다");
            return new HashMap<>();
        }

        log.info("L1 norm = {}", l1Norm);

        // 정규화 (음수 유지)
        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : exposure.entrySet()) {
            double normalizedWeight = entry.getValue() / l1Norm;
            normalized.put(entry.getKey(), normalizedWeight);
        }

        return normalized;
    }

    /**
     * 정규화 모드
     */
    public enum NormalizationMode {
        /**
         * Long-only 모드 (기본 권장)
         * - 음수 노출 제거
         * - 13F 보고서와 직접 비교 가능
         */
        LONG_ONLY,

        /**
         * Long/Short 모드 (고급)
         * - 음수 노출 유지
         * - 헤지 전략 반영
         */
        LONG_SHORT
    }
}