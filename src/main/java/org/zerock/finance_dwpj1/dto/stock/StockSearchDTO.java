package org.zerock.finance_dwpj1.dto.stock;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockSearchDTO {

    private String type;
    private String keyword;
}
