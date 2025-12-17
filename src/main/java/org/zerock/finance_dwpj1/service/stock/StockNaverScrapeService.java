package org.zerock.finance_dwpj1.service.stock;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.stock.StockSimpleMetricDTO;

import java.math.BigDecimal;

@Slf4j
@Service
public class StockNaverScrapeService {

    private static final String NAVER_URL =
            "https://finance.naver.com/item/main.naver?code=%s";

    public StockSimpleMetricDTO scrape(String ticker) {

        log.info("🔥 NAVER SCRAPE START : {}", ticker);

        try {
            Document doc = Jsoup.connect(String.format(NAVER_URL, ticker))
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            // ✅ 핵심: sub_section 마지막
            Elements sections = doc.select(".sub_section");
            if (sections.isEmpty()) {
                log.warn("❌ sub_section not found");
                return empty();
            }

            Element targetSection = sections.last();
            Elements rows = targetSection.select("table tbody tr");

            BigDecimal per = null;
            BigDecimal roe = null;
            BigDecimal dividend = null;
            BigDecimal dividendYield = null;
            BigDecimal marketCap = null;
            Long sharesOutstanding = null;

            for (Element row : rows) {
                String label = row.select("th").text();

                if (per == null && label.contains("PER")) {
                    per = parseBigDecimal(row);
                }

                if (roe == null && label.contains("ROE")) {
                    roe = parseBigDecimal(row);
                }

                if (dividend == null && label.contains("주당배당금")) {
                    dividend = parseBigDecimal(row);
                }


                if (dividendYield == null &&
                        (label.contains("시가배당률") || label.contains("배당수익률"))) {
                    dividendYield = parseBigDecimal(row);
                }


                if (sharesOutstanding == null && label.contains("상장주식수")) {
                    sharesOutstanding = parseLong(row);
                }
            }

            marketCap = scrapeMarketCap(doc);



            log.info("✅ SCRAPE RESULT per={}, roe={}, div={}, yield={}",
                    per, roe, dividend, dividendYield);

            return StockSimpleMetricDTO.builder()
                    .per(per)
                    .roe(roe)
                    .dividend(dividend)
                    .dividendYield(dividendYield)
                    .marketCap(marketCap)
                    .sharesOutstanding(sharesOutstanding)
                    .marketCapSource("naver")
                    .build();

        } catch (Exception e) {
            log.error("❌ NAVER SCRAPE FAIL", e);
            return empty();
        }
    }





    // ========================
    // 파싱 유틸
    // ========================

    private BigDecimal parseBigDecimal(Element row) {
        try {
            Elements tds = row.select("td");

            // 연간 기준: 2022, 2023, 2024 → index 2
            if (tds.size() <= 2) return null;

            String text = tds.get(2).text()
                    .replace(",", "")
                    .replace("%", "")
                    .replace("원", "")
                    .trim();

            if (text.isEmpty() || text.equals("--")) return null;
            return new BigDecimal(text);

        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(Element row) {
        try {
            Elements tds = row.select("td");
            if (tds.size() <= 2) return null;

            String text = tds.get(2).text()
                    .replace(",", "")
                    .replace("주", "")
                    .trim();

            if (text.isEmpty() || text.equals("--")) return null;
            return Long.parseLong(text);

        } catch (Exception e) {
            return null;
        }
    }


    private BigDecimal scrapeMarketCap(Document doc) {
        try {
            Element th = doc.selectFirst("th:contains(시가총액)");
            if (th == null) {
                log.warn("❌ 시가총액 th not found");
                return null;
            }

            Element td = th.nextElementSibling();
            if (td == null) return null;

            String text = td.text()
                    .replace(",", "")
                    .replace("억원", "")
                    .trim();

            if (text.isEmpty() || text.equals("--")) return null;

            // 이미 '억' 단위
            return new BigDecimal(text);

        } catch (Exception e) {
            return null;
        }
    }


    private StockSimpleMetricDTO empty() {
        return StockSimpleMetricDTO.builder().build();
    }
}