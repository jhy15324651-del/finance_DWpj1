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

        // 3. Fallback 매핑 먼저 확인 (API 호출 최소화)
        String ticker = getFallbackTicker(cusip);

        // 4. Fallback에 없으면 OpenFIGI API 호출 (재시도 포함)
        if (ticker == null) {
            ticker = callOpenFigiWithRetry(cusip);
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
        // 주요 종목 CUSIP → Ticker 매핑 (Buffett 포트폴리오 + NASDAQ 100)
        Map<String, String> fallbackMap = new java.util.HashMap<>();

        // === NASDAQ 100: Technology & Semiconductors ===
        fallbackMap.put("037833100", "AAPL");   // Apple
        fallbackMap.put("594918104", "MSFT");   // Microsoft
        fallbackMap.put("02079K305", "GOOGL");  // Alphabet Class A
        fallbackMap.put("02079K107", "GOOG");   // Alphabet Class C
        fallbackMap.put("023135106", "AMZN");   // Amazon
        fallbackMap.put("30303M102", "META");   // Meta (Facebook)
        fallbackMap.put("67066G104", "NVDA");   // NVIDIA
        fallbackMap.put("11135F101", "AVGO");   // Broadcom
        fallbackMap.put("N07059210", "ASML");   // ASML Holding
        fallbackMap.put("007903107", "AMD");    // Advanced Micro Devices
        fallbackMap.put("624518782", "MU");     // Micron Technology
        fallbackMap.put("747525103", "QCOM");   // Qualcomm
        fallbackMap.put("458140100", "INTC");   // Intel
        fallbackMap.put("882508104", "TXN");    // Texas Instruments
        fallbackMap.put("031672100", "AMAT");   // Applied Materials
        fallbackMap.put("512807108", "LRCX");   // Lam Research
        fallbackMap.put("032654105", "ADI");    // Analog Devices
        fallbackMap.put("482480100", "KLAC");   // KLA Corp
        fallbackMap.put("G5876H103", "MRVL");   // Marvell Technology
        fallbackMap.put("N6596X101", "NXPI");   // NXP Semiconductors
        fallbackMap.put("595017104", "MCHP");   // Microchip Technology
        fallbackMap.put("G04280101", "ARM");    // ARM Holdings
        fallbackMap.put("68902V107", "ON");     // ON Semiconductor

        // === NASDAQ 100: Software & Cloud Services ===
        fallbackMap.put("68389X105", "ORCL");   // Oracle
        fallbackMap.put("79466L302", "CRM");    // Salesforce
        fallbackMap.put("00724F101", "ADBE");   // Adobe
        fallbackMap.put("N6596X103", "PANW");   // Palo Alto Networks
        fallbackMap.put("22788C105", "CRWD");   // CrowdStrike
        fallbackMap.put("871607107", "SNPS");   // Synopsys
        fallbackMap.put("127387108", "CDNS");   // Cadence Design Systems
        fallbackMap.put("461202103", "INTU");   // Intuit
        fallbackMap.put("98138H101", "WDAY");   // Workday
        fallbackMap.put("88338R105", "TEAM");   // Atlassian
        fallbackMap.put("23804L103", "DDOG");   // Datadog
        fallbackMap.put("98980G102", "ZS");     // Zscaler
        fallbackMap.put("G3224Y105", "FTNT");   // Fortinet
        fallbackMap.put("052769106", "ADSK");   // Autodesk
        fallbackMap.put("03852U106", "APP");    // AppLovin
        fallbackMap.put("69608A108", "PLTR");   // Palantir Technologies
        fallbackMap.put("81762P102", "SHOP");   // Shopify

        // === NASDAQ 100: Consumer & Retail ===
        fallbackMap.put("88160R101", "TSLA");   // Tesla
        fallbackMap.put("22160K105", "COST");   // Costco Wholesale
        fallbackMap.put("64110L106", "NFLX");   // Netflix
        fallbackMap.put("855244109", "SBUX");   // Starbucks
        fallbackMap.put("09857L108", "BKNG");   // Booking Holdings
        fallbackMap.put("019447AG3", "ABNB");   // Airbnb
        fallbackMap.put("P5876H103", "MELI");   // MercadoLibre
        fallbackMap.put("571903202", "MAR");    // Marriott International
        fallbackMap.put("67103H107", "ORLY");   // O'Reilly Automotive
        fallbackMap.put("778296103", "ROST");   // Ross Stores
        fallbackMap.put("550021109", "LULU");   // Lululemon Athletica
        fallbackMap.put("611740101", "MNST");   // Monster Beverage
        fallbackMap.put("191098102", "CDW");    // CDW Corporation

        // === NASDAQ 100: Communication Services ===
        fallbackMap.put("20030N101", "CMCSA");  // Comcast
        fallbackMap.put("17275R102", "CSCO");   // Cisco Systems
        fallbackMap.put("954550102", "WBD");    // Warner Bros Discovery

        // === NASDAQ 100: Healthcare & Biotech ===
        fallbackMap.put("46120E103", "ISRG");   // Intuitive Surgical
        fallbackMap.put("375558103", "GILD");   // Gilead Sciences
        fallbackMap.put("75886F107", "REGN");   // Regeneron Pharmaceuticals
        fallbackMap.put("92532F100", "VRTX");   // Vertex Pharmaceuticals
        fallbackMap.put("09062X103", "BIIB");   // Biogen
        fallbackMap.put("60770K107", "MRNA");   // Moderna
        fallbackMap.put("26442P104", "DXCM");   // DexCom
        fallbackMap.put("046353108", "AZN");    // AstraZeneca

        // === NASDAQ 100: Industrials & Manufacturing ===
        fallbackMap.put("438516106", "HON");    // Honeywell International
        fallbackMap.put("G5494J103", "LIN");    // Linde
        fallbackMap.put("172908105", "CTAS");   // Cintas
        fallbackMap.put("607059109", "ROP");    // Roper Technologies
        fallbackMap.put("126410109", "CSX");    // CSX Corporation
        fallbackMap.put("311900104", "FAST");   // Fastenal
        fallbackMap.put("003816107", "AXON");   // Axon Enterprise

        // === NASDAQ 100: Business Services ===
        fallbackMap.put("053015103", "ADP");    // Automatic Data Processing
        fallbackMap.put("704326107", "PAYX");   // Paychex

        // === NASDAQ 100: Consumer Staples ===
        fallbackMap.put("717081103", "PEP");    // PepsiCo
        fallbackMap.put("609207105", "MDLZ");   // Mondelez International
        fallbackMap.put("500754106", "KHC");    // Kraft Heinz
        fallbackMap.put("48203R104", "KDP");    // Keurig Dr Pepper
        fallbackMap.put("20030N200", "CCEP");   // Coca-Cola Europacific Partners

        // === NASDAQ 100: Energy & Utilities ===
        fallbackMap.put("12592G107", "CEG");    // Constellation Energy
        fallbackMap.put("025537101", "AEP");    // American Electric Power
        fallbackMap.put("30161N101", "EXC");    // Exelon
        fallbackMap.put("98389B100", "XEL");    // Xcel Energy
        fallbackMap.put("17875T107", "BKR");    // Baker Hughes
        fallbackMap.put("35671D857", "FANG");   // Diamondback Energy

        // === NASDAQ 100: International & Other ===
        fallbackMap.put("G7338J103", "PDD");    // PDD Holdings (Pinduoduo)
        fallbackMap.put("25809K105", "DASH");   // DoorDash
        fallbackMap.put("594972408", "MSTR");   // MicroStrategy
        fallbackMap.put("88339J105", "TTD");    // The Trade Desk
        fallbackMap.put("368730101", "GFS");    // GlobalFoundries
        fallbackMap.put("270138107", "EA");     // Electronic Arts
        fallbackMap.put("88302K102", "TTWO");   // Take-Two Interactive

        // === Berkshire Hathaway ===
        fallbackMap.put("084670702", "BRK.B");  // Berkshire Hathaway B
        fallbackMap.put("084670108", "BRK.A");  // Berkshire Hathaway A

        // === Financials (Buffett holdings) ===
        fallbackMap.put("46625H100", "JPM");    // JPMorgan
        fallbackMap.put("02005N100", "ALLY");   // Ally Financial
        fallbackMap.put("025816109", "AXP");    // American Express
        fallbackMap.put("060505104", "BAC");    // Bank of America
        fallbackMap.put("14040H105", "COF");    // Capital One
        fallbackMap.put("172967424", "C");      // Citigroup
        fallbackMap.put("693506107", "PNC");    // PNC Financial

        // === Other Consumer & Retail ===
        fallbackMap.put("191216100", "KO");     // Coca-Cola
        fallbackMap.put("501044101", "KR");     // Kroger
        fallbackMap.put("25754A201", "DPZ");    // Domino's Pizza

        // === Energy & Industrials ===
        fallbackMap.put("166764100", "CVX");    // Chevron
        fallbackMap.put("88579Y101", "BA");     // Boeing

        // === Communications ===
        fallbackMap.put("16119P108", "CHTR");   // Charter Communications
        fallbackMap.put("47233W109", "JEF");    // Jefferies Financial
        fallbackMap.put("87612E106", "T");      // AT&T
        fallbackMap.put("92343V104", "VZ");     // Verizon

        // === Healthcare & Pharma (Other) ===
        fallbackMap.put("904764109", "UNH");    // UnitedHealth
        fallbackMap.put("716973101", "PFE");    // Pfizer
        fallbackMap.put("23918K108", "DVA");    // DaVita

        // === International ===
        fallbackMap.put("25243Q205", "DEO");    // Diageo
        fallbackMap.put("46428Q103", "JD");     // JD.com

        // === Construction & Real Estate ===
        fallbackMap.put("526057104", "LEN");    // Lennar Corp Class A
        fallbackMap.put("526057302", "LEN.B");  // Lennar Corp Class B

        // === Media & Entertainment ===
        fallbackMap.put("047726302", "BATRK");  // Liberty Media
        fallbackMap.put("21036P108", "STZ");    // Constellation Brands

        // === Advertising ===
        fallbackMap.put("512816109", "LAMR");   // Lamar Advertising

        // === Liberty companies ===
        fallbackMap.put("531229722", "LLYVK");  // Liberty Media Liberty Live Class C
        fallbackMap.put("531229748", "LLYVA");  // Liberty Media Liberty Live Class A

        // === Tech & Other ===
        fallbackMap.put("459200101", "IBM");    // IBM
        fallbackMap.put("422806208", "HEI.A");  // HEICO Corp Class A

        // === ETFs ===
        fallbackMap.put("922908363", "VOO");    // Vanguard S&P 500 ETF
        fallbackMap.put("922908769", "VTI");    // Vanguard Total Stock Market ETF
        fallbackMap.put("464287655", "IVV");    // iShares Core S&P 500 ETF

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