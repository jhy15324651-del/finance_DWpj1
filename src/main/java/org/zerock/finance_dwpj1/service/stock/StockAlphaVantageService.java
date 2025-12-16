package org.zerock.finance_dwpj1.service.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zerock.finance_dwpj1.dto.stock.StockAlphaOverviewDTO;
import org.zerock.finance_dwpj1.dto.stock.StockSimpleMetricDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlphaVantageService {

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public StockAlphaOverviewDTO getOverview(String symbol) {


        String url = baseUrl
                + "?function=OVERVIEW"
                + "&symbol=" + symbol
                + "&apikey=" + apiKey;

        log.info("AlphaVantage URL = {}", url);

        String response = restTemplate.getForObject(url, String.class);

        log.info("AlphaVantage RAW response = {}", response);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, StockAlphaOverviewDTO.class);
        } catch (Exception e) {
            log.error("AlphaVantage parsing error", e);
            throw new RuntimeException("AlphaVantage response parse failed");
        }
    }

    public StockSimpleMetricDTO getSimpleMetric(String symbol) {

        StockAlphaOverviewDTO o = getOverview(symbol);

        if (o == null) return null;

        return StockSimpleMetricDTO.builder()
                .per(o.getPer())
                .roe(o.getRoe())
                .dividend(o.getDividend())
                .dividendYield(o.getDividendYield())
                .marketCap(o.getMarketCap())
                .sharesOutstanding(o.getSharesOutstanding())
                .build();
    }


}