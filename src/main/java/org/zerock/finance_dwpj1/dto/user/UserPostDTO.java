package org.zerock.finance_dwpj1.dto.user;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPostDTO {
    private Long id;
    private String title;
    private String category;     // "콘텐츠 리뷰", "종목 토론방"
    private LocalDateTime date;
    private Integer viewCount;
    private String link;         // 클릭했을 때 이동할 URL

    //마이페이지 관련
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private Integer deleteRemainDays;

}