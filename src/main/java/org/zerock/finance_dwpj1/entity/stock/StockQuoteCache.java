package org.zerock.finance_dwpj1.entity.stock;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 주식 시세 캐시 엔티티
 * Yahoo Finance API 호출 결과를 캐싱합니다.
 */
@Entity
@Table(name = "stock_quote_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockQuoteCache {

    @Id
    @Column(length = 20)
    private String ticker;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String market;

    private Double currentPrice;

    private Double changeAmount;

    private Double changeRate;

    private Long tradingVolume;

    private Long marketCap;

    private Double high52Week;

    private Double low52Week;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
