package org.zerock.finance_dwpj1.dto.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class StockAlphaOverviewDTO {

    @JsonProperty("PERatio")
    private BigDecimal per;

    @JsonProperty("ReturnOnEquityTTM")
    private BigDecimal roe;

    @JsonProperty("DividendPerShare")
    private BigDecimal dividend;

    @JsonProperty("DividendYield")
    private BigDecimal dividendYield;

    @JsonProperty("MarketCapitalization")
    private BigDecimal marketCap;

    @JsonProperty("SharesOutstanding")
    private Long sharesOutstanding;
}