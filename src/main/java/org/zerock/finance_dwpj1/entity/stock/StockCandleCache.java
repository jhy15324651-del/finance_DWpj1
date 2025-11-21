package org.zerock.finance_dwpj1.entity.stock;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 캔들(OHLCV) 캐시 엔티티
 * Yahoo Finance API 호출 결과를 캐싱합니다.
 */
@Entity
@Table(name = "stock_candle_cache")
@IdClass(StockCandleCacheId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCandleCache {

    @Id
    @Column(length = 20)
    private String ticker;

    @Id
    private LocalDate date;

    @Id
    @Column(length = 1)
    private String timeframe; // D, W, M

    private Double open;

    private Double high;

    private Double low;

    private Double close;

    private Long volume;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
