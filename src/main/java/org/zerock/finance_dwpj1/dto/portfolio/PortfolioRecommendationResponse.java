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
public class PortfolioRecommendationResponse {
    private List<String> selectedInvestors;
    private String combinedPhilosophy;
    private List<StockRecommendation> recommendations;
    private String rationale;
    private String riskProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockRecommendation {
        private String symbol;
        private String name;
        private String sector;
        private double allocation;
        private String reason;
    }
}