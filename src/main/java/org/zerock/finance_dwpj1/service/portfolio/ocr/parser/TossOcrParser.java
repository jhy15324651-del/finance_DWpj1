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

    // 🔤 티커: 2~6자 대문자
    private static final Pattern TICKER_PATTERN =
            Pattern.compile("\\b([A-Z]{2,6})\\b");

    // 📈 보유 주식 수: "250주", "6.012704 주"
    private static final Pattern SHARES_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*주");

    // 📈 보유 주식 수 (fallback): "250 +1,874,884 ..." 처럼 '주'가 없을 때
    private static final Pattern SHARES_FALLBACK_PATTERN =
            Pattern.compile("^(\\d{1,4})\\b");
    // 💰 평가금액: "7,149,820 원"
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([\\d,.]+)\\s*원");

    // 💰 티커 줄 등에서 "7149,8202", "153,3622" 같은 금액 후보
    private static final Pattern INLINE_AMOUNT_PATTERN =
            Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+)");

    // 헤더/불필요 텍스트 필터 (필요하면 추가)
    private static final Pattern EXCLUDE_PATTERN =
            Pattern.compile(".*(?:합계|총|평가손익|수익률|현금|포트폴리오|잔고).*");

    // 한 티커 기준으로 아래 몇 줄까지 볼지
    private static final int SEARCH_RANGE = 8;

    @Override
    public List<PortfolioStock> parse(String ocrText) {
        log.info("토스 증권 OCR 텍스트 파싱 시작 (T2.5: 티커 + 평가금액 + 보유수 개선)");

        List<PortfolioStock> result = new ArrayList<>();

        if (ocrText == null || ocrText.trim().isEmpty()) {
            log.warn("OCR 텍스트가 비어있습니다.");
            return result;
        }

        String[] lines = ocrText.split("\\r?\\n");
        log.debug("총 {}줄 감지", lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (EXCLUDE_PATTERN.matcher(line).matches()) {
                log.debug("필터링된 줄(헤더/불필요 텍스트): {}", line);
                continue;
            }

            log.debug("파싱 중 [{}]: {}", i + 1, line);

            Matcher tickerMatcher = TICKER_PATTERN.matcher(line);
            if (!tickerMatcher.find()) {
                continue;
            }

            String ticker = tickerMatcher.group(1);
            log.debug("티커 후보 발견: {} (라인 {})", ticker, i + 1);

            Double shares = null;
            Long amount = null;

            // 1) 티커 줄에서 금액 후보 찾기 (TSLL 7149,8202!, TSLQ 153,3622 등)
            String tail = line.substring(tickerMatcher.end());
            Matcher inlineAmountMatcher = INLINE_AMOUNT_PATTERN.matcher(tail);
            if (inlineAmountMatcher.find()) {
                String rawAmount = inlineAmountMatcher.group(1);
                amount = parseAmount(rawAmount);
                log.debug("티커 줄에서 평가금액 후보 인식: {} ({} → {})", rawAmount, tail, amount);
            }

            // 2) 아래 줄들에서 보유수 / 평가금액 찾기
            for (int j = i + 1; j < lines.length && j <= i + SEARCH_RANGE; j++) {
                String nextLine = lines[j].trim();
                if (nextLine.isEmpty()) continue;

                // 다음 티커가 나오면 다른 종목 시작으로 보고 break
                Matcher nextTickerMatcher = TICKER_PATTERN.matcher(nextLine);
                if (nextTickerMatcher.find()) {
                    log.debug("다음 티커 발견(라인 {}: {}), 현재 종목 {}의 탐색 종료",
                            j + 1, nextLine, ticker);
                    break;
                }

                if (EXCLUDE_PATTERN.matcher(nextLine).matches()) {
                    log.debug("필터링된 줄(헤더/불필요 텍스트): {}", nextLine);
                    continue;
                }

                log.debug("  └ 하위 라인 [{}]: {}", j + 1, nextLine);

                // 2-1) 보유 주식 수 (정규식 1차: "97 주", "6.012704 주")
                if (shares == null) {
                    Matcher sharesMatcher = SHARES_PATTERN.matcher(nextLine);
                    if (sharesMatcher.find()) {
                        shares = parseShares(sharesMatcher.group(1));
                        log.debug("보유 주식 인식(주 포함): {}주 (라인 {})", shares, j + 1);
                    }
                }

                // 2-2) 보유 주식 수 (fallback: "250 +1,874,884 ...", "5 -54,679 ...")
                if (shares == null) {
                    Matcher sharesFallbackMatcher = SHARES_FALLBACK_PATTERN.matcher(nextLine);
                    if (sharesFallbackMatcher.find()) {
                        shares = parseShares(sharesFallbackMatcher.group(1));
                        log.debug("보유 주식 인식(fallback): {}주 (라인 {})", shares, j + 1);
                    }
                }

                // 2-3) 평가금액: "~ 원" 형태
                if (amount == null) {
                    Matcher amountMatcher = AMOUNT_PATTERN.matcher(nextLine);
                    if (amountMatcher.find()) {
                        String rawAmount = amountMatcher.group(1);
                        amount = parseAmount(rawAmount);
                        log.debug("평가금액 인식: {}원 (라인 {})", amount, j + 1);
                    }
                }

                if (shares != null && amount != null) {
                    // 둘 다 찾았으면 이 종목은 완료
                    break;
                }
            }

            if (shares != null && amount != null) {
                PortfolioStock stock = new PortfolioStock(ticker, shares, amount);
                result.add(stock);
                log.info("✅ 종목 인식: {} / {}주 / {}원", ticker, shares, amount);
            } else {
                log.warn("⚠ 티커 {} 에 대해 충분한 정보(보유수/평가금액)를 찾지 못했습니다. (shares={}, amount={})",
                        ticker, shares, amount);
            }
        }

        log.info("토스 증권 T2.5 파싱 완료: 총 {}개 종목 인식", result.size());
        return result;
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }

    // ===== 숫자 파싱 유틸 =====

    private Double parseShares(String raw) {
        String normalized = raw.replace(",", "").replace(" ", "");
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            log.warn("보유 주식 수 파싱 실패: {}", raw, e);
            return null;
        }
    }

    private Long parseAmount(String raw) {
        // "153,3622" 같은 이상한 패턴 정리:
        // 1) 마지막 콤마 뒤 숫자가 3자보다 많으면 3자리만 남기고 뒤는 버림
        String cleaned = raw;
        int lastComma = cleaned.lastIndexOf(',');
        if (lastComma != -1) {
            String after = cleaned.substring(lastComma + 1).replaceAll("\\D", "");
            if (after.length() > 3) {
                // 예: "153,3622" -> "153,362"
                cleaned = cleaned.substring(0, lastComma + 1) + after.substring(0, 3);
            }
        }

        String normalized = cleaned.replaceAll("[^0-9]", "");
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            log.warn("평가금액 파싱 실패: {} (정규화 후: {})", raw, normalized, e);
            return null;
        }
    }
}
