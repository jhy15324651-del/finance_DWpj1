package org.zerock.finance_dwpj1.controller.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioAnalysisRequest;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioAnalysisResponse;
import org.zerock.finance_dwpj1.service.portfolio.OcrService;
import org.zerock.finance_dwpj1.service.portfolio.PortfolioMatchingService;
import org.zerock.finance_dwpj1.service.portfolio.TickerMappingService;
import org.zerock.finance_dwpj1.service.portfolio.ocr.BrokerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 포트폴리오 분석 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
public class PortfolioAnalyzerController {

    private final OcrService ocrService;
    private final PortfolioMatchingService matchingService;
    private final TickerMappingService tickerMappingService;

    /**
     * OCR로 이미지에서 포트폴리오 추출
     * @param image 포트폴리오 이미지 파일
     * @param broker 증권사 타입 (선택사항, 기본값: DEFAULT)
     *               가능한 값: TOSS, DEFAULT
     */
    @PostMapping("/extract-from-image")
    public ResponseEntity<Map<String, Object>> extractFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "broker", required = false, defaultValue = "DEFAULT") String broker) {

        log.info("이미지 OCR 요청: {} ({} bytes), 증권사: {}",
                image.getOriginalFilename(), image.getSize(), broker);

        try {
            // 증권사 타입 파싱
            BrokerType brokerType = BrokerType.fromString(broker);
            log.info("파싱된 증권사 타입: {}", brokerType);

            // OCR 실행 (증권사별 전처리 및 파싱 적용)
            List<OcrService.PortfolioStock> stocks = ocrService.extractPortfolioFromImage(image, brokerType);

            log.info("=== OCR 원본 결과 ({} 종목) ===", stocks.size());
            stocks.forEach(stock -> log.info("  - {}", stock));

            // 🔥 후처리: 한국어 종목명 → 영어 티커로 치환
            List<Map<String, Object>> processedStocks = stocks.stream()
                    .map(stock -> {
                        String originalTicker = stock.getTicker();
                        String mappedTicker = tickerMappingService.mapToTicker(originalTicker);

                        Map<String, Object> stockData = new HashMap<>();
                        stockData.put("ticker", mappedTicker);
                        stockData.put("originalTicker", originalTicker); // 원본도 함께 반환
                        stockData.put("amount", stock.getAmount());
                        stockData.put("shares", stock.getShares());
                        stockData.put("isMapped", !originalTicker.equals(mappedTicker));

                        if (!originalTicker.equals(mappedTicker)) {
                            log.info("  ✓ 티커 치환: '{}' → '{}'", originalTicker, mappedTicker);
                        }

                        return stockData;
                    })
                    .collect(Collectors.toList());

            log.info("=== 티커 치환 완료 ({} 종목) ===", processedStocks.size());

            // 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("broker", brokerType.name());
            response.put("brokerName", brokerType.getKoreanName());
            response.put("stocks", processedStocks); // 치환된 데이터 반환
            response.put("count", processedStocks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("OCR 처리 중 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 포트폴리오 분석 (투자대가 매칭)
     */
    @PostMapping("/analyze")
    public ResponseEntity<PortfolioAnalysisResponse> analyzePortfolio(
            @RequestBody PortfolioAnalysisRequest request) {

        log.info("포트폴리오 분석 요청: {} 종목", request.getPortfolio().size());

        try {
            // TOP 3 투자대가 매칭
            List<PortfolioMatchingService.MatchResult> topMatches =
                    matchingService.findTopMatches(request.getPortfolio());

            // MatchResult를 DTO로 변환 (순위 포함)
            List<PortfolioAnalysisResponse.InvestorMatch> investorMatches =
                    IntStream.range(0, topMatches.size())
                            .mapToObj(i -> PortfolioAnalysisResponse.fromMatchResult(
                                    topMatches.get(i), i + 1))
                            .collect(Collectors.toList());

            // 더미 콘텐츠 추천 생성
            List<PortfolioAnalysisResponse.ContentRecommendation> dummyContents =
                    generateDummyContentRecommendations(topMatches);

            // 응답 구성
            PortfolioAnalysisResponse response = PortfolioAnalysisResponse.builder()
                    .topMatches(investorMatches)
                    .recommendedContents(dummyContents)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("포트폴리오 분석 중 오류", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 더미 콘텐츠 추천 데이터 생성
     * (실제로는 콘텐츠 DB에서 조회)
     */
    private List<PortfolioAnalysisResponse.ContentRecommendation> generateDummyContentRecommendations(
            List<PortfolioMatchingService.MatchResult> matches) {

        List<PortfolioAnalysisResponse.ContentRecommendation> contents = new ArrayList<>();

        // TOP 1 투자대가 기반 콘텐츠
        if (!matches.isEmpty()) {
            String topInvestor = matches.get(0).getInvestorName();

            contents.add(PortfolioAnalysisResponse.ContentRecommendation.builder()
                    .contentId(1L)
                    .title(topInvestor + "의 투자 철학과 성공 비결")
                    .category("투자 전략")
                    .rating(4.8)
                    .thumbnailUrl("/images/content/dummy1.jpg")
                    .keyword(topInvestor)
                    .build());

            contents.add(PortfolioAnalysisResponse.ContentRecommendation.builder()
                    .contentId(2L)
                    .title("가치투자 vs 성장주 투자: 어떤 전략이 나에게 맞을까?")
                    .category("투자 입문")
                    .rating(4.7)
                    .thumbnailUrl("/images/content/dummy2.jpg")
                    .keyword("투자 전략")
                    .build());

            contents.add(PortfolioAnalysisResponse.ContentRecommendation.builder()
                    .contentId(3L)
                    .title("포트폴리오 분산투자의 중요성")
                    .category("리스크 관리")
                    .rating(4.6)
                    .thumbnailUrl("/images/content/dummy3.jpg")
                    .keyword("분산투자")
                    .build());

            contents.add(PortfolioAnalysisResponse.ContentRecommendation.builder()
                    .contentId(4L)
                    .title("초보 투자자가 피해야 할 5가지 실수")
                    .category("투자 팁")
                    .rating(4.9)
                    .thumbnailUrl("/images/content/dummy4.jpg")
                    .keyword("투자 실수")
                    .build());

            contents.add(PortfolioAnalysisResponse.ContentRecommendation.builder()
                    .contentId(5L)
                    .title("2025년 주목해야 할 산업과 종목")
                    .category("시장 분석")
                    .rating(4.5)
                    .thumbnailUrl("/images/content/dummy5.jpg")
                    .keyword("시장 전망")
                    .build());
        }

        return contents;
    }
}
