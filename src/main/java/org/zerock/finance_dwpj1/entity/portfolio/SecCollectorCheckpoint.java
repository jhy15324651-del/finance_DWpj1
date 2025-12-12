package org.zerock.finance_dwpj1.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SEC 13F 수집 체크포인트
 * 각 투자자별 수집 상태 및 재시도 정보 저장
 */
@Entity
@Table(name = "sec_collector_checkpoint",
    uniqueConstraints = @UniqueConstraint(columnNames = {"investor_id", "filing_quarter"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecCollectorCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investor_id", nullable = false, length = 50)
    private String investorId;

    @Column(name = "filing_quarter", nullable = false, length = 10)
    private String filingQuarter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CheckpointStatus status = CheckpointStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "holdings_count")
    private Integer holdingsCount;

    public enum CheckpointStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        SKIPPED
    }
}