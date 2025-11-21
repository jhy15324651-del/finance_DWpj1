package org.zerock.finance_dwpj1.entity.stock;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * StockCandleCache 복합 키 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockCandleCacheId implements Serializable {
    private String ticker;
    private LocalDate date;
    private String timeframe;
}