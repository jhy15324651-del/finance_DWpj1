package org.zerock.finance_dwpj1.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주식 기본 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoDTO {

    private String ticker;          // 티커 (종목코드)
    private String name;            // 종목명
    private String market;          // 시장 (KOSPI/KOSDAQ/NASDAQ 등)
    private Double currentPrice;    // 현재가
    private Double changeAmount;    // 전일대비 금액
    private Double changeRate;      // 전일대비 등락률 (%)
    private Long tradingVolume;     // 거래량
    private Long marketCap;         // 시가총액
    private Double high52Week;      // 52주 최고가
    private Double low52Week;       // 52주 최저가
    private String currency;     //금액 단위
}
