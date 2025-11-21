package org.zerock.finance_dwpj1.service.stock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.dto.stock.StockCandleDTO;
import org.zerock.finance_dwpj1.dto.stock.StockInfoDTO;
import org.zerock.finance_dwpj1.entity.stock.StockCandleCache;
import org.zerock.finance_dwpj1.entity.stock.StockQuoteCache;
import org.zerock.finance_dwpj1.repository.stock.StockCandleCacheRepository;
import org.zerock.finance_dwpj1.repository.stock.StockQuoteCacheRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Yahoo Finance v8 API 직접 호출 + DB 캐싱 서비스
 * API 호출을 최소화하고 DB에 캐싱하여 성능 최적화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceStockService implements StockService {

    private final StockQuoteCacheRepository quoteCacheRepository;
    private final StockCandleCacheRepository candleCacheRepository;

    // 캐시 TTL 설정 (분)
    @Value("${stock.cache.quote-ttl:15}")
    private int quoteTtlMinutes;

    @Value("${stock.cache.candle-daily-ttl:360}")
    private int candleDailyTtlMinutes;

    @Value("${stock.cache.candle-weekly-ttl:1440}")
    private int candleWeeklyTtlMinutes;

    private static final String YAHOO_CHART_API = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 티커를 Yahoo Finance 형식으로 변환
     * 한국 종목(6자리 숫자)은 .KS(KOSPI) suffix 추가
     */
    private String convertToYahooTicker(String ticker) {
        // 이미 suffix가 있으면 그대로 반환
        if (ticker.contains(".")) {
            return ticker;
        }
        // 6자리 숫자면 한국 종목으로 간주하고 .KS 추가 (KOSPI)
        if (ticker.matches("^\\d{6}$")) {
            return ticker + ".KS";
        }
        return ticker;
    }

    /**
     * 종목 정보 조회 (캐시 우선)
     */
    @Override
    public StockInfoDTO getStockInfo(String ticker) {
        ticker = ticker.toUpperCase();
        log.info("종목 정보 조회 요청: {}", ticker);

        // 1. 캐시 확인
        LocalDateTime cacheThreshold = LocalDateTime.now().minusMinutes(quoteTtlMinutes);
        Optional<StockQuoteCache> cachedQuote = quoteCacheRepository
                .findByTickerAndUpdatedAtAfter(ticker, cacheThreshold);

        if (cachedQuote.isPresent()) {
            log.info("캐시에서 조회: {} (캐시 시간: {})", ticker, cachedQuote.get().getUpdatedAt());
            return convertToDTO(cachedQuote.get());
        }

        // 2. API 호출
        log.info("Yahoo Finance API 호출: {}", ticker);
        StockInfoDTO stockInfo = fetchStockInfoFromApi(ticker);

        if (stockInfo != null) {
            // 3. 캐시 저장
            saveQuoteCache(ticker, stockInfo);
        }

        return stockInfo;
    }

    /**
     * 캔들 데이터 조회 (캐시 우선)
     */
    @Override
    @Transactional
    public List<StockCandleDTO> getCandleData(String ticker, String timeframe, int count) {
        ticker = ticker.toUpperCase();
        timeframe = timeframe.toUpperCase();
        log.info("캔들 데이터 조회 요청: ticker={}, timeframe={}, count={}", ticker, timeframe, count);

        // 1. 캐시 TTL 결정
        int ttlMinutes = getCandleTtl(timeframe);
        LocalDateTime cacheThreshold = LocalDateTime.now().minusMinutes(ttlMinutes);

        // 2. 캐시 확인
        List<StockCandleCache> cachedCandles = candleCacheRepository
                .findValidCache(ticker, timeframe, cacheThreshold);

        if (!cachedCandles.isEmpty() && cachedCandles.size() >= count * 0.8) {
            log.info("캐시에서 조회: {} {}개 캔들", ticker, cachedCandles.size());
            return convertCandlesToDTO(cachedCandles, count);
        }

        // 3. API 호출
        log.info("Yahoo Finance API 호출: {} ({})", ticker, timeframe);
        List<StockCandleDTO> candles = fetchCandlesFromApi(ticker, timeframe, count);

        if (!candles.isEmpty()) {
            // 4. 캐시 저장
            saveCandleCache(ticker, timeframe, candles);
        }

        return candles;
    }

    /**
     * 종목 검색 (티커로 직접 조회)
     */
    @Override
    public List<StockInfoDTO> searchStocks(String query) {
        List<StockInfoDTO> results = new ArrayList<>();
        StockInfoDTO stockInfo = getStockInfo(query.toUpperCase());
        if (stockInfo != null) {
            results.add(stockInfo);
        }
        return results;
    }

    // ==================== API 호출 메서드 ====================

    /**
     * Yahoo Finance v8 API로 시세 정보 조회
     */
    private StockInfoDTO fetchStockInfoFromApi(String ticker) {
        String yahooTicker = convertToYahooTicker(ticker);
        String url = YAHOO_CHART_API + yahooTicker + "?interval=1d&range=1d";
        log.info("Yahoo API 요청 URL: {}", url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("API 호출 실패: {} - {}", ticker, response.code());
                    return null;
                }

                String json = response.body().string();
                return parseQuoteResponse(ticker, json);
            }
        } catch (Exception e) {
            log.error("API 호출 예외: {}", ticker, e);
            return null;
        }
    }

    /**
     * Yahoo Finance v8 API로 캔들 데이터 조회
     */
    private List<StockCandleDTO> fetchCandlesFromApi(String ticker, String timeframe, int count) {
        String yahooTicker = convertToYahooTicker(ticker);
        String interval = getYahooInterval(timeframe);
        String range = getYahooRange(timeframe, count);
        String url = YAHOO_CHART_API + yahooTicker + "?interval=" + interval + "&range=" + range;
        log.info("Yahoo 캔들 API 요청 URL: {}", url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("캔들 API 호출 실패: {} - {}", ticker, response.code());
                    return new ArrayList<>();
                }

                String json = response.body().string();
                return parseCandleResponse(json, count);
            }
        } catch (Exception e) {
            log.error("캔들 API 호출 예외: {}", ticker, e);
            return new ArrayList<>();
        }
    }

    // ==================== JSON 파싱 메서드 ====================

    private StockInfoDTO parseQuoteResponse(String ticker, String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject chart = root.getAsJsonObject("chart");
            JsonArray results = chart.getAsJsonArray("result");

            if (results == null || results.isEmpty()) {
                return null;
            }

            JsonObject result = results.get(0).getAsJsonObject();
            JsonObject meta = result.getAsJsonObject("meta");

            double price = getDoubleOrDefault(meta, "regularMarketPrice", 0.0);
            double prevClose = getDoubleOrDefault(meta, "chartPreviousClose", price);
            double change = price - prevClose;
            double changePercent = prevClose > 0 ? (change / prevClose) * 100 : 0;

            return StockInfoDTO.builder()
                    .ticker(ticker)
                    .name(getStringOrDefault(meta, "shortName", ticker))
                    .market(getStringOrDefault(meta, "exchangeName", "N/A"))
                    .currentPrice(price)
                    .changeAmount(change)
                    .changeRate(changePercent)
                    .tradingVolume(getLongOrDefault(meta, "regularMarketVolume", 0L))
                    .marketCap(0L) // v8 API에서는 marketCap 미제공
                    .high52Week(getDoubleOrDefault(meta, "fiftyTwoWeekHigh", 0.0))
                    .low52Week(getDoubleOrDefault(meta, "fiftyTwoWeekLow", 0.0))
                    .build();

        } catch (Exception e) {
            log.error("시세 JSON 파싱 오류", e);
            return null;
        }
    }

    private List<StockCandleDTO> parseCandleResponse(String json, int count) {
        List<StockCandleDTO> candles = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject chart = root.getAsJsonObject("chart");
            JsonArray results = chart.getAsJsonArray("result");

            if (results == null || results.isEmpty()) {
                return candles;
            }

            JsonObject result = results.get(0).getAsJsonObject();
            JsonArray timestamps = result.getAsJsonArray("timestamp");
            JsonObject indicators = result.getAsJsonObject("indicators");
            JsonArray quotes = indicators.getAsJsonArray("quote");

            if (timestamps == null || quotes == null || quotes.isEmpty()) {
                return candles;
            }

            JsonObject quote = quotes.get(0).getAsJsonObject();
            JsonArray opens = quote.getAsJsonArray("open");
            JsonArray highs = quote.getAsJsonArray("high");
            JsonArray lows = quote.getAsJsonArray("low");
            JsonArray closes = quote.getAsJsonArray("close");
            JsonArray volumes = quote.getAsJsonArray("volume");

            for (int i = 0; i < timestamps.size(); i++) {
                long timestamp = timestamps.get(i).getAsLong();
                LocalDate date = Instant.ofEpochSecond(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                StockCandleDTO candle = StockCandleDTO.builder()
                        .date(date.toString())
                        .open(getDoubleFromArray(opens, i))
                        .high(getDoubleFromArray(highs, i))
                        .low(getDoubleFromArray(lows, i))
                        .close(getDoubleFromArray(closes, i))
                        .volume(getLongFromArray(volumes, i))
                        .build();

                candles.add(candle);
            }

            // 이동평균선 계산
            calculateMovingAverages(candles);

            // 요청한 개수만큼 반환
            if (candles.size() > count) {
                candles = candles.subList(candles.size() - count, candles.size());
            }

        } catch (Exception e) {
            log.error("캔들 JSON 파싱 오류", e);
        }

        return candles;
    }

    // ==================== 캐시 저장 메서드 ====================

    private void saveQuoteCache(String ticker, StockInfoDTO dto) {
        StockQuoteCache cache = StockQuoteCache.builder()
                .ticker(ticker)
                .name(dto.getName())
                .market(dto.getMarket())
                .currentPrice(dto.getCurrentPrice())
                .changeAmount(dto.getChangeAmount())
                .changeRate(dto.getChangeRate())
                .tradingVolume(dto.getTradingVolume())
                .marketCap(dto.getMarketCap())
                .high52Week(dto.getHigh52Week())
                .low52Week(dto.getLow52Week())
                .updatedAt(LocalDateTime.now())
                .build();

        quoteCacheRepository.save(cache);
        log.info("시세 캐시 저장: {}", ticker);
    }

    @Transactional
    private void saveCandleCache(String ticker, String timeframe, List<StockCandleDTO> candles) {
        // 기존 캐시 삭제
        candleCacheRepository.deleteByTickerAndTimeframe(ticker, timeframe);

        // 새 캐시 저장
        List<StockCandleCache> cacheList = new ArrayList<>();
        for (StockCandleDTO dto : candles) {
            StockCandleCache cache = StockCandleCache.builder()
                    .ticker(ticker)
                    .date(LocalDate.parse(dto.getDate()))
                    .timeframe(timeframe)
                    .open(dto.getOpen())
                    .high(dto.getHigh())
                    .low(dto.getLow())
                    .close(dto.getClose())
                    .volume(dto.getVolume())
                    .updatedAt(LocalDateTime.now())
                    .build();
            cacheList.add(cache);
        }

        candleCacheRepository.saveAll(cacheList);
        log.info("캔들 캐시 저장: {} {} {}개", ticker, timeframe, cacheList.size());
    }

    // ==================== 헬퍼 메서드 ====================

    private StockInfoDTO convertToDTO(StockQuoteCache cache) {
        return StockInfoDTO.builder()
                .ticker(cache.getTicker())
                .name(cache.getName())
                .market(cache.getMarket())
                .currentPrice(cache.getCurrentPrice())
                .changeAmount(cache.getChangeAmount())
                .changeRate(cache.getChangeRate())
                .tradingVolume(cache.getTradingVolume())
                .marketCap(cache.getMarketCap())
                .high52Week(cache.getHigh52Week())
                .low52Week(cache.getLow52Week())
                .build();
    }

    private List<StockCandleDTO> convertCandlesToDTO(List<StockCandleCache> caches, int count) {
        List<StockCandleDTO> candles = new ArrayList<>();
        for (StockCandleCache cache : caches) {
            candles.add(StockCandleDTO.builder()
                    .date(cache.getDate().toString())
                    .open(cache.getOpen())
                    .high(cache.getHigh())
                    .low(cache.getLow())
                    .close(cache.getClose())
                    .volume(cache.getVolume())
                    .build());
        }

        // 이동평균선 계산
        calculateMovingAverages(candles);

        // 요청한 개수만큼 반환
        if (candles.size() > count) {
            candles = candles.subList(candles.size() - count, candles.size());
        }

        return candles;
    }

    private int getCandleTtl(String timeframe) {
        return switch (timeframe) {
            case "W" -> candleWeeklyTtlMinutes;
            case "M" -> candleWeeklyTtlMinutes * 2; // 월봉은 2배
            default -> candleDailyTtlMinutes;
        };
    }

    private String getYahooInterval(String timeframe) {
        return switch (timeframe) {
            case "W" -> "1wk";
            case "M" -> "1mo";
            default -> "1d";
        };
    }

    private String getYahooRange(String timeframe, int count) {
        return switch (timeframe) {
            case "W" -> Math.min(count / 4, 10) + "y"; // 주봉
            case "M" -> Math.min(count / 12, 20) + "y"; // 월봉
            default -> Math.min(count + 30, 365) + "d"; // 일봉 (여유있게)
        };
    }

    private double getDoubleOrDefault(JsonObject obj, String key, double defaultValue) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull()) ? element.getAsDouble() : defaultValue;
    }

    private String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull()) ? element.getAsString() : defaultValue;
    }

    private long getLongOrDefault(JsonObject obj, String key, long defaultValue) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull()) ? element.getAsLong() : defaultValue;
    }

    private double getDoubleFromArray(JsonArray arr, int index) {
        if (arr == null || index >= arr.size() || arr.get(index).isJsonNull()) {
            return 0.0;
        }
        return arr.get(index).getAsDouble();
    }

    private long getLongFromArray(JsonArray arr, int index) {
        if (arr == null || index >= arr.size() || arr.get(index).isJsonNull()) {
            return 0L;
        }
        return arr.get(index).getAsLong();
    }

    private void calculateMovingAverages(List<StockCandleDTO> candles) {
        for (int i = 0; i < candles.size(); i++) {
            if (i >= 19) {
                double sum = 0;
                for (int j = 0; j < 20; j++) sum += candles.get(i - j).getClose();
                candles.get(i).setSma20(Math.round(sum / 20 * 100.0) / 100.0);
            }
            if (i >= 59) {
                double sum = 0;
                for (int j = 0; j < 60; j++) sum += candles.get(i - j).getClose();
                candles.get(i).setSma60(Math.round(sum / 60 * 100.0) / 100.0);
            }
            if (i >= 119) {
                double sum = 0;
                for (int j = 0; j < 120; j++) sum += candles.get(i - j).getClose();
                candles.get(i).setSma120(Math.round(sum / 120 * 100.0) / 100.0);
            }
        }
    }
}