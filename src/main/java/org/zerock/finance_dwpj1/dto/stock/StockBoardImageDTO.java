package org.zerock.finance_dwpj1.dto.stock;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockBoardImageDTO {

    private Long id;
    private String fileName;
    private String filePath;
}
