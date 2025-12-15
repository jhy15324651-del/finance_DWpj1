package org.zerock.finance_dwpj1.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.finance_dwpj1.service.portfolio.PortfolioExposureNormalizer.NormalizationMode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PortfolioExposureNormalizer 단위 테스트
 */
class PortfolioExposureNormalizerTest {

    private PortfolioExposureNormalizer normalizer;
    private LeveragedProductMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LeveragedProductMapper();
        mapper.initializeMappings();

        normalizer = new PortfolioExposureNormalizer(mapper);
        normalizer.setNormalizationMode(NormalizationMode.LONG_ONLY); // 기본 모드
    }

    @Test
    @DisplayName("TSLL 5% → TSLA 10% 노출로 환산")
    void testTsllExposureConversion() {
        // Given
        Map<String, Double> portfolio = Map.of("TSLL", 5.0);

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertNotNull(normalized);
        assertTrue(normalized.containsKey("TSLA"), "TSLL이 TSLA로 변환됨");
        assertFalse(normalized.containsKey("TSLL"), "TSLL은 사라짐");

        // TSLL 5% × 2.0 = 10% → 정규화 후 100%
        assertEquals(1.0, normalized.get("TSLA"), 0.001, "단일 종목이므로 100%");
    }

    @Test
    @DisplayName("동일 base로 합산 테스트 - TSLL + TSLA")
    void testSameBaseMerging() {
        // Given: TSLL 5% + TSLA 10%
        Map<String, Double> portfolio = Map.of(
            "TSLL", 5.0,   // TSLA 10% 노출
            "TSLA", 10.0   // TSLA 10% 노출
        );

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertTrue(normalized.containsKey("TSLA"));
        assertEquals(1, normalized.size(), "TSLA로 합산되어 1개 종목");

        // 총 TSLA 노출 = 10% + 10% = 20% → 정규화 후 100%
        assertEquals(1.0, normalized.get("TSLA"), 0.001);
    }

    @Test
    @DisplayName("복합 포트폴리오 환산 + 정규화")
    void testComplexPortfolio() {
        // Given
        Map<String, Double> portfolio = Map.of(
            "TSLL", 5.0,    // TSLA 10% 노출
            "TQQQ", 10.0,   // QQQ 30% 노출
            "AAPL", 20.0    // AAPL 20% 노출
        );
        // 총 노출 = TSLA 10% + QQQ 30% + AAPL 20% = 60%

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertEquals(3, normalized.size(), "3개 종목");
        assertTrue(normalized.containsKey("TSLA"));
        assertTrue(normalized.containsKey("QQQ"));
        assertTrue(normalized.containsKey("AAPL"));

        // 정규화 후 합 = 1.0 (100%)
        double sum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001, "정규화 후 합은 1.0");

        // 비율 검증
        assertEquals(10.0 / 60.0, normalized.get("TSLA"), 0.001, "TSLA 16.67%");
        assertEquals(30.0 / 60.0, normalized.get("QQQ"), 0.001, "QQQ 50%");
        assertEquals(20.0 / 60.0, normalized.get("AAPL"), 0.001, "AAPL 33.33%");
    }

    @Test
    @DisplayName("일반 종목만 있을 때 정규화")
    void testNormalStocksOnly() {
        // Given
        Map<String, Double> portfolio = Map.of(
            "AAPL", 40.0,
            "MSFT", 60.0
        );
        // 총합 = 100%

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertEquals(2, normalized.size());

        double sum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001, "정규화 후 합은 1.0");

        assertEquals(0.4, normalized.get("AAPL"), 0.001, "40%");
        assertEquals(0.6, normalized.get("MSFT"), 0.001, "60%");
    }

    @Test
    @DisplayName("LONG_ONLY 모드: 인버스(음수) 제거")
    void testLongOnlyModeRemovesNegativeExposure() {
        // Given
        normalizer.setNormalizationMode(NormalizationMode.LONG_ONLY);

        Map<String, Double> portfolio = Map.of(
            "SQQQ", 10.0,   // QQQ -30% 노출 (인버스)
            "AAPL", 40.0,   // AAPL 40% 노출
            "MSFT", 20.0    // MSFT 20% 노출
        );

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertFalse(normalized.containsKey("QQQ"), "음수 노출(QQQ)는 제거됨");
        assertTrue(normalized.containsKey("AAPL"));
        assertTrue(normalized.containsKey("MSFT"));

        // 양수만 정규화: AAPL 40 + MSFT 20 = 60
        double sum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001);

        assertEquals(40.0 / 60.0, normalized.get("AAPL"), 0.001, "AAPL 66.67%");
        assertEquals(20.0 / 60.0, normalized.get("MSFT"), 0.001, "MSFT 33.33%");
    }

    @Test
    @DisplayName("LONG_SHORT 모드: 인버스(음수) 유지")
    void testLongShortModeKeepsNegativeExposure() {
        // Given
        normalizer.setNormalizationMode(NormalizationMode.LONG_SHORT);

        Map<String, Double> portfolio = Map.of(
            "SQQQ", 10.0,   // QQQ -30% 노출
            "AAPL", 40.0,   // AAPL 40% 노출
            "MSFT", 20.0    // MSFT 20% 노출
        );
        // QQQ -30 + AAPL 40 + MSFT 20 = 30
        // L1 norm = |-30| + |40| + |20| = 90

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertTrue(normalized.containsKey("QQQ"), "음수 노출 유지");
        assertTrue(normalized.get("QQQ") < 0, "QQQ는 음수");

        // L1 정규화
        double l1Sum = normalized.values().stream().mapToDouble(Math::abs).sum();
        assertEquals(1.0, l1Sum, 0.001, "L1 norm = 1.0");

        assertEquals(-30.0 / 90.0, normalized.get("QQQ"), 0.001, "QQQ -33.33%");
        assertEquals(40.0 / 90.0, normalized.get("AAPL"), 0.001, "AAPL 44.44%");
        assertEquals(20.0 / 90.0, normalized.get("MSFT"), 0.001, "MSFT 22.22%");
    }

    @Test
    @DisplayName("빈 포트폴리오 처리")
    void testEmptyPortfolio() {
        // Given
        Map<String, Double> portfolio = new HashMap<>();

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertNotNull(normalized);
        assertTrue(normalized.isEmpty(), "빈 포트폴리오는 빈 결과");
    }

    @Test
    @DisplayName("모든 노출이 음수인 경우 (LONG_ONLY)")
    void testAllNegativeExposureLongOnly() {
        // Given
        normalizer.setNormalizationMode(NormalizationMode.LONG_ONLY);

        Map<String, Double> portfolio = Map.of(
            "SQQQ", 50.0,  // QQQ -150% 노출
            "SPXS", 20.0   // SPY -60% 노출
        );

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertTrue(normalized.isEmpty(), "모든 노출이 음수면 빈 포트폴리오");
    }

    @Test
    @DisplayName("정규화 전후 총합 검증")
    void testNormalizationSum() {
        // Given
        Map<String, Double> portfolio = Map.of(
            "TQQQ", 15.0,   // QQQ 45% 노출
            "AAPL", 35.0,
            "MSFT", 25.0
        );
        // 총 노출 = 45 + 35 + 25 = 105%

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        double sum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001, "정규화 후 합은 정확히 1.0");
    }

    @Test
    @DisplayName("레버리지 상품 여러 개 환산 테스트")
    void testMultipleLeveragedProducts() {
        // Given
        Map<String, Double> portfolio = Map.of(
            "TSLL", 10.0,   // TSLA 20% 노출
            "TQQQ", 5.0,    // QQQ 15% 노출
            "SOXL", 3.0     // SOXX 9% 노출
        );
        // 총 노출 = 20 + 15 + 9 = 44%

        // When
        Map<String, Double> normalized = normalizer.normalize(portfolio);

        // Then
        assertEquals(3, normalized.size());
        assertTrue(normalized.containsKey("TSLA"));
        assertTrue(normalized.containsKey("QQQ"));
        assertTrue(normalized.containsKey("SOXX"));

        double sum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001);

        assertEquals(20.0 / 44.0, normalized.get("TSLA"), 0.001);
        assertEquals(15.0 / 44.0, normalized.get("QQQ"), 0.001);
        assertEquals(9.0 / 44.0, normalized.get("SOXX"), 0.001);
    }
}