package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 콘텐츠 리뷰 엔티티
 * 사용자가 작성하는 콘텐츠 리뷰/게시글을 저장
 */
@Entity
@Table(name = "content_review", indexes = {
        @Index(name = "idx_category_created_date", columnList = "category, created_date DESC"),
        @Index(name = "idx_type_created_date", columnList = "type, created_date DESC"),
        @Index(name = "idx_view_count", columnList = "view_count DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "img_url", length = 1000)
    private String imgUrl;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(length = 100)
    private String category; // 공지, 거시경제, 원유, 엔비디아, 테슬라 등

    @Column(length = 50)
    @Builder.Default
    private String type = "review"; // review, info 등

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
    }
}