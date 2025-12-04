package org.zerock.finance_dwpj1.service.portfolio.ocr.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zerock.finance_dwpj1.service.portfolio.OcrService.PortfolioStock;
import org.zerock.finance_dwpj1.service.portfolio.ocr.BrokerType;
import org.zerock.finance_dwpj1.service.portfolio.ocr.OcrParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@Slf4j
public class TossOcrParser implements OcrParser {

    // 티커 패턴: 2-5자 대문자 (AAPL, TSLA, GOOGL 등)
    private static final Pattern TICKER_PATTERN = Pattern.compile("\\b([A-Z]{2,5})\\b");

    // 비중 패턴: 숫자 + % (30%, 25.5%, 0.5% 등)
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    // 한글 종목명 패턴 (선택사항)
    private static final Pattern KOREAN_NAME_PATTERN = Pattern.compile("([가-힣]{2,})");

    // 필터링할 불필요한 텍스트 (헤더, 라벨 등)
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile(
            ".*(?:합계|총|평가금액|보유|종목|자산|현금|Total|Asset|Cash|Portfolio).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<PortfolioStock> parse(String ocrText) {
        log.info("토스 증권 OCR 텍스트 파싱 시작");
        log.debug("원본 텍스트:\n{}", ocrText);

        List<PortfolioStock> stocks = new ArrayList<>();

        if (ocrText == null || ocrText.trim().isEmpty()) {
            log.warn("OCR 텍스트가 비어있습니다.");
            return stocks;
        }

        // 줄 단위로 분리
        String[] lines = ocrText.split("\\n");
        log.debug("총 {}줄 감지", lines.length);

        // 여러 줄에 걸쳐 있는 정보를 저장할 임시 변수
        String currentTicker = null;
        Double currentWeight = null;
        String currentKoreanName = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 빈 줄 스킵
            if (line.isEmpty()) {
                continue;
            }

            // 불필요한 텍스트 필터링
            if (EXCLUDE_PATTERN.matcher(line).matches()) {
                log.debug("필터링된 줄: {}", line);
                continue;
            }

            log.debug("파싱 중 [{}]: {}", i + 1, line);

            // 한 줄에 티커와 비중이 모두 있는 경우
            Matcher tickerMatcher = TICKER_PATTERN.matcher(line);
            Matcher weightMatcher = WEIGHT_PATTERN.matcher(line);

            boolean foundTicker = tickerMatcher.find();
            boolean foundWeight = weightMatcher.find();

            if (foundTicker && foundWeight) {
                // 한 줄에 모든 정보가 있는 경우
                String ticker = tickerMatcher.group(1);
                Double weight = Double.parseDouble(weightMatcher.group(1));

                // 한글 종목명도 추출 시도
                Matcher koreanMatcher = KOREAN_NAME_PATTERN.matcher(line);
                String koreanName = koreanMatcher.find() ? koreanMatcher.group(1) : null;

                PortfolioStock stock = new PortfolioStock(ticker, weight);
                stocks.add(stock);

                log.info("종목 인식 (한 줄): {} - {}% {}",
                        ticker, weight, koreanName != null ? "(" + koreanName + ")" : "");

                // 임시 변수 초기화
                currentTicker = null;
                currentWeight = null;
                currentKoreanName = null;

            } else if (foundTicker) {
                // 티커만 있는 경우 - 다음 줄에 비중이 올 가능성
                if (currentTicker != null && currentWeight != null) {
                    // 이전에 저장된 정보가 있으면 먼저 추가
                    PortfolioStock stock = new PortfolioStock(currentTicker, currentWeight);
                    stocks.add(stock);
                    log.info("종목 인식 (여러 줄): {} - {}%", currentTicker, currentWeight);
                }

                currentTicker = tickerMatcher.group(1);
                currentWeight = null;
                log.debug("티커 발견: {}", currentTicker);

                // 한글 종목명도 함께 있는지 확인
                Matcher koreanMatcher = KOREAN_NAME_PATTERN.matcher(line);
                currentKoreanName = koreanMatcher.find() ? koreanMatcher.group(1) : null;

            } else if (foundWeight && currentTicker != null) {
                // 비중만 있고 이전에 티커가 있었던 경우
                currentWeight = Double.parseDouble(weightMatcher.group(1));

                PortfolioStock stock = new PortfolioStock(currentTicker, currentWeight);
                stocks.add(stock);

                log.info("종목 인식 (여러 줄): {} - {}% {}",
                        currentTicker, currentWeight,
                        currentKoreanName != null ? "(" + currentKoreanName + ")" : "");

                // 임시 변수 초기화
                currentTicker = null;
                currentWeight = null;
                currentKoreanName = null;

            } else {
                // 티커도 비중도 없는 경우 - 한글 종목명일 가능성
                Matcher koreanMatcher = KOREAN_NAME_PATTERN.matcher(line);
                if (koreanMatcher.find()) {
                    currentKoreanName = koreanMatcher.group(1);
                    log.debug("한글 종목명 발견: {}", currentKoreanName);
                }
            }
        }

        // 마지막 줄까지 처리 후 남은 데이터가 있으면 추가
        if (currentTicker != null && currentWeight != null) {
            PortfolioStock stock = new PortfolioStock(currentTicker, currentWeight);
            stocks.add(stock);
            log.info("종목 인식 (마지막): {} - {}%", currentTicker, currentWeight);
        }

        log.info("토스 증권 파싱 완료: 총 {}개 종목", stocks.size());
        return stocks;
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }
}