package org.zerock.finance_dwpj1.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 포트폴리오 분석 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisRequest {

    /**
     * 사용자 포트폴리오
     * Key: 티커 (AAPL, TSLA 등)
     * Value: 비중 (%)
     */
    private Map<String, Double> portfolio;
}
