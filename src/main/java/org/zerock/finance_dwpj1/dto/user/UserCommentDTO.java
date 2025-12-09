package org.zerock.finance_dwpj1.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserCommentDTO {

    private Long id;               // 댓글 ID
    private String content;        // 댓글 내용
    private LocalDateTime date;    // 댓글 작성일

    private String category;       // 댓글이 달린 게시판 종류 ("콘텐츠 리뷰", "종목 토론방")
    private String postTitle;      // 댓글이 달린 게시글 제목
    private String postLink;       // 해당 게시글 상세보기 링크
}
