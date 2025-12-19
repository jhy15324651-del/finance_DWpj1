package org.zerock.finance_dwpj1.dto.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 합의형 포트폴리오 응답 DTO
 * 4명의 투자자가 회의 후 모두 동의했을 법한 합의 종목 10개
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusPortfolioResponse {

    /**
     * 투자위원회 철학 (3~4문장)
     */
    @JsonProperty("investmentCommitteePhilosophy")
    private String investmentCommitteePhilosophy;

    /**
     * 합의 종목 리스트 (반드시 10개)
     */
    @JsonProperty("stocks")
    private List<ConsensusStock> stocks;

    /**
     * 포트폴리오 특성 요약
     */
    @JsonProperty("portfolioCharacteristics")
    private List<String> portfolioCharacteristics;

    /**
     * 리스크 노트
     */
    @JsonProperty("riskNotes")
    private List<String> riskNotes;

    /**
     * 합의 종목 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsensusStock {
        /**
         * 회사명
         */
        @JsonProperty("company")
        private String company;

        /**
         * 티커 (필수)
         */
        @JsonProperty("ticker")
        private String ticker;

        /**
         * 섹터
         */
        @JsonProperty("sector")
        private String sector;

        /**
         * 비중 (%) - 합계 100이어야 함
         */
        @JsonProperty("weightPercent")
        private Double weightPercent;

        /**
         * 합의 이유 (4명 철학이 동시에 반영)
         */
        @JsonProperty("consensusReason")
        private String consensusReason;

        /**
         * 장기 전망 (5~10년 관점)
         */
        @JsonProperty("longTermView")
        private String longTermView;
    }

    /**
     * 포트폴리오 검증
     * @return 검증 성공 여부
     */
    public boolean validate() {
        if (stocks == null || stocks.size() != 10) {
            return false;
        }

        // ticker 누락 체크
        for (ConsensusStock stock : stocks) {
            if (stock.getTicker() == null || stock.getTicker().trim().isEmpty()) {
                return false;
            }
            if (stock.getWeightPercent() == null || stock.getWeightPercent() <= 0) {
                return false;
            }
        }

        // 비중 합계 체크 (100 ± 1)
        double totalWeight = stocks.stream()
                .mapToDouble(ConsensusStock::getWeightPercent)
                .sum();

        return totalWeight >= 99.0 && totalWeight <= 101.0;
    }

    /**
     * 검증 실패 메시지
     */
    public String getValidationErrorMessage() {
        if (stocks == null) {
            return "stocks 필드가 null입니다.";
        }
        if (stocks.size() != 10) {
            return "종목 개수가 10개가 아닙니다 (현재: " + stocks.size() + "개)";
        }

        for (int i = 0; i < stocks.size(); i++) {
            ConsensusStock stock = stocks.get(i);
            if (stock.getTicker() == null || stock.getTicker().trim().isEmpty()) {
                return "종목 #" + (i + 1) + "의 ticker가 누락되었습니다.";
            }
            if (stock.getWeightPercent() == null || stock.getWeightPercent() <= 0) {
                return "종목 #" + (i + 1) + "의 weightPercent가 유효하지 않습니다.";
            }
        }

        double totalWeight = stocks.stream()
                .mapToDouble(ConsensusStock::getWeightPercent)
                .sum();

        if (totalWeight < 99.0 || totalWeight > 101.0) {
            return "비중 합계가 100이 아닙니다 (현재: " + totalWeight + "%)";
        }

        return null;
    }
}