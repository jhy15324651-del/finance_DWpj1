package org.zerock.finance_dwpj1.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zerock.finance_dwpj1.entity.insights.News;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 데일리 뉴스 DTO
 * Yahoo Finance 크롤링 뉴스 데이터 전송
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyNewsDTO {

    private Long id;
    private String title;
    private String content;
    private String summary; // GPT 요약
    private String url;
    private String source;
    private String publishedAt; // 포맷팅된 날짜
    private String createdAt;   // 포맷팅된 날짜
    private Long viewCount;
    private String status; // DAILY or ARCHIVE
    private Long commentCount; // 댓글 개수

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Entity → DTO 변환
     */
    public static DailyNewsDTO fromEntity(News news) {
        return DailyNewsDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .summary(news.getSummary())
                .url(news.getUrl())
                .source(news.getSource())
                .publishedAt(news.getPublishedAt() != null ?
                        news.getPublishedAt().format(FORMATTER) : null)
                .createdAt(news.getCreatedAt().format(FORMATTER))
                .viewCount(news.getViewCount())
                .status(news.getStatus().name())
                .build();
    }

    /**
     * Entity → DTO 변환 (댓글 개수 포함)
     */
    public static DailyNewsDTO fromEntity(News news, Long commentCount) {
        DailyNewsDTO dto = fromEntity(news);
        dto.setCommentCount(commentCount);
        return dto;
    }

    /**
     * DTO → Entity 변환
     */
    public News toEntity() {
        return News.builder()
                .title(this.title)
                .content(this.content)
                .summary(this.summary)
                .url(this.url)
                .source(this.source)
                .publishedAt(this.publishedAt != null ?
                        LocalDateTime.parse(this.publishedAt, FORMATTER) : null)
                .build();
    }
}
