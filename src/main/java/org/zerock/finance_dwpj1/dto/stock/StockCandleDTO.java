package org.zerock.finance_dwpj1.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주식 캔들 데이터 DTO
 * 일봉/주봉/월봉 차트에 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCandleDTO {

    private String date;        // 날짜 (YYYY-MM-DD)
    private Double open;        // 시가
    private Double high;        // 고가
    private Double low;         // 저가
    private Double close;       // 종가
    private Long volume;        // 거래량

    // 기술적 지표 (선택적)
    private Double sma20;       // 20일 이동평균선
    private Double sma60;       // 60일 이동평균선
    private Double sma120;      // 120일 이동평균선
    private Double rsi;         // RSI 지표
    private Double macd;        // MACD 지표
    private Double signal;      // MACD 시그널
}
