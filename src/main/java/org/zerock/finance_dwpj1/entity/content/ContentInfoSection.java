package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 약력 게시글의 섹션 엔티티
 *
 * 개념:
 * - 1개의 InfoPost에 여러 개의 섹션이 포함됨
 * - 각 섹션은 PROFILE, CHANNEL, ACTIVITY, WHY 등의 타입을 가짐
 * - 섹션별로 제목, 본문, 이미지를 가질 수 있음
 *
 * 관계:
 * - InfoPost (1) : ContentInfoSection (N)
 *
 * 제약:
 * - 같은 게시글(info_post_id) 내에서 동일한 섹션 타입(section_type)은 1개만 허용
 */
@Entity
@Table(
    name = "content_info_sections",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_info_post_section_type",
            columnNames = {"info_post_id", "section_type"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentInfoSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 섹션이 속한 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_post_id", nullable = false)
    private InfoPost infoPost;

    /**
     * 섹션 타입 (PROFILE, CHANNEL, ACTIVITY, WHY, SIGNATURE, SOURCES)
     */
    @Column(nullable = false, length = 50)
    private String sectionType;

    /**
     * 게시글 제목 (예: "홍길동의 프로필", "김철수의 채널 소개")
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 게시글 본문 내용 (HTML 또는 마크다운)
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 섹션별 이미지 URL (선택사항)
     * 예: "/uploads/info/uuid.jpg"
     */
    @Column(length = 255)
    private String thumbnailUrl;

    /**
     * 정렬 순서 (섹션 표시 순서)
     * PROFILE(1) → CHANNEL(2) → ACTIVITY(3) → WHY(4) → ...
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 999;
}