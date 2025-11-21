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
    private String userName;
    private String content;
    private String createdAt; // 포맷팅된 날짜

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Entity → DTO 변환
     */
    public static CommentDTO fromEntity(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .newsId(comment.getNews().getId())
                .userName(comment.getUserName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().format(FORMATTER))
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
}
