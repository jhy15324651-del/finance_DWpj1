package org.zerock.finance_dwpj1.controller.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.dto.portfolio.ContentRecommendationDTO;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioAnalysisRequest;
import org.zerock.finance_dwpj1.dto.portfolio.PortfolioAnalysisResponse;
import org.zerock.finance_dwpj1.service.portfolio.ContentRecommendationService;
import org.zerock.finance_dwpj1.service.portfolio.OcrService;
import org.zerock.finance_dwpj1.service.portfolio.PortfolioMatchingService;
import org.zerock.finance_dwpj1.service.portfolio.TickerMappingService;
import org.zerock.finance_dwpj1.service.portfolio.ocr.BrokerType;

import java.util.*;
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
    private final ContentRecommendationService contentRecommendationService;

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

            // 🔥 실제 콘텐츠 추천 생성 (더미 대신 DB 기반)
            Set<String> userTickers = request.getPortfolio().keySet();
            List<ContentRecommendationDTO> recommendedContentDTOs =
                    contentRecommendationService.recommendContents(userTickers, topMatches);

            // ContentRecommendationDTO → PortfolioAnalysisResponse.ContentRecommendation 변환
            List<PortfolioAnalysisResponse.ContentRecommendation> recommendedContents =
                    recommendedContentDTOs.stream()
                            .map(dto -> PortfolioAnalysisResponse.ContentRecommendation.builder()
                                    .contentId(dto.getId())
                                    .title(dto.getTitle())
                                    .category(dto.getCategoryLabel())
                                    .rating(dto.getRating())
                                    .thumbnailUrl(dto.getThumbnailUrl())
                                    .keyword(dto.getHashtags() != null && !dto.getHashtags().isEmpty()
                                            ? dto.getHashtags().get(0) : "")
                                    .build())
                            .collect(Collectors.toList());

            log.info("추천 콘텐츠 개수: {}", recommendedContents.size());

            // 응답 구성
            PortfolioAnalysisResponse response = PortfolioAnalysisResponse.builder()
                    .topMatches(investorMatches)
                    .recommendedContents(recommendedContents)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("포트폴리오 분석 중 오류", e);
            return ResponseEntity.status(500).build();
        }
    }

    // 더미 콘텐츠 생성 메서드 제거 (더 이상 사용하지 않음)
}
