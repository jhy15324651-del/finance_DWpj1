package org.zerock.finance_dwpj1.service.stock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.stock.StockSimpleMetricDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDetailService {

    private final StockAlphaVantageService alphaService;
    private final StockNaverScrapeService naverService;

    public StockSimpleMetricDTO getDetail(String ticker) {

        // 🇰🇷 한국 주식 (숫자 6자리)
        if (ticker.matches("\\d{6}")) {
            log.info("🇰🇷 한국 주식 → 네이버 파싱: {}", ticker);
            return naverService.scrape(ticker);
        }

        // 🇺🇸 미국 주식
        log.info("🇺🇸 미국 주식 → AlphaVantage: {}", ticker);
        return alphaService.getSimpleMetric(ticker);

    }
}