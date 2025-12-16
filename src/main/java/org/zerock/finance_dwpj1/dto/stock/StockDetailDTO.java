package org.zerock.finance_dwpj1.dto.stock;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailDTO {

    private String ticker;
    private String price;
    private String marketCap;       // 시가총액
    private String per;
    private String roe;
}