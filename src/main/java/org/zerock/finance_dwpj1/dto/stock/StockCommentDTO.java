package org.zerock.finance_dwpj1.dto.stock;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockCommentDTO {

    private Long id;
    private Long boardId;
    private String writer;
    private String content;
    private LocalDateTime regDate;
    private LocalDateTime modDate;
    private String grade;
    private String medal;
    private Long parentId;
    private int depth;

}
