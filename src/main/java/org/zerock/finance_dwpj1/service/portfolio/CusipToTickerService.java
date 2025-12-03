package org.zerock.finance_dwpj1.service.portfolio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 */
@Service
@Slf4j
public class CusipToTickerService {

    private static final String OPENFIGI_API_URL = "https://api.openfigi.com/v3/mapping";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // 캐시 (같은 CUSIP 반복 조회 방지)
    private final Map<String, String> cusipCache = new HashMap<>();

    /**
     * CUSIP를 Ticker로 변환
     * @param cusip CUSIP 코드 (9자리)
     * @return Ticker 심볼 (예: AAPL) 또는 null
     */
    public String convertCusipToTicker(String cusip) {
        if (cusip == null || cusip.length() != 9) {
            return null;
        }

        // 캐시 확인
        if (cusipCache.containsKey(cusip)) {
            return cusipCache.get(cusip);
        }

        try {
            // OpenFIGI API 요청 JSON 구성
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
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("OpenFIGI API 호출 실패: {} - {}", response.code(), cusip);
                    return getFallbackTicker(cusip); // Fallback
                }

                String responseJson = response.body().string();
                JsonArray root = JsonParser.parseString(responseJson).getAsJsonArray();

                if (root.size() > 0) {
                    JsonObject firstResult = root.get(0).getAsJsonObject();

                    if (firstResult.has("data")) {
                        JsonArray dataArray = firstResult.getAsJsonArray("data");
                        if (dataArray.size() > 0) {
                            JsonObject data = dataArray.get(0).getAsJsonObject();
                            String ticker = data.get("ticker").getAsString();

                            // 캐시에 저장
                            cusipCache.put(cusip, ticker);
                            log.info("CUSIP {} → Ticker {}", cusip, ticker);
                            return ticker;
                        }
                    }
                }
            }

            // API 실패 시 Fallback
            return getFallbackTicker(cusip);

        } catch (Exception e) {
            log.error("CUSIP 변환 중 오류: {}", cusip, e);
            return getFallbackTicker(cusip);
        }
    }

    /**
     * API 실패 시 하드코딩된 주요 종목 매핑 사용
     */
    private String getFallbackTicker(String cusip) {
        // 주요 종목 CUSIP → Ticker 매핑 (TOP 100)
        Map<String, String> fallbackMap = Map.ofEntries(
            Map.entry("037833100", "AAPL"),  // Apple
            Map.entry("594918104", "MSFT"),  // Microsoft
            Map.entry("02079K305", "GOOGL"), // Alphabet Class A
            Map.entry("02079K107", "GOOG"),  // Alphabet Class C
            Map.entry("023135106", "AMZN"),  // Amazon
            Map.entry("88160R101", "TSLA"),  // Tesla
            Map.entry("30303M102", "META"),  // Meta (Facebook)
            Map.entry("67066G104", "NVDA"),  // NVIDIA
            Map.entry("084670702", "BRK.B"), // Berkshire Hathaway B
            Map.entry("084670108", "BRK.A"), // Berkshire Hathaway A
            Map.entry("46625H100", "JPM"),   // JPMorgan
            Map.entry("037411105", "APP"),   // Applovin
            Map.entry("172967424", "C"),     // Citigroup
            Map.entry("166764100", "CHTR"),  // Charter Communications
            Map.entry("169905106", "CHD"),   // Church & Dwight
            Map.entry("747525103", "VOO"),   // Vanguard S&P 500 ETF
            Map.entry("922908769", "VTI"),   // Vanguard Total Stock Market ETF
            Map.entry("464287655", "IVV"),   // iShares Core S&P 500 ETF
            Map.entry("693506107", "PNC"),   // PNC Financial
            Map.entry("716973101", "PFE"),   // Pfizer
            Map.entry("459200101", "IBM"),   // IBM
            Map.entry("46428Q103", "JD"),    // JD.com
            Map.entry("88579Y101", "BA"),    // Boeing
            Map.entry("02376R102", "AMD"),   // AMD
            Map.entry("654106103", "NFLX"),  // Netflix
            Map.entry("717081103", "PEP"),   // PepsiCo
            Map.entry("191216100", "KO"),    // Coca-Cola
            Map.entry("87612E106", "T"),     // AT&T
            Map.entry("92343V104", "VZ"),    // Verizon
            Map.entry("904764109", "UNH")    // UnitedHealth
        );

        String ticker = fallbackMap.get(cusip);
        if (ticker != null) {
            cusipCache.put(cusip, ticker);
            log.info("CUSIP {} → Ticker {} (Fallback 매핑)", cusip, ticker);
        } else {
            log.warn("CUSIP {}에 대한 Ticker를 찾을 수 없습니다", cusip);
        }

        return ticker;
    }

    /**
     * 캐시 초기화
     */
    public void clearCache() {
        cusipCache.clear();
        log.info("CUSIP 캐시 초기화됨");
    }
}
