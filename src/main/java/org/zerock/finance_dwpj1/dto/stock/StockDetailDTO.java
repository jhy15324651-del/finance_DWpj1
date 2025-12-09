package org.zerock.finance_dwpj1.dto.stock;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockDetailDTO {

    private String ticker;
    private String name;          // 기업명
    private String sector;        // 섹터
    private String industry;      // 산업군
    private String exchange;      // 상장 거래소
    private String country;       // 국가

    private long marketCap;       // 시가총액
    private long volume;          // 현재 거래량
}