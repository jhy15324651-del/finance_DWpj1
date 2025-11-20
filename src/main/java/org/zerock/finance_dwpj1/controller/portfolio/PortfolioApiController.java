package org.zerock.finance_dwpj1.controller.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorComparisonDTO;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorComparisonRequest;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorSearchRequest;
import org.zerock.finance_dwpj1.dto.portfolio.InvestorSearchResponse;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioRecommendationResponse;
import org.zerock.finance_dwpj1.service.portfolio.InvestorComparisonService;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Log4j2
public class PortfolioApiController {

    private final InvestorComparisonService investorComparisonService;

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
}