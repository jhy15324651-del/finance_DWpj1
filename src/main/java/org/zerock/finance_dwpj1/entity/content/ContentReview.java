package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_review", indexes = {
        @Index(name = "idx_hashtags", columnList = "hashtags"),
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

    @Column(name = "img_url", length = 1000)
    private String imgUrl;

    @Column(length = 2000)
    private String hashtags;   // "#원유 #테슬라 #경제"

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(length = 50)
    @Builder.Default
    private String type = "review";

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
