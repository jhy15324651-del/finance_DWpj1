package org.zerock.finance_dwpj1.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserPostDTO {
    private Long id;
    private String title;
    private String category;     // "콘텐츠 리뷰", "종목 토론방"
    private LocalDateTime date;
    private Integer viewCount;
    private String link;         // 클릭했을 때 이동할 URL
    private Boolean isDeleted;
}