package org.zerock.finance_dwpj1.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.portfolio.SecCollectorCheckpoint;
import org.zerock.finance_dwpj1.entity.portfolio.SecCollectorCheckpoint.CheckpointStatus;

import java.util.List;
import java.util.Optional;

public interface SecCollectorCheckpointRepository extends JpaRepository<SecCollectorCheckpoint, Long> {

    Optional<SecCollectorCheckpoint> findByInvestorIdAndFilingQuarter(String investorId, String filingQuarter);

    List<SecCollectorCheckpoint> findByStatus(CheckpointStatus status);

    List<SecCollectorCheckpoint> findByInvestorId(String investorId);

    boolean existsByInvestorIdAndFilingQuarterAndStatus(String investorId, String filingQuarter, CheckpointStatus status);
}