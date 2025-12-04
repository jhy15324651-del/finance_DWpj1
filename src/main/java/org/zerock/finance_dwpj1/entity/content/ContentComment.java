package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;      // 댓글이 달리는 게시글 ID
    private Long userId;      // 작성자 회원 ID
    private String writer;    // 작성자 닉네임
    private String content;   // 댓글 내용

    // ⭐ 추가: 평점 (0.0 ~ 5.0, 0.5 단위)
    @Column
    private Double rating;

    // 대댓글 기능 툴
    @Column(name = "parent_id")
    private Long parentId;    // 부모 댓글 ID (null이면 루트 댓글)

    private LocalDateTime createdDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }
}
