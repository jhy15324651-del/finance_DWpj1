package org.zerock.finance_dwpj1.dto.stock;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockBoardDTO {

    private Long id;

    private String ticker;    // 종목 코드

    private String title;

    private String content;

    private String writer;

    private LocalDateTime regDate; // BaseEntity 기준

    private LocalDateTime modDate;

    private Integer view;

    private Integer recommend;

    private Integer unrecommend;

    private Integer commentCount;
}