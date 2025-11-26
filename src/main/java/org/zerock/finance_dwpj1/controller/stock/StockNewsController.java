package org.zerock.finance_dwpj1.controller.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.service.stock.StockNewsService;

@RestController
@RequestMapping("/stock/api/news")
@RequiredArgsConstructor
public class StockNewsController {

    private final StockNewsService newsService;

    @GetMapping("/{ticker}")
    public ResponseEntity<String> getNews(@PathVariable String ticker) {
        return ResponseEntity.ok(
                newsService.getNews(ticker)
        );
    }
}