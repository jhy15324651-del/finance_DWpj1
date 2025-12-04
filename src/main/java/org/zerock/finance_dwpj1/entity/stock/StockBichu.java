package org.zerock.finance_dwpj1.entity.stock;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_bichu", uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBichu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private StockBoard board;

    @Column(name="user_id", nullable = false)
    private Long userId;
}
