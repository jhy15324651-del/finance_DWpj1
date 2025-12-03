package org.zerock.finance_dwpj1.dto.content;

import lombok.Data;

@Data
public class ContentCommentWriteDTO {
    private Long postId;     // 댓글이 달릴 게시글 ID
    private String content;  // 댓글 내용
    private Double rating;  //평점
}
