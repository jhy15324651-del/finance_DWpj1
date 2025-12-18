package org.zerock.finance_dwpj1.entity.insights;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 투자자 인사이트 엔티티
 * 투자자의 투자 철학을 관리자가 직접 관리하는 정적 콘텐츠
 */
@Entity
@Table(name = "investor_insight", indexes = {
        @Index(name = "idx_investor_id", columnList = "investor_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 투자자 고유 ID (예: "buffett", "wood", "dalio")
     */
    @Column(name = "investor_id", nullable = false, unique = true, length = 50)
    private String investorId;

    /**
     * 투자자 이름 (한국어)
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 투자 철학 (한국어)
     */
    @Column(name = "philosophy_ko", columnDefinition = "TEXT", nullable = false)
    private String philosophyKo;

    /**
     * 투자 스타일 (예: "가치투자", "성장주 투자", "퀀트 투자")
     */
    @Column(name = "investment_style", length = 100)
    private String investmentStyle;

    /**
     * 프로필 이미지 URL (선택 사항)
     */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 관리자가 수정한 마지막 관리자 이메일
     */
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;
}