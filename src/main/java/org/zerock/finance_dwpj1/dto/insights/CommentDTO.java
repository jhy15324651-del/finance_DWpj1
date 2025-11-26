package org.zerock.finance_dwpj1.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zerock.finance_dwpj1.entity.insights.Comment;
import org.zerock.finance_dwpj1.entity.insights.News;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 댓글 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {

    private Long id;
    private Long newsId;
    private Long parentCommentId; // 부모 댓글 ID (답글인 경우)
    private String userName;
    private String content;
    private Integer likeCount; // 좋아요 수
    private Integer dislikeCount; // 싫어요 수
    private String createdAt; // 포맷팅된 날짜
    private Boolean isReply; // 답글 여부

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Entity → DTO 변환
     */
    public static CommentDTO fromEntity(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .newsId(comment.getNews().getId())
                .parentCommentId(comment.getParentComment() != null ?
                        comment.getParentComment().getId() : null)
                .userName(comment.getUserName())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .createdAt(comment.getCreatedAt().format(FORMATTER))
                .isReply(comment.isReply())
                .build();
    }

    /**
     * DTO → Entity 변환
     */
    public Comment toEntity(News news) {
        return Comment.builder()
                .news(news)
                .userName(this.userName)
                .content(this.content)
                .build();
    }

    /**
     * DTO → Entity 변환 (답글인 경우)
     */
    public Comment toEntity(News news, Comment parentComment) {
        return Comment.builder()
                .news(news)
                .parentComment(parentComment)
                .userName(this.userName)
                .content(this.content)
                .build();
    }
}
