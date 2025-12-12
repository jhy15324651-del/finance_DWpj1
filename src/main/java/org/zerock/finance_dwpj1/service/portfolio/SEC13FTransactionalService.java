package org.zerock.finance_dwpj1.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.entity.portfolio.Investor13FHolding;
import org.zerock.finance_dwpj1.entity.portfolio.InvestorProfile;
import org.zerock.finance_dwpj1.entity.portfolio.SecCollectorCheckpoint;
import org.zerock.finance_dwpj1.repository.portfolio.Investor13FHoldingRepository;
import org.zerock.finance_dwpj1.repository.portfolio.SecCollectorCheckpointRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SEC 13F 트랜잭션 전담 서비스
 * - 각 투자자별 독립적인 트랜잭션 처리
 * - Checkpoint 저장 및 업데이트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SEC13FTransactionalService {

    private final Investor13FHoldingRepository holdingRepository;
    private final SecCollectorCheckpointRepository checkpointRepository;

    /**
     * Checkpoint 저장 또는 업데이트 (IN_PROGRESS)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecCollectorCheckpoint markInProgress(String investorId, String quarter) {
        SecCollectorCheckpoint checkpoint = checkpointRepository
                .findByInvestorIdAndFilingQuarter(investorId, quarter)
                .orElse(SecCollectorCheckpoint.builder()
                        .investorId(investorId)
                        .filingQuarter(quarter)
                        .retryCount(0)
                        .build());

        checkpoint.setStatus(SecCollectorCheckpoint.CheckpointStatus.IN_PROGRESS);
        checkpoint.setStartedAt(LocalDateTime.now());
        checkpoint.setRetryCount(checkpoint.getRetryCount() + 1);

        return checkpointRepository.save(checkpoint);
    }

    /**
     * Holdings 저장 (이미 존재 여부 확인 포함)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveHoldings(String investorId, String quarter, List<Investor13FHolding> holdings) {
        if (holdings.isEmpty()) {
            return 0;
        }

        // 이미 존재하는지 확인
        if (holdingRepository.existsByInvestorIdAndFilingQuarter(investorId, quarter)) {
            log.info("{}의 {} 데이터가 이미 존재합니다. 건너뜁니다.", investorId, quarter);
            return 0;
        }

        // 저장
        holdingRepository.saveAll(holdings);
        holdingRepository.flush();

        log.info("{}의 13F 데이터 {}건 저장 완료 (분기: {})",
                investorId, holdings.size(), quarter);

        return holdings.size();
    }

    /**
     * Checkpoint를 SUCCESS로 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String investorId, String quarter, int holdingsCount) {
        SecCollectorCheckpoint checkpoint = checkpointRepository
                .findByInvestorIdAndFilingQuarter(investorId, quarter)
                .orElseThrow(() -> new IllegalStateException("Checkpoint not found"));

        checkpoint.setStatus(SecCollectorCheckpoint.CheckpointStatus.SUCCESS);
        checkpoint.setCompletedAt(LocalDateTime.now());
        checkpoint.setHoldingsCount(holdingsCount);
        checkpoint.setFailReason(null);

        checkpointRepository.save(checkpoint);
    }

    /**
     * Checkpoint를 SKIPPED로 업데이트 (데이터 없음)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSkipped(String investorId, String quarter, String reason) {
        SecCollectorCheckpoint checkpoint = checkpointRepository
                .findByInvestorIdAndFilingQuarter(investorId, quarter)
                .orElseThrow(() -> new IllegalStateException("Checkpoint not found"));

        checkpoint.setStatus(SecCollectorCheckpoint.CheckpointStatus.SKIPPED);
        checkpoint.setCompletedAt(LocalDateTime.now());
        checkpoint.setFailReason(reason);

        checkpointRepository.save(checkpoint);
    }

    /**
     * Checkpoint를 FAILED로 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String investorId, String quarter, Exception e) {
        SecCollectorCheckpoint checkpoint = checkpointRepository
                .findByInvestorIdAndFilingQuarter(investorId, quarter)
                .orElse(SecCollectorCheckpoint.builder()
                        .investorId(investorId)
                        .filingQuarter(quarter)
                        .build());

        checkpoint.setStatus(SecCollectorCheckpoint.CheckpointStatus.FAILED);
        checkpoint.setCompletedAt(LocalDateTime.now());

        String failReason = e.getClass().getSimpleName() + ": " +
                (e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(500, e.getMessage().length()))
                        : "Unknown");
        checkpoint.setFailReason(failReason);

        checkpointRepository.save(checkpoint);
    }

    /**
     * Checkpoint 조회
     */
    @Transactional(readOnly = true)
    public SecCollectorCheckpoint findCheckpoint(String investorId, String quarter) {
        return checkpointRepository
                .findByInvestorIdAndFilingQuarter(investorId, quarter)
                .orElse(null);
    }

    /**
     * Holdings 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsHoldings(String investorId, String quarter) {
        return holdingRepository.existsByInvestorIdAndFilingQuarter(investorId, quarter);
    }
}