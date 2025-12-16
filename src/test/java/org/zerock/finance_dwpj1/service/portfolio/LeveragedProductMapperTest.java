package org.zerock.finance_dwpj1.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.finance_dwpj1.service.portfolio.LeveragedProductMapper.ProductMapping;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LeveragedProductMapper 단위 테스트
 */
class LeveragedProductMapperTest {

    private LeveragedProductMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LeveragedProductMapper();
        mapper.initializeMappings();
    }

    @Test
    @DisplayName("TSLL (TSLA 2배 레버리지) 매핑 테스트")
    void testTsllMapping() {
        // Given
        String ticker = "TSLL";

        // When
        ProductMapping mapping = mapper.getMapping(ticker);

        // Then
        assertEquals("TSLA", mapping.getBaseTicker(), "TSLL의 기초자산은 TSLA");
        assertEquals(2.0, mapping.getFactor(), 0.001, "TSLL의 레버리지 배수는 2.0");
    }

    @Test
    @DisplayName("TQQQ (QQQ 3배 레버리지) 매핑 테스트")
    void testTqqqMapping() {
        // Given
        String ticker = "TQQQ";

        // When
        ProductMapping mapping = mapper.getMapping(ticker);

        // Then
        assertEquals("QQQ", mapping.getBaseTicker());
        assertEquals(3.0, mapping.getFactor(), 0.001);
    }

    @Test
    @DisplayName("SQQQ (QQQ 3배 인버스) 매핑 테스트 - 음수 factor")
    void testSqqqMapping() {
        // Given
        String ticker = "SQQQ";

        // When
        ProductMapping mapping = mapper.getMapping(ticker);

        // Then
        assertEquals("QQQ", mapping.getBaseTicker());
        assertEquals(-3.0, mapping.getFactor(), 0.001, "인버스는 음수 factor");
    }

    @Test
    @DisplayName("일반 종목 (매핑 없음) - 자기 자신 + factor=1.0 반환")
    void testNormalStockMapping() {
        // Given
        String ticker = "AAPL";

        // When
        ProductMapping mapping = mapper.getMapping(ticker);

        // Then
        assertEquals("AAPL", mapping.getBaseTicker(), "매핑 없는 종목은 자기 자신");
        assertEquals(1.0, mapping.getFactor(), 0.001, "일반 종목의 factor는 1.0");
    }

    @Test
    @DisplayName("대소문자 무관 테스트")
    void testCaseInsensitive() {
        // Given
        String lowerCase = "tsll";
        String upperCase = "TSLL";
        String mixedCase = "TsLl";

        // When
        ProductMapping mapping1 = mapper.getMapping(lowerCase);
        ProductMapping mapping2 = mapper.getMapping(upperCase);
        ProductMapping mapping3 = mapper.getMapping(mixedCase);

        // Then
        assertEquals("TSLA", mapping1.getBaseTicker());
        assertEquals("TSLA", mapping2.getBaseTicker());
        assertEquals("TSLA", mapping3.getBaseTicker());
    }

    @Test
    @DisplayName("레버리지 상품 여부 확인")
    void testIsLeveragedProduct() {
        // When & Then
        assertTrue(mapper.isLeveragedProduct("TSLL"), "TSLL은 레버리지 상품");
        assertTrue(mapper.isLeveragedProduct("TQQQ"), "TQQQ는 레버리지 상품");
        assertTrue(mapper.isLeveragedProduct("SQQQ"), "SQQQ는 인버스 상품");
        assertFalse(mapper.isLeveragedProduct("AAPL"), "AAPL은 일반 종목");
        assertFalse(mapper.isLeveragedProduct("TSLA"), "TSLA는 일반 종목");
    }

    @Test
    @DisplayName("새로운 매핑 추가 테스트")
    void testAddMapping() {
        // Given
        String newTicker = "NVDL";
        String baseTicker = "NVDA";
        double factor = 2.0;

        // When
        mapper.addMapping(newTicker, baseTicker, factor);
        ProductMapping mapping = mapper.getMapping(newTicker);

        // Then
        assertEquals(baseTicker, mapping.getBaseTicker());
        assertEquals(factor, mapping.getFactor(), 0.001);
        assertTrue(mapper.isLeveragedProduct(newTicker));
    }

    @Test
    @DisplayName("factor=0 입력 시 예외 발생")
    void testZeroFactorThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            mapper.addMapping("TEST", "BASE", 0.0);
        }, "factor=0은 예외 발생");
    }

    @Test
    @DisplayName("모든 매핑 조회 테스트")
    void testGetAllMappings() {
        // When
        var allMappings = mapper.getAllMappings();

        // Then
        assertNotNull(allMappings);
        assertTrue(allMappings.size() > 0, "최소 1개 이상의 매핑 존재");
        assertTrue(allMappings.containsKey("TSLL"));
        assertTrue(allMappings.containsKey("TQQQ"));
        assertTrue(allMappings.containsKey("SQQQ"));
    }

    @Test
    @DisplayName("SOXL (반도체 3배 레버리지) 매핑 테스트")
    void testSoxlMapping() {
        // Given
        String ticker = "SOXL";

        // When
        ProductMapping mapping = mapper.getMapping(ticker);

        // Then
        assertEquals("SOXX", mapping.getBaseTicker());
        assertEquals(3.0, mapping.getFactor(), 0.001);
    }

    @Test
    @DisplayName("SPXL (S&P500 3배 레버리지) 매핑 테스트")
    void testSpxlMapping() {
        // Given
        String ticker = "SPXL";

        // When
        ProductMapping mapping = mapper.getMapping(ticker);

        // Then
        assertEquals("SPY", mapping.getBaseTicker());
        assertEquals(3.0, mapping.getFactor(), 0.001);
    }
}