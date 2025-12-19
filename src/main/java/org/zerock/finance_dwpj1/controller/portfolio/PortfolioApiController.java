package org.zerock.finance_dwpj1.controller.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.portfolio.ConsensusPortfolioResponse;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorComparisonDTO;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorComparisonRequest;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorSearchRequest;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorSearchResponse;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioRecommendationResponse;
import org.zerock.finance_dwpj1.service.portfolio.InvestorComparisonService;
import org.zerock.finance_dwpj1.service.portfolio.SEC13FService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Log4j2
public class PortfolioApiController {

    private final InvestorComparisonService investorComparisonService;
    private final SEC13FService sec13FService;

    @PostMapping("/compare")
    public List<InvestorComparisonDTO> compareInvestors(@RequestBody InvestorComparisonRequest request) {
        log.info("투자자 비교 요청: " + request.getInvestors());
        return investorComparisonService.compareInvestors(request.getInvestors());
    }

    @PostMapping("/search")
    public InvestorSearchResponse searchInvestor(@RequestBody InvestorSearchRequest request) {
        log.info("투자자 검색 요청: " + request.getName());
        return investorComparisonService.searchInvestor(request.getName());
    }

    @PostMapping("/recommend")
    public PortfolioRecommendationResponse generatePortfolioRecommendation(@RequestBody InvestorComparisonRequest request) {
        log.info("포트폴리오 추천 요청: " + request.getInvestors());
        return investorComparisonService.generatePortfolioRecommendation(request.getInvestors());
    }

    /**
     * 합의형 포트폴리오 생성 (도넛 차트용)
     * 4명 투자자가 회의 후 모두 동의한 합의 종목 10개 선정
     */
    @PostMapping("/consensus")
    public ResponseEntity<ConsensusPortfolioResponse> generateConsensusPortfolio(@RequestBody InvestorComparisonRequest request) {
        log.info("===== 합의형 포트폴리오 생성 요청 - 투자자: {} =====", request.getInvestors());

        try {
            ConsensusPortfolioResponse portfolio = investorComparisonService.generateConsensusPortfolio(request.getInvestors());
            log.info("===== 합의형 포트폴리오 생성 성공 - 종목 개수: {} =====", portfolio.getStocks().size());
            return ResponseEntity.ok(portfolio);

        } catch (RuntimeException e) {
            log.error("합의형 포트폴리오 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * 전체 투자자 13F 데이터 수집 (관리자용)
     */
    @PostMapping("/admin/fetch-13f-all")
    public Map<String, Object> fetchAll13FData() {
        log.info("=== 관리자 - 전체 13F 데이터 수집 시작 ===");
        Map<String, Object> response = new HashMap<>();

        if (sec13FService.isCollecting()) {
            response.put("success", false);
            response.put("message", "이미 13F 데이터 수집이 진행 중입니다.");
            return response;
        }

        // 🔹 비동기 시작 메서드 호출
        sec13FService.startAsyncCollection();

        response.put("success", true);
        response.put("message", "전체 투자자의 13F 데이터 수집을 백그라운드에서 시작했습니다.");
        return response;
    }

    /**
     * 특정 투자자 13F 데이터 수집 (관리자용)
     */
    @PostMapping("/admin/fetch-13f/{investorId}")
    public Map<String, Object> fetch13FDataForInvestor(@PathVariable String investorId) {
        log.info("=== 관리자 - {} 투자자 13F 데이터 수집 시작 ===", investorId);
        Map<String, Object> response = new HashMap<>();

        try {
            int count = sec13FService.fetch13FDataForInvestor(investorId);
            response.put("success", true);
            response.put("investor", investorId);
            response.put("count", count);
            response.put("message", count > 0
                ? count + "개 종목 데이터 수집 완료"
                : "이미 최신 데이터가 존재하거나 수집할 데이터가 없습니다.");
        } catch (Exception e) {
            log.error("{} 13F 데이터 수집 실패", investorId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "13F 데이터 수집 중 오류가 발생했습니다: " + e.getMessage());
        }

        return response;
    }
}