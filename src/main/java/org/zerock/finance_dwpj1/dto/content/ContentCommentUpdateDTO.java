package org.zerock.finance_dwpj1.dto.content;

import lombok.Data;

@Data
public class ContentCommentUpdateDTO {
    private String content; // 수정된 댓글 내용
    private Double rating;  // ⭐ 수정된 평점
}
