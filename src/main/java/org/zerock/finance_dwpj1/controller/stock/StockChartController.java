package org.zerock.finance_dwpj1.controller.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.stock.StockCandleDTO;
import org.zerock.finance_dwpj1.dto.stock.StockInfoDTO;
import org.zerock.finance_dwpj1.service.stock.StockService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주식 차트 컨트롤러
 * Yahoo Finance API + DB 캐싱 방식 사용
 */
@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j
public class StockChartController {

    private final StockService stockService;

    /**
     * 차트 페이지 뷰
     */
    @GetMapping("/chart")
    public String chartPage() {
        return "stock-chart";
    }

    /**
     * 종목 정보 조회 API
     */
    @GetMapping("/api/info/{ticker}")
    public ResponseEntity<StockInfoDTO> getStockInfo(@PathVariable String ticker) {
        log.info("종목 정보 조회: {}", ticker);

        try {
            StockInfoDTO stockInfo = stockService.getStockInfo(ticker);

            if (stockInfo == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(stockInfo);

        } catch (Exception e) {
            log.error("종목 정보 조회 실패: {}", ticker, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 캔들 데이터 조회 API
     * @param ticker 티커
     * @param timeframe D(일봉), W(주봉), M(월봉)
     * @param count 데이터 개수 (기본 120개)
     */
    @GetMapping("/api/candles/{ticker}")
    public ResponseEntity<List<StockCandleDTO>> getCandleData(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "D") String timeframe,
            @RequestParam(defaultValue = "120") int count) {

        log.info("캔들 데이터 조회: ticker={}, timeframe={}, count={}", ticker, timeframe, count);

        try {
            List<StockCandleDTO> candles = stockService.getCandleData(ticker, timeframe, count);

            if (candles.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(candles);

        } catch (Exception e) {
            log.error("캔들 데이터 조회 실패: {}", ticker, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 종목 검색 API
     */
    @GetMapping("/api/search")
    public ResponseEntity<List<StockInfoDTO>> searchStocks(@RequestParam String query) {
        log.info("종목 검색: {}", query);

        try {
            List<StockInfoDTO> results = stockService.searchStocks(query);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("종목 검색 실패: {}", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 헬스 체크 (API 연결 테스트용)
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Yahoo Finance API + DB 캐싱");
        response.put("description", "API 호출 최소화를 위해 DB 캐싱 적용됨");
        return ResponseEntity.ok(response);
    }
}
