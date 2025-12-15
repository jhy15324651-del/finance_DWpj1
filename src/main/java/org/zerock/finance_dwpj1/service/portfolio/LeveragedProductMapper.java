package org.zerock.finance_dwpj1.service.portfolio;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 레버리지/인버스 상품의 기초자산 매핑을 제공하는 컴포넌트
 *
 * <h2>목적</h2>
 * <p>레버리지/인버스 ETF를 기초자산과 factor로 매핑하여,
 * 포트폴리오 비교 시 실제 노출(exposure)을 계산할 수 있게 합니다.</p>
 *
 * <h2>주요 개념</h2>
 * <ul>
 *   <li><b>기초자산(Base Ticker)</b>: 레버리지 상품이 추종하는 원본 자산</li>
 *   <li><b>Factor</b>: 레버리지 배수
 *     <ul>
 *       <li>양수: 레버리지 (예: 2.0 = 2배 레버리지)</li>
 *       <li>음수: 인버스 (예: -3.0 = 3배 인버스)</li>
 *       <li>1.0: 일반 현물/ETF (레버리지 없음)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * LeveragedProductMapper mapper = new LeveragedProductMapper();
 *
 * // TSLL (TSLA 2배 레버리지)
 * ProductMapping mapping = mapper.getMapping("TSLL");
 * // mapping.getBaseTicker() = "TSLA"
 * // mapping.getFactor() = 2.0
 *
 * // 일반 종목 (매핑 없음)
 * ProductMapping appleMapping = mapper.getMapping("AAPL");
 * // appleMapping.getBaseTicker() = "AAPL" (자기 자신)
 * // appleMapping.getFactor() = 1.0
 * }</pre>
 *
 * <h2>확장 방법</h2>
 * <p>새로운 레버리지 상품을 추가하려면 {@link #initializeMappings()} 메소드에서
 * {@code addMapping()} 호출을 추가하면 됩니다.</p>
 *
 * <pre>{@code
 * @PostConstruct
 * public void initializeMappings() {
 *     // 기존 매핑들...
 *     addMapping("TSLL", "TSLA", 2.0);
 *
 *     // 신규 매핑 추가 (이 한 줄만 추가하면 됨)
 *     addMapping("NVDL", "NVDA", 2.0);
 * }
 * }</pre>
 *
 * <h2>향후 DB 기반 확장</h2>
 * <p>현재는 코드 기반 매핑이지만, 향후 DB 테이블로 전환 시에도
 * 다른 코드를 수정할 필요 없이 이 클래스의 {@code initializeMappings()}만
 * 변경하면 됩니다.</p>
 *
 * @author finance_dwpj1 team
 * @since 2025-12-15
 */
@Component
@Slf4j
public class LeveragedProductMapper {

    /**
     * 레버리지/인버스 상품 매핑 저장소
     * Key: 레버리지 상품 티커 (예: "TSLL")
     * Value: ProductMapping (기초자산 티커 + factor)
     */
    private final Map<String, ProductMapping> productMappings = new HashMap<>();

    /**
     * Spring Bean 초기화 시 매핑 테이블을 구성합니다.
     *
     * <p>현재 지원하는 레버리지/인버스 상품:</p>
     * <ul>
     *   <li>TSLL: TSLA 2배 레버리지</li>
     *   <li>TQQQ: QQQ(나스닥100) 3배 레버리지</li>
     *   <li>SQQQ: QQQ 3배 인버스</li>
     *   <li>SOXL: SOXX(반도체) 3배 레버리지</li>
     *   <li>SOXS: SOXX 3배 인버스</li>
     *   <li>SPXL: SPY(S&P500) 3배 레버리지</li>
     *   <li>SPXS: SPY 3배 인버스</li>
     *   <li>UPRO: SPY 3배 레버리지 (SPXL과 동일하지만 다른 발행사)</li>
     *   <li>UVXY: VIX(변동성 지수) 1.5배 레버리지</li>
     * </ul>
     *
     * <p><b>주의</b>: 새로운 상품 추가 시 여기에만 추가하면 됩니다.</p>
     */
    @PostConstruct
    public void initializeMappings() {
        log.info("=== 레버리지 상품 매핑 초기화 시작 ===");

        // TSLA 관련
        addMapping("TSLL", "TSLA", 2.0);  // Direxion Daily TSLA Bull 2X

        // 나스닥100 관련
        addMapping("TQQQ", "QQQ", 3.0);   // ProShares UltraPro QQQ (3x)
        addMapping("SQQQ", "QQQ", -3.0);  // ProShares UltraPro Short QQQ (-3x)
        addMapping("QLD", "QQQ", 2.0);    // ProShares Ultra QQQ (2x)

        // 반도체 관련
        addMapping("SOXL", "SOXX", 3.0);  // Direxion Daily Semiconductor Bull 3X
        addMapping("SOXS", "SOXX", -3.0); // Direxion Daily Semiconductor Bear 3X

        // S&P500 관련
        addMapping("SPXL", "SPY", 3.0);   // Direxion Daily S&P 500 Bull 3X
        addMapping("SPXS", "SPY", -3.0);  // Direxion Daily S&P 500 Bear 3X
        addMapping("UPRO", "SPY", 3.0);   // ProShares UltraPro S&P500 (3x)
        addMapping("SPXU", "SPY", -3.0);  // ProShares UltraPro Short S&P500 (-3x)
        addMapping("SSO", "SPY", 2.0);    // ProShares Ultra S&P500 (2x)
        addMapping("SDS", "SPY", -2.0);   // ProShares UltraShort S&P500 (-2x)

        // 변동성 관련
        addMapping("UVXY", "VIX", 1.5);   // ProShares Ultra VIX Short-Term Futures (1.5x)
        addMapping("SVXY", "VIX", -0.5);  // ProShares Short VIX Short-Term Futures (-0.5x)

        // 금 관련
        addMapping("UGLD", "GLD", 3.0);   // VelocityShares 3x Long Gold ETN
        addMapping("DGLD", "GLD", -3.0);  // VelocityShares 3x Inverse Gold ETN

        // 원유 관련
        addMapping("OILU", "USO", 2.0);   // ProShares UltraPro 3x Crude Oil ETF (2x)
        addMapping("OILD", "USO", -2.0);  // ProShares UltraPro 3x Short Crude Oil (-2x)

        log.info("레버리지 상품 매핑 완료: 총 {}개 상품", productMappings.size());
    }

    /**
     * 매핑을 추가합니다.
     *
     * @param leveragedTicker 레버리지 상품 티커 (예: "TSLL")
     * @param baseTicker 기초자산 티커 (예: "TSLA")
     * @param factor 레버리지 배수 (양수=레버리지, 음수=인버스)
     *
     * @throws IllegalArgumentException factor가 0인 경우
     */
    public void addMapping(String leveragedTicker, String baseTicker, double factor) {
        if (factor == 0.0) {
            throw new IllegalArgumentException(
                "Factor는 0이 될 수 없습니다. ticker=" + leveragedTicker
            );
        }

        ProductMapping mapping = new ProductMapping(baseTicker, factor);
        productMappings.put(leveragedTicker.toUpperCase(), mapping);

        log.debug("매핑 추가: {} → {} (factor={})", leveragedTicker, baseTicker, factor);
    }

    /**
     * 주어진 티커의 매핑 정보를 반환합니다.
     *
     * <p>매핑이 없는 경우(일반 현물/ETF):
     * <ul>
     *   <li>baseTicker = 원본 티커 (자기 자신)</li>
     *   <li>factor = 1.0</li>
     * </ul>
     * </p>
     *
     * <h3>사용 예시</h3>
     * <pre>{@code
     * // 레버리지 상품
     * ProductMapping tsll = mapper.getMapping("TSLL");
     * // tsll.getBaseTicker() = "TSLA"
     * // tsll.getFactor() = 2.0
     *
     * // 일반 종목 (매핑 없음)
     * ProductMapping aapl = mapper.getMapping("AAPL");
     * // aapl.getBaseTicker() = "AAPL"
     * // aapl.getFactor() = 1.0
     * }</pre>
     *
     * @param ticker 조회할 티커 (대소문자 무관)
     * @return 매핑 정보 (없으면 자기 자신 + factor=1.0)
     */
    public ProductMapping getMapping(String ticker) {
        String upperTicker = ticker.toUpperCase();

        // 매핑이 있으면 반환
        if (productMappings.containsKey(upperTicker)) {
            return productMappings.get(upperTicker);
        }

        // 매핑이 없으면 자기 자신 + factor=1.0
        return new ProductMapping(ticker, 1.0);
    }

    /**
     * 특정 티커가 레버리지/인버스 상품인지 확인합니다.
     *
     * @param ticker 조회할 티커
     * @return 매핑이 존재하면 true (레버리지 상품), 없으면 false (일반 종목)
     */
    public boolean isLeveragedProduct(String ticker) {
        return productMappings.containsKey(ticker.toUpperCase());
    }

    /**
     * 현재 등록된 모든 매핑을 반환합니다.
     *
     * @return 레버리지 상품 매핑 맵 (불변 복사본)
     */
    public Map<String, ProductMapping> getAllMappings() {
        return new HashMap<>(productMappings);
    }

    /**
     * 레버리지/인버스 상품의 매핑 정보
     *
     * <p>불변(immutable) 객체입니다.</p>
     */
    @Getter
    public static class ProductMapping {
        /**
         * 기초자산 티커
         * 예: TSLL의 경우 "TSLA"
         */
        private final String baseTicker;

        /**
         * 레버리지 배수
         * <ul>
         *   <li>양수: 레버리지 (예: 2.0 = 2배 레버리지)</li>
         *   <li>음수: 인버스 (예: -3.0 = 3배 인버스)</li>
         *   <li>1.0: 일반 현물/ETF</li>
         * </ul>
         */
        private final double factor;

        /**
         * 매핑 정보를 생성합니다.
         *
         * @param baseTicker 기초자산 티커
         * @param factor 레버리지 배수
         */
        public ProductMapping(String baseTicker, double factor) {
            this.baseTicker = baseTicker;
            this.factor = factor;
        }

        @Override
        public String toString() {
            return String.format("ProductMapping{baseTicker='%s', factor=%.1f}", baseTicker, factor);
        }
    }
}