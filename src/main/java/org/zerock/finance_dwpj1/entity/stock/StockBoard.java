package org.zerock.finance_dwpj1.entity.stock;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_board")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 종목의 게시판인지 구분하는 티커 (005930, TSLA 등)
    @Column(nullable = false, length = 50)
    private String ticker;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String writer;

    @Column(updatable = false)
    private LocalDateTime regDate;

    private LocalDateTime modDate;

    @PrePersist
    public void onCreate() {
        this.regDate = LocalDateTime.now();
        this.modDate = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {

        this.modDate = LocalDateTime.now();
    }

    @Column(nullable = false)
    private int recommend = 0;

    @Column(nullable = false)
    private int unrecommend = 0;

    @Column(nullable = false)
    private int view = 0;
}