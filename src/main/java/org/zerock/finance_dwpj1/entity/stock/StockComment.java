package org.zerock.finance_dwpj1.entity.stock;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="stock_board_comment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private StockBoard board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private StockComment parent;

    // 0 = 댓글, 1 = 대댓글
    @Column(nullable = false)
    private int depth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime regDate;
    private LocalDateTime modDate;

    @Column(nullable = false, length=50)
    private String writer;

    @PrePersist
    public void onCreate(){
        this.regDate = LocalDateTime.now();
        this.modDate = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate(){
        this.modDate = LocalDateTime.now();
    }
}
