package org.zerock.finance_dwpj1.service.portfolio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CUSIP 코드를 Ticker 심볼로 변환하는 서비스
 * OpenFIGI API 사용 (무료, 분당 25회 제한)
 *
 * 특징:
 * - 메모리 캐시로 중복 API 호출 방지
 * - 429 에러 시 지수 백오프로 재시도 (최대 3회)
 * - HTML 응답 방어 로직
 * - Fallback 매핑 지원
 */
@Service
@Slf4j
public class CusipToTickerService {

    private static final String OPENFIGI_API_URL = "https://api.openfigi.com/v3/mapping";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // 재시도 설정
    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 2000, 4000}; // 1초, 2초, 4초

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // 메모리 캐시 (같은 CUSIP 반복 조회 방지)
    private final Map<String, String> cusipCache = new HashMap<>();

    /**
     * CUSIP를 Ticker로 변환
     *
     * @param cusip CUSIP 코드 (9자리)
     * @return Ticker 심볼 (예: AAPL) 또는 null (변환 실패/상장폐지)
     */
    public String convertCusipToTicker(String cusip) {
        // 1. 입력 검증
        if (cusip == null || cusip.length() != 9) {
            log.warn("❌ 잘못된 CUSIP 형식: {}", cusip);
            return null;
        }

        // 2. 캐시 확인 (중복 API 호출 방지)
        if (cusipCache.containsKey(cusip)) {
            String cachedTicker = cusipCache.get(cusip);
            log.debug("💾 캐시에서 조회: {} → {}", cusip, cachedTicker);
            return cachedTicker;
        }

        // 3. OpenFIGI API 호출 (재시도 포함)
        String ticker = callOpenFigiWithRetry(cusip);

        // 4. API 실패 시 Fallback 매핑 시도
        if (ticker == null) {
            ticker = getFallbackTicker(cusip);
        }

        // 5. 결과를 캐시에 저장 (null도 저장하여 반복 조회 방지)
        cusipCache.put(cusip, ticker);

        return ticker;
    }

    /**
     * OpenFIGI API 호출 (재시도 및 백오프 포함)
     */
    private String callOpenFigiWithRetry(String cusip) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // 재시도인 경우 대기
                if (attempt > 0) {
                    long delay = RETRY_DELAYS_MS[attempt - 1];
                    log.info("⏳ {}ms 대기 후 재시도 ({}/{}): CUSIP {}",
                            delay, attempt + 1, MAX_RETRIES, cusip);
                    Thread.sleep(delay);
                }

                // API 요청 JSON 구성
                String requestJson = String.format(
                    "[{\"idType\":\"ID_CUSIP\",\"idValue\":\"%s\",\"exchCode\":\"US\"}]",
                    cusip
                );

                RequestBody body = RequestBody.create(requestJson, JSON);
                Request request = new Request.Builder()
                        .url(OPENFIGI_API_URL)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int statusCode = response.code();

                    // 429 (Too Many Requests) - 재시도 필요
                    if (statusCode == 429) {
                        log.warn("⚠️ Rate Limit 초과 (429): CUSIP {} - 재시도 {}/{}",
                                cusip, attempt + 1, MAX_RETRIES);
                        continue; // 다음 시도로
                    }

                    // 기타 HTTP 에러
                    if (!response.isSuccessful() || response.body() == null) {
                        log.warn("❌ OpenFIGI API 호출 실패: {} - CUSIP {}", statusCode, cusip);
                        return null;
                    }

                    // 응답 본문 읽기
                    String responseBody = response.body().string();

                    // HTML 응답 방어 로직 (에러 페이지 감지)
                    if (responseBody.trim().startsWith("<")) {
                        log.warn("❌ HTML 응답 감지 (API 에러): CUSIP {}", cusip);
                        return null;
                    }

                    // JSON 파싱 및 Ticker 추출
                    return parseTickerFromResponse(responseBody, cusip);

                } catch (Exception e) {
                    log.error("❌ API 호출 중 예외 발생: CUSIP {} - {}", cusip, e.getMessage());
                    if (attempt == MAX_RETRIES - 1) {
                        return null; // 마지막 시도에서도 실패
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("❌ 재시도 대기 중 인터럽트: CUSIP {}", cusip);
                return null;
            }
        }

        // 모든 재시도 실패
        log.warn("❌ 최대 재시도 횟수 초과: CUSIP {} - API 호출 포기", cusip);
        return null;
    }

    /**
     * OpenFIGI API 응답에서 Ticker 추출
     */
    private String parseTickerFromResponse(String responseJson, String cusip) {
        try {
            JsonArray root = JsonParser.parseString(responseJson).getAsJsonArray();

            if (root.size() > 0) {
                JsonObject firstResult = root.get(0).getAsJsonObject();

                // "data" 필드 확인
                if (firstResult.has("data")) {
                    JsonArray dataArray = firstResult.getAsJsonArray("data");
                    if (dataArray.size() > 0) {
                        JsonObject data = dataArray.get(0).getAsJsonObject();

                        // Ticker 추출
                        if (data.has("ticker")) {
                            String ticker = data.get("ticker").getAsString();
                            log.info("✅ CUSIP {} → Ticker {} (OpenFIGI)", cusip, ticker);
                            return ticker;
                        }
                    }
                }

                // "error" 필드 확인
                if (firstResult.has("error")) {
                    String error = firstResult.get("error").getAsString();
                    log.warn("⚠️ OpenFIGI 오류 응답: CUSIP {} - {}", cusip, error);
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ JSON 파싱 실패: CUSIP {} - {}", cusip, e.getMessage());
            return null;
        }
    }

    /**
     * API 실패 시 하드코딩된 주요 종목 매핑 사용 (Fallback)
     */
    private String getFallbackTicker(String cusip) {
        // 주요 종목 CUSIP → Ticker 매핑 (Buffett 포트폴리오 중심)
        Map<String, String> fallbackMap = Map.ofEntries(
            // Tech Giants
            Map.entry("037833100", "AAPL"),  // Apple
            Map.entry("594918104", "MSFT"),  // Microsoft
            Map.entry("02079K305", "GOOGL"), // Alphabet Class A
            Map.entry("02079K107", "GOOG"),  // Alphabet Class C
            Map.entry("023135106", "AMZN"),  // Amazon
            Map.entry("88160R101", "TSLA"),  // Tesla
            Map.entry("30303M102", "META"),  // Meta (Facebook)
            Map.entry("67066G104", "NVDA"),  // NVIDIA

            // Berkshire Hathaway
            Map.entry("084670702", "BRK.B"), // Berkshire Hathaway B
            Map.entry("084670108", "BRK.A"), // Berkshire Hathaway A

            // Financials (Buffett holdings)
            Map.entry("46625H100", "JPM"),   // JPMorgan
            Map.entry("02005N100", "ALLY"),  // Ally Financial
            Map.entry("025816109", "AXP"),   // American Express
            Map.entry("060505104", "BAC"),   // Bank of America
            Map.entry("14040H105", "COF"),   // Capital One
            Map.entry("172967424", "C"),     // Citigroup
            Map.entry("693506107", "PNC"),   // PNC Financial

            // Consumer & Retail
            Map.entry("191216100", "KO"),    // Coca-Cola
            Map.entry("500754106", "KHC"),   // Kraft Heinz
            Map.entry("501044101", "KR"),    // Kroger
            Map.entry("717081103", "PEP"),   // PepsiCo
            Map.entry("25754A201", "DPZ"),   // Domino's Pizza

            // Energy & Industrials
            Map.entry("166764100", "CVX"),   // Chevron
            Map.entry("88579Y101", "BA"),    // Boeing

            // Communications
            Map.entry("16119P108", "CHTR"),  // Charter Communications
            Map.entry("47233W109", "JEF"),   // Jefferies Financial
            Map.entry("87612E106", "T"),     // AT&T
            Map.entry("92343V104", "VZ"),    // Verizon

            // Healthcare & Pharma
            Map.entry("904764109", "UNH"),   // UnitedHealth
            Map.entry("716973101", "PFE"),   // Pfizer
            Map.entry("23918K108", "DVA"),   // DaVita

            // International
            Map.entry("25243Q205", "DEO"),   // Diageo
            Map.entry("46428Q103", "JD"),    // JD.com

            // Construction & Real Estate
            Map.entry("526057104", "LEN"),   // Lennar Corp Class A
            Map.entry("526057302", "LEN.B"), // Lennar Corp Class B

            // Media & Entertainment
            Map.entry("047726302", "BATRK"), // Liberty Media
            Map.entry("21036P108", "STZ"),   // Constellation Brands

            // Advertising
            Map.entry("512816109", "LAMR"), // Lamar Advertising

            // Liberty companies
            Map.entry("531229722", "LLYVK"), // Liberty Media Liberty Live Class C
            Map.entry("531229748", "LLYVA"), // Liberty Media Liberty Live Class A

            // Tech & Other
            Map.entry("459200101", "IBM"),   // IBM
            Map.entry("02376R102", "AMD"),   // AMD
            Map.entry("654106103", "NFLX"),  // Netflix
            Map.entry("422806208", "HEI.A"), // HEICO Corp Class A

            // ETFs
            Map.entry("747525103", "VOO"),   // Vanguard S&P 500 ETF
            Map.entry("922908769", "VTI"),   // Vanguard Total Stock Market ETF
            Map.entry("464287655", "IVV")    // iShares Core S&P 500 ETF
        );

        String ticker = fallbackMap.get(cusip);
        if (ticker != null) {
            log.info("✅ CUSIP {} → Ticker {} (Fallback 매핑)", cusip, ticker);
            return ticker;
        } else {
            log.warn("⚠️ CUSIP {}에 대한 Ticker를 찾을 수 없음 (API 실패 + Fallback 없음)", cusip);
            return null;
        }
    }

    /**
     * 캐시 초기화 (테스트/디버깅용)
     */
    public void clearCache() {
        int size = cusipCache.size();
        cusipCache.clear();
        log.info("🗑️ CUSIP 캐시 초기화 완료 ({}개 항목 삭제)", size);
    }

    /**
     * 현재 캐시 크기 조회
     */
    public int getCacheSize() {
        return cusipCache.size();
    }
}