package org.zerock.finance_dwpj1.dto.stock;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockSimpleMetricDTO {

    private BigDecimal per;              // PER
    private BigDecimal roe;              // ROE (TTM)
    private BigDecimal dividend;         // 주당배당금
    private BigDecimal dividendYield;    // 배당수익률
    private BigDecimal marketCap;         // 시가총액
    private Long sharesOutstanding;      // 상장주식수
}