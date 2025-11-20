package org.zerock.finance_dwpj1.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorComparisonDTO {
    private String investorId;
    private List<PhilosophyItem> philosophy;
    private String insights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhilosophyItem {
        private String category;
        private int percentage;
    }
}