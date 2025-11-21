package org.zerock.finance_dwpj1.entity.insights;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 뉴스 댓글 엔티티
 * 사용자가 뉴스에 작성한 댓글을 저장
 */
@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_news_id_created_at", columnList = "news_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comment_news"))
    private News news;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName; // 댓글 작성자 이름

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 댓글 내용

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false; // 관리자가 삭제한 경우

    /**
     * 댓글을 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
    }
}
