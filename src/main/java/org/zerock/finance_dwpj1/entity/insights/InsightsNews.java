package org.zerock.finance_dwpj1.entity.insights;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 뉴스 기사 엔티티
 * Yahoo Finance에서 크롤링한 뉴스 데이터를 저장
 */
@Entity
@Table(name = "news", indexes = {
        @Index(name = "idx_status_created_at", columnList = "status, created_at DESC"),
        @Index(name = "idx_view_count", columnList = "view_count DESC"),
        @Index(name = "idx_url", columnList = "url", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightsNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // 한국어 번역 본문

    @Column(columnDefinition = "TEXT")
    private String originalContent; // 영어 원문

    @Column(columnDefinition = "TEXT")
    private String summary; // GPT 요약

    @Column(nullable = false, unique = true, length = 1000)
    private String url; // 중복 방지를 위한 UNIQUE 제약

    @Column(length = 100)
    private String source; // 출처 (Yahoo Finance)

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // 기사 발행 시간

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 크롤링된 시간

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L; // 조회수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NewsStatus status = NewsStatus.DAILY; // DAILY(24시간 이내) 또는 ARCHIVE(24시간 이상)

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false; // 관리자가 삭제한 경우

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 24시간이 경과했는지 확인
     */
    public boolean isOver24Hours() {
        return createdAt.plusHours(24).isBefore(LocalDateTime.now());
    }

    /**
     * 뉴스 상태를 ARCHIVE로 변경
     */
    public void archiveNews() {
        this.status = NewsStatus.ARCHIVE;
    }

    /**
     * 뉴스를 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
    }

    /**
     * 뉴스 상태 열거형
     */
    public enum NewsStatus {
        DAILY,   // 24시간 이내
        ARCHIVE  // 24시간 이상
    }
}
