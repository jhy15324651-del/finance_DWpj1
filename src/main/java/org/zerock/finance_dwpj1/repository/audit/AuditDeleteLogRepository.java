package org.zerock.finance_dwpj1.repository.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.audit.AuditDeleteLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 로그 Repository
 */
@Repository
public interface AuditDeleteLogRepository extends JpaRepository<AuditDeleteLog, Long> {

    /**
     * 특정 대상의 삭제 로그 조회
     */
    List<AuditDeleteLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            AuditDeleteLog.TargetType targetType,
            Long targetId
    );

    /**
     * 특정 관리자가 수행한 삭제 로그 조회
     */
    List<AuditDeleteLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail);

    /**
     * 특정 기간 동안의 삭제 로그 조회
     */
    @Query("SELECT a FROM AuditDeleteLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditDeleteLog> findLogsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 액션의 로그 조회
     */
    List<AuditDeleteLog> findByActionOrderByCreatedAtDesc(AuditDeleteLog.DeleteAction action);
}