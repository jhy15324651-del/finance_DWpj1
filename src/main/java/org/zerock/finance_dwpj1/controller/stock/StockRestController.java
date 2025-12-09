package org.zerock.finance_dwpj1.controller.stock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.finance_dwpj1.dto.stock.StockDetailDTO;
import org.zerock.finance_dwpj1.service.stock.StockService;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/stock/api")
public class StockRestController {

    private final StockService stockService;


    @GetMapping("/detail/{ticker}")
    public ResponseEntity<StockDetailDTO> getDetail(@PathVariable String ticker) {
        log.info("기업 상세 정보 요청 = {}", ticker);
        return ResponseEntity.ok(stockService.fetchCompanyDetail(ticker));

    }
}