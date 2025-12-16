package org.zerock.finance_dwpj1.controller.stock;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.stock.StockSimpleMetricDTO;
import org.zerock.finance_dwpj1.service.stock.StockAlphaVantageService;
import org.zerock.finance_dwpj1.service.stock.StockDetailService;
import org.zerock.finance_dwpj1.service.stock.StockNaverScrapeService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/stock/api")
    public class StockDetailController {

        private final StockDetailService detailService;

        @GetMapping("/detail/{ticker}")
        public StockSimpleMetricDTO detail(@PathVariable String ticker) {
            return detailService.getDetail(ticker);
        }
}