package org.zerock.finance_dwpj1.service.portfolio.similarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CosineSimilarityCalculator 단위 테스트
 */
class CosineSimilarityCalculatorTest {

    private CosineSimilarityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CosineSimilarityCalculator();
    }

    @Test
    @DisplayName("완전히 동일한 포트폴리오 - 유사도 100%")
    void testIdenticalPortfolios() {
        // Given
        Map<String, Double> portfolio1 = Map.of("AAPL", 0.5, "MSFT", 0.5);
        Map<String, Double> portfolio2 = Map.of("AAPL", 0.5, "MSFT", 0.5);

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertEquals(100.0, similarity, 0.1, "완전히 동일하면 100%");
    }

    @Test
    @DisplayName("크기가 다르지만 비율이 같으면 100% - Scale Invariant")
    void testScaleInvariance() {
        // Given
        Map<String, Double> portfolio1 = Map.of("AAPL", 0.3, "MSFT", 0.7);
        Map<String, Double> portfolio2 = Map.of("AAPL", 30.0, "MSFT", 70.0);

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertEquals(100.0, similarity, 0.1, "비율이 같으면 크기 무관하게 100%");
    }

    @Test
    @DisplayName("겹치는 종목 없음 - 유사도 0%")
    void testNoOverlap() {
        // Given
        Map<String, Double> portfolio1 = Map.of("AAPL", 0.5, "MSFT", 0.5);
        Map<String, Double> portfolio2 = Map.of("GOOGL", 0.6, "TSLA", 0.4);

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertEquals(0.0, similarity, 0.1, "겹치는 종목 없으면 0%");
    }

    @Test
    @DisplayName("부분적으로 겹치는 포트폴리오")
    void testPartialOverlap() {
        // Given
        Map<String, Double> portfolio1 = Map.of(
            "AAPL", 0.5,
            "MSFT", 0.5
        );
        Map<String, Double> portfolio2 = Map.of(
            "AAPL", 0.4,
            "MSFT", 0.4,
            "GOOGL", 0.2
        );

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertTrue(similarity > 0 && similarity < 100, "부분 겹침은 0% < 유사도 < 100%");
        assertTrue(similarity > 90, "비중이 비슷하므로 높은 유사도 기대");
    }

    @Test
    @DisplayName("비중 패턴이 유사한 포트폴리오")
    void testSimilarWeightPatterns() {
        // Given
        Map<String, Double> portfolio1 = Map.of(
            "AAPL", 0.4,
            "MSFT", 0.6
        );
        Map<String, Double> portfolio2 = Map.of(
            "AAPL", 0.45,
            "MSFT", 0.55
        );

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertTrue(similarity > 99, "비중 패턴이 매우 유사하면 99% 이상");
    }

    @Test
    @DisplayName("정반대 비중 패턴")
    void testOppositeWeightPatterns() {
        // Given
        Map<String, Double> portfolio1 = Map.of(
            "AAPL", 0.9,
            "MSFT", 0.1
        );
        Map<String, Double> portfolio2 = Map.of(
            "AAPL", 0.1,
            "MSFT", 0.9
        );

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertTrue(similarity < 50, "정반대 비중은 낮은 유사도");
    }

    @Test
    @DisplayName("null 포트폴리오 예외 처리")
    void testNullPortfolio() {
        // Given
        Map<String, Double> portfolio = Map.of("AAPL", 0.5);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculate(null, portfolio);
        }, "portfolio1이 null이면 예외");

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculate(portfolio, null);
        }, "portfolio2가 null이면 예외");
    }

    @Test
    @DisplayName("빈 포트폴리오 예외 처리")
    void testEmptyPortfolio() {
        // Given
        Map<String, Double> emptyPortfolio = new HashMap<>();
        Map<String, Double> validPortfolio = Map.of("AAPL", 0.5);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculate(emptyPortfolio, validPortfolio);
        }, "빈 포트폴리오는 예외");
    }

    @Test
    @DisplayName("모든 비중이 0인 포트폴리오 - 유사도 0%")
    void testZeroWeightPortfolio() {
        // Given
        Map<String, Double> portfolio1 = Map.of("AAPL", 0.5, "MSFT", 0.5);
        Map<String, Double> portfolio2 = Map.of("AAPL", 0.0, "MSFT", 0.0);

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertEquals(0.0, similarity, 0.1, "모든 비중 0이면 유사도 0%");
    }

    @Test
    @DisplayName("음수 비중 포함 (LONG_SHORT 모드)")
    void testNegativeWeights() {
        // Given
        Map<String, Double> portfolio1 = Map.of(
            "AAPL", 0.6,
            "MSFT", 0.4
        );
        Map<String, Double> portfolio2 = Map.of(
            "AAPL", 0.6,
            "MSFT", -0.4  // 숏 포지션
        );

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertTrue(similarity >= 0, "유사도는 0 이상");
        assertTrue(similarity < 100, "long vs short 혼합은 100% 미만");
    }

    @Test
    @DisplayName("둘 다 숏 포지션이면 유사도 증가")
    void testBothShortPositions() {
        // Given
        Map<String, Double> portfolio1 = Map.of(
            "AAPL", -0.6,   // 둘 다 AAPL 숏
            "MSFT", 0.4
        );
        Map<String, Double> portfolio2 = Map.of(
            "AAPL", -0.6,   // 둘 다 AAPL 숏
            "MSFT", 0.4
        );

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertEquals(100.0, similarity, 0.1, "둘 다 숏이면 유사도 증가 (음수×음수=양수)");
    }

    @Test
    @DisplayName("희소 벡터 테스트 - 크기가 매우 다른 포트폴리오")
    void testSparseVectors() {
        // Given
        Map<String, Double> userPortfolio = Map.of(
            "AAPL", 0.5,
            "MSFT", 0.5
        );

        Map<String, Double> investorPortfolio = Map.of(
            "AAPL", 0.1,
            "MSFT", 0.1,
            "GOOGL", 0.2,
            "TSLA", 0.2,
            "AMZN", 0.2,
            "NVDA", 0.2   // 6개 종목
        );

        // When
        double similarity = calculator.calculate(userPortfolio, investorPortfolio);

        // Then
        assertTrue(similarity > 0, "겹치는 종목(AAPL, MSFT)이 있으므로 유사도 > 0");
        assertTrue(similarity < 100, "다른 종목들이 있으므로 유사도 < 100");
    }

    @Test
    @DisplayName("getName() 메소드 테스트")
    void testGetName() {
        // When
        String name = calculator.getName();

        // Then
        assertEquals("Cosine Similarity", name);
    }

    @Test
    @DisplayName("정규화된 포트폴리오 간 유사도 계산")
    void testNormalizedPortfolios() {
        // Given - 합이 1.0인 정규화된 포트폴리오
        Map<String, Double> portfolio1 = Map.of(
            "AAPL", 0.3,
            "MSFT", 0.4,
            "GOOGL", 0.3
        );

        Map<String, Double> portfolio2 = Map.of(
            "AAPL", 0.35,
            "MSFT", 0.35,
            "GOOGL", 0.3
        );

        // When
        double similarity = calculator.calculate(portfolio1, portfolio2);

        // Then
        assertTrue(similarity > 95, "비슷한 비중 분포는 높은 유사도");
        assertTrue(similarity <= 100, "유사도는 100% 이하");
    }

    @Test
    @DisplayName("실제 사용 시나리오 - TSLA 노출 비교")
    void testRealWorldScenario() {
        // Given
        // 사용자: TSLL 10% → TSLA 20% 노출 (정규화 후 100%)
        Map<String, Double> userExposure = Map.of("TSLA", 1.0);

        // 투자대가: TSLA 20% (정규화 후 100%)
        Map<String, Double> guruExposure = Map.of("TSLA", 1.0);

        // When
        double similarity = calculator.calculate(userExposure, guruExposure);

        // Then
        assertEquals(100.0, similarity, 0.1, "동일한 TSLA 노출이면 100% 유사");
    }
}