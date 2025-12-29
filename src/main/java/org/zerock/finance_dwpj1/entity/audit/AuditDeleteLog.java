package org.zerock.finance_dwpj1.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 콘텐츠 삭제 감사 로그 엔티티
 * - 관리자의 소프트/하드 삭제 행위를 모두 기록
 * - 법적 요구사항 및 내부 감사를 위한 추적 가능성 확보
 */
@Entity
@Table(name = "audit_delete_log", indexes = {
        @Index(name = "idx_target_type_id", columnList = "target_type, target_id"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDeleteLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // 삭제 행위 정보
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeleteAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_title", length = 500)
    private String targetTitle;

    // ========================================
    // 행위자 정보
    // ========================================

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 100)
    private String actorEmail;

    @Column(name = "actor_role", length = 20)
    private String actorRole;

    // ========================================
    // 삭제 사유 및 추적 정보
    // ========================================

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ========================================
    // 생성 시간
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========================================
    // Enum 정의
    // ========================================

    public enum DeleteAction {
        SOFT_DELETE,  // 관리자 버튼 클릭 삭제
        HARD_DELETE   // 30일 경과 후 자동 물리 삭제
    }

    public enum TargetType {
        NEWS,           // InsightsNews
        CONTENT_REVIEW, // ContentReview
        INFO            // InfoPost (약력 소개)
    }

    // ========================================
    // 팩토리 메소드
    // ========================================

    /**
     * 소프트 삭제 로그 생성
     */
    public static AuditDeleteLog createSoftDeleteLog(
            TargetType targetType,
            Long targetId,
            String targetTitle,
            Long actorUserId,
            String actorEmail,
            String actorRole,
            String reason,
            String ipAddress,
            String userAgent
    ) {
        return AuditDeleteLog.builder()
                .action(DeleteAction.SOFT_DELETE)
                .targetType(targetType)
                .targetId(targetId)
                .targetTitle(targetTitle)
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .reason(reason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    /**
     * 하드 삭제 로그 생성
     */
    public static AuditDeleteLog createHardDeleteLog(
            TargetType targetType,
            Long targetId,
            String targetTitle
    ) {
        return AuditDeleteLog.builder()
                .action(DeleteAction.HARD_DELETE)
                .targetType(targetType)
                .targetId(targetId)
                .targetTitle(targetTitle)
                .actorUserId(null)
                .actorEmail("SYSTEM")
                .actorRole("SCHEDULER")
                .reason("AUTO_PURGE_AFTER_30_DAYS")
                .ipAddress(null)
                .userAgent(null)
                .build();
    }
}