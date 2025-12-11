package org.zerock.finance_dwpj1.dto.content;

import lombok.Data;

@Data
public class ContentCommentWriteDTO {
    private Long postId;     // 댓글이 달릴 게시글 ID
    private String content;  // 댓글 내용
    private Double rating;  //평점

    // ⭐ 추가: 부모 댓글 ID (대댓글 기능)
    private Long parentCommentId;
}
