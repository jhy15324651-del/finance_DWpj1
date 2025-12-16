package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 약력 게시글 엔티티
 *
 * 개념:
 * - 글 1개 = InfoPost 1개
 * - 글 안에 여러 섹션(PROFILE, CHANNEL, ACTIVITY, WHY 등) 포함
 *
 * 구조:
 * InfoPost (1)
 *  ├─ ContentInfoSection (PROFILE)
 *  ├─ ContentInfoSection (CHANNEL)
 *  ├─ ContentInfoSection (ACTIVITY)
 *  └─ ContentInfoSection (WHY)
 */
@Entity
@Table(name = "info_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 게시글 제목 (PROFILE 섹션의 제목을 사용)
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 대표 썸네일 (PROFILE 섹션의 이미지를 사용)
     */
    @Column(length = 255)
    private String thumbnailUrl;

    /**
     * 작성자 닉네임
     */
    @Column(nullable = false, length = 50)
    private String writer;

    /**
     * 소프트 삭제 플래그
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 생성일시
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * 수정일시
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;

    /**
     * 삭제일시 (소프트 삭제 시 기록)
     */
    @Column
    private LocalDateTime deletedDate;

    /**
     * 삭제한 관리자 ID
     */
    @Column(length = 50)
    private String deletedBy;

    /**
     * 이 글에 포함된 섹션들
     * (PROFILE, CHANNEL, ACTIVITY, WHY, SIGNATURE, SOURCES)
     */
    @OneToMany(mappedBy = "infoPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ContentInfoSection> sections = new ArrayList<>();

    /**
     * 섹션 추가 헬퍼 메서드
     */
    public void addSection(ContentInfoSection section) {
        sections.add(section);
        section.setInfoPost(this);
    }

    /**
     * 섹션 제거 헬퍼 메서드
     */
    public void removeSection(ContentInfoSection section) {
        sections.remove(section);
        section.setInfoPost(null);
    }

    /**
     * PROFILE 섹션 찾기 (대표 정보 추출용)
     */
    public ContentInfoSection getProfileSection() {
        return sections.stream()
                .filter(s -> "PROFILE".equals(s.getSectionType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 소프트 삭제 실행
     */
    public void softDelete(String deletedBy) {
        this.isDeleted = true;
        this.deletedDate = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * 삭제 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedDate = null;
        this.deletedBy = null;
    }
}