package org.zerock.finance_dwpj1.entity.content;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // 부모 댓글 ID (원댓글이면 null, 대댓글이면 부모의 id)
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Transient
    private List<ContentComment> replies = new ArrayList<>();

    public List<ContentComment> getReplies() {
        return replies;
    }

    public void setReplies(List<ContentComment> replies) {
        this.replies = replies;
    }

    private LocalDateTime createdDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }
}
