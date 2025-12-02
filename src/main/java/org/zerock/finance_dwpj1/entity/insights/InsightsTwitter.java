package org.zerock.finance_dwpj1.entity.insights;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 트위터 인사이트 엔티티
 * 유명 투자자들의 트윗 데이터를 저장
 */
@Entity
@Table(name = "twitter_insights", indexes = {
        @Index(name = "idx_source_created_at", columnList = "source, created_at DESC"),
        @Index(name = "idx_tweet_date", columnList = "tweet_date DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightsTwitter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // 트위터 사용자 이름

    @Column(nullable = false, length = 50)
    private String handle; // 트위터 핸들 (@username)

    @Column(length = 500)
    private String avatar; // 프로필 이미지 URL

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false; // 인증 계정 여부

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalText; // 영어 원문

    @Column(columnDefinition = "TEXT")
    private String translatedText; // 한국어 번역

    @Column(name = "tweet_date", length = 50)
    private String tweetDate; // 트윗 날짜 (예: "2h ago", "Dec 1")

    @Column(length = 500)
    private String url; // 트윗 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TwitterSource source = TwitterSource.MANUAL; // 데이터 출처

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 데이터베이스 등록 시간

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false; // 소프트 삭제 여부

    /**
     * 트윗을 소프트 삭제
     */
    public void softDelete() {
        this.isDeleted = true;
    }

    /**
     * 트위터 데이터 출처 열거형
     */
    public enum TwitterSource {
        DUMMY,   // 더미 데이터 (샘플용)
        MANUAL,  // 관리자가 수동으로 입력
        API      // Twitter API로 자동 수집 (추후 구현)
    }
}