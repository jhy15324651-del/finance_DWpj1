package org.zerock.finance_dwpj1.service.portfolio;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.entity.portfolio.Investor13FHolding;
import org.zerock.finance_dwpj1.entity.portfolio.InvestorProfile;
import org.zerock.finance_dwpj1.entity.portfolio.SecCollectorCheckpoint;
import org.zerock.finance_dwpj1.entity.portfolio.SecCollectorCheckpoint.CheckpointStatus;
import org.zerock.finance_dwpj1.repository.portfolio.InvestorProfileRepository;
import org.zerock.finance_dwpj1.dto.sec.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SEC 13F 분기보고서 데이터 수집 서비스
 * - RateLimiter로 정확한 API 제한 (초당 10회)
 * - 트랜잭션은 별도 TransactionalService로 분리
 * - Resume 기능 포함
 */
@Service
@Slf4j
public class SEC13FService {

    private final InvestorProfileRepository profileRepository;
    private final SEC13FTransactionalService transactionalService;
    private final CusipToTickerService cusipService;

    // SEC API Rate Limiter: 초당 10회 (여유 있게 9회로 설정)
    private final RateLimiter rateLimiter = RateLimiter.create(9.0);

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean collecting = new AtomicBoolean(false);

    private static final String SEC_SUBMISSIONS_API = "https://data.sec.gov/submissions/CIK%s.json";
    private static final String USER_AGENT = "FinanceDWPJ1/1.0 (jhy15324651@gmail.com)";

    // 재시도 설정
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 2000; // 2초

    private final OkHttpClient httpClient;

    // Constructor
    public SEC13FService(
            InvestorProfileRepository profileRepository,
            SEC13FTransactionalService transactionalService,
            CusipToTickerService cusipService) {

        this.profileRepository = profileRepository;
        this.transactionalService = transactionalService;
        this.cusipService = cusipService;

        // OkHttpClient 설정 (RateLimiter는 별도 처리)
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .header("Accept", "application/xml, text/xml, application/json, */*")
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    /**
     * ApplicationContext가 완전히 준비된 후 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("✅ ApplicationContext 준비 완료 - SEC 13F 수집 대기 상태");
    }

    /**
     * ApplicationContext가 닫힐 때 수집 작업 중단
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        log.error("🔴 ApplicationContext 닫힘 감지! 수집 작업 실행 중: {}", collecting.get());
        if (collecting.get()) {
            log.warn("⚠️ 강제 중단: 진행 중인 수집 작업 종료");
            stopCollection();
        }
    }

    /**
     * 특정 투자자의 최신 13F 데이터 수집 (단일 투자자용)
     */
    public int fetch13FDataForInvestor(String investorId) {
        log.info("=== {} 투자대가 13F 데이터 수집 시작 ===", investorId);

        InvestorProfile profile = profileRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("투자대가를 찾을 수 없습니다: " + investorId));

        // RateLimiter 획득 (블로킹)
        rateLimiter.acquire();

        String latest13FUrl = getLatest13FFileUrl(profile.getCik());
        if (latest13FUrl == null) {
            log.warn("{}의 13F 파일을 찾을 수 없습니다", profile.getName());
            return 0;
        }

        // RateLimiter 획득 (XML 파일 다운로드)
        rateLimiter.acquire();

        List<Investor13FHolding> holdings = parse13FFile(latest13FUrl, investorId);

        if (!holdings.isEmpty()) {
            String quarter = holdings.get(0).getFilingQuarter();

            // 트랜잭션 서비스로 저장 위임
            int count = transactionalService.saveHoldings(investorId, quarter, holdings);

            if (count > 0) {
                log.info("{}의 13F 데이터 {}건 저장 완료 (분기: {})",
                        profile.getName(), holdings.size(), quarter);
            }
            return count;
        }

        return 0;
    }

    /**
     * 특정 투자자의 모든 기존 데이터 삭제 후 재수집 (강제 재수집)
     * 관리자 페이지에서 투자자 이름 클릭 시 호출
     */
    public int refetchInvestorData(String investorId) {
        log.info("🔄 === {} 투자대가 13F 데이터 강제 재수집 시작 ===", investorId);

        InvestorProfile profile = profileRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("투자대가를 찾을 수 없습니다: " + investorId));

        // 1. 기존 데이터 모두 삭제
        int deletedCount = transactionalService.deleteAllInvestorData(investorId);
        log.info("✅ {}의 기존 데이터 {}건 삭제 완료", profile.getName(), deletedCount);

        // 2. 새로 수집
        int newCount = fetch13FDataForInvestor(investorId);
        log.info("✅ {}의 새 데이터 {}건 수집 완료", profile.getName(), newCount);

        return newCount;
    }

    /**
     * 모든 투자자의 13F 데이터 수집 (Resume 기능 포함)
     */
    private void fetchAll13FData() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("⚠️ 이미 13F 데이터 수집이 실행 중입니다");
            return;
        }

        stopRequested.set(false);
        List<InvestorProfile> profiles = profileRepository.findByActiveTrue();
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  SEC 13F 전체 수집 시작 - 총 {}명                      ║", profiles.size());
        log.info("╚══════════════════════════════════════════════════════════╝");

        int totalSuccess = 0;
        int totalFailed = 0;
        int totalSkipped = 0;

        try {
            for (int i = 0; i < profiles.size(); i++) {
                InvestorProfile profile = profiles.get(i);

                // 중단 요청 확인
                if (stopRequested.get()) {
                    log.warn("⏸️ 중단 요청 감지 - 현재까지 {}/{}명 처리 완료",
                            i, profiles.size());
                    break;
                }

                log.info("┌─────────────────────────────────────────────────────────┐");
                log.info("│ [{}/{}] 투자자: {} ({})",
                    i + 1, profiles.size(), profile.getName(), profile.getInvestorId());
                log.info("│ CIK: {}, 조직: {}", profile.getCik(), profile.getOrganization());
                log.info("└─────────────────────────────────────────────────────────┘");

                try {
                    // Checkpoint 기반 Resume 로직
                    String currentQuarter = extractQuarter(LocalDate.now());
                    SecCollectorCheckpoint checkpoint = transactionalService
                            .findCheckpoint(profile.getInvestorId(), currentQuarter);

                    // 이미 성공한 경우 건너뛰기
                    if (checkpoint != null && checkpoint.getStatus() == CheckpointStatus.SUCCESS) {
                        log.info("✓ 이미 성공 ({} at {})", currentQuarter, checkpoint.getCompletedAt());
                        totalSkipped++;
                        continue;
                    }

                    // Checkpoint 생성 또는 업데이트 (IN_PROGRESS)
                    transactionalService.markInProgress(profile.getInvestorId(), currentQuarter);

                    // 재시도 로직으로 수집 실행
                    int count = fetch13FDataWithRetry(profile);

                    if (count > 0) {
                        // 성공
                        transactionalService.markSuccess(
                                profile.getInvestorId(), currentQuarter, count);
                        log.info("✅ SUCCESS - {}건 저장", count);
                        totalSuccess++;
                    } else {
                        // 데이터 없음 (SKIPPED)
                        transactionalService.markSkipped(
                                profile.getInvestorId(), currentQuarter, "No 13F data found");
                        log.warn("⚠️ SKIPPED - 데이터 없음");
                        totalSkipped++;
                    }

                } catch (Exception e) {
                    // 실패 처리 (앱 전체는 종료 안 함!)
                    log.error("❌ FAILED - {}", profile.getName(), e);

                    String currentQuarter = extractQuarter(LocalDate.now());
                    transactionalService.markFailed(
                            profile.getInvestorId(), currentQuarter, e);

                    totalFailed++;
                }
            }

            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║  SEC 13F 수집 완료                                       ║");
            log.info("║  ✅ 성공: {}명  ❌ 실패: {}명  ⏭️ 건너뜀: {}명          ║",
                    totalSuccess, totalFailed, totalSkipped);
            log.info("╚══════════════════════════════════════════════════════════╝");

        } finally {
            collecting.set(false);
        }
    }

    /**
     * 재시도 로직이 포함된 13F 데이터 수집
     */
    private int fetch13FDataWithRetry(InvestorProfile profile) throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                return fetch13FDataForInvestor(profile.getInvestorId());

            } catch (Exception e) {
                retryCount++;
                lastException = e;

                // 재시도 가능한 오류인지 판단
                if (isRetryable(e)) {
                    long delay = calculateBackoff(retryCount);
                    log.warn("⚠️ 재시도 {}/{} - {}초 후 재시도 (원인: {})",
                            retryCount, MAX_RETRIES, delay / 1000, e.getMessage());
                    Thread.sleep(delay);
                } else {
                    // 재시도 불가능한 오류 (즉시 실패)
                    log.error("💀 재시도 불가능한 오류 발생: {}", e.getMessage());
                    throw e;
                }
            }
        }

        // 최대 재시도 횟수 초과
        log.error("💀 최대 재시도 횟수 초과 ({}/{})", retryCount, MAX_RETRIES);
        throw lastException;
    }

    /**
     * 재시도 가능한 예외인지 판단
     */
    private boolean isRetryable(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // 네트워크 오류, 타임아웃, SEC rate limit 등은 재시도
        return message.contains("timeout") ||
               message.contains("429") ||
               message.contains("503") ||
               message.contains("connection") ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.io.IOException;
    }

    /**
     * Exponential Backoff 계산
     */
    private long calculateBackoff(int retryCount) {
        return BASE_DELAY_MS * (long) Math.pow(2, retryCount - 1);
    }

    /**
     * SEC API에서 최신 13F 파일 URL 가져오기 (RateLimiter 적용)
     */
    private String getLatest13FFileUrl(String cik) {
        String paddedCik = String.format("%010d", Integer.parseInt(cik));
        String url = String.format(SEC_SUBMISSIONS_API, paddedCik);
        log.info("🔍 SEC API 호출: {}", url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                int statusCode = response.code();
                String contentType = response.header("Content-Type", "unknown");
                log.info("📡 SEC API 응답 - Status: {}, Content-Type: {}", statusCode, contentType);

                // Rate Limit 처리
                if (statusCode == 429) {
                    String retryAfter = response.header("Retry-After", "10");
                    log.warn("🚨 SEC Rate Limit! Retry-After: {}초", retryAfter);
                    Thread.sleep(Integer.parseInt(retryAfter) * 1000);
                    throw new RuntimeException("SEC Rate Limit (429) - Retry-After: " + retryAfter);
                }

                // 기타 HTTP 오류
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("❌ SEC API 호출 실패: {} ({})", statusCode, response.message());
                    throw new RuntimeException("SEC API HTTP Error: " + statusCode);
                }

                String responseBody = response.body().string();

                // HTML 응답 감지
                String trimmedBody = responseBody.trim().toLowerCase();
                if (trimmedBody.startsWith("<!doctype html") || trimmedBody.startsWith("<html")) {
                    log.warn("⚠️ SEC API가 HTML 페이지를 반환!");
                    log.warn("응답 시작: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                    throw new RuntimeException("SEC API returned HTML instead of JSON");
                }

                // JSON 파싱
                JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonObject filings = root.getAsJsonObject("filings");
                JsonObject recent = filings.getAsJsonObject("recent");

                JsonArray forms = recent.getAsJsonArray("form");
                JsonArray accessionNumbers = recent.getAsJsonArray("accessionNumber");

                // 최신 13F-HR 찾기
                for (int i = 0; i < forms.size(); i++) {
                    String form = forms.get(i).getAsString();
                    if ("13F-HR".equals(form) || "13F-HR/A".equals(form)) {
                        String accessionNumber = accessionNumbers.get(i).getAsString().replace("-", "");

                        // RateLimiter 획득 (index.json 호출)
                        rateLimiter.acquire();

                        String fileUrl = find13FXmlFromIndex(cik, accessionNumber);

                        if (fileUrl != null) {
                            log.info("✅ 최신 13F 파일 찾음: {}", fileUrl);
                            return fileUrl;
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🔴 스레드 인터럽트 발생", e);
            throw new RuntimeException("Thread interrupted", e);
        } catch (Exception e) {
            log.error("❌ SEC API 호출 중 오류", e);
            throw new RuntimeException("SEC API call failed", e);
        }

        return null;
    }

    /**
     * 13F XML 파일 파싱
     */
    private List<Investor13FHolding> parse13FFile(String fileUrl, String investorId) {
        List<Investor13FHolding> holdings = new ArrayList<>();

        try {
            Request request = new Request.Builder()
                    .url(fileUrl)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("13F 파일 다운로드 실패: {}", response.code());
                    return holdings;
                }

                String xmlContent = response.body().string();
                log.info("13F XML 파일 다운로드 완료 ({}자)", xmlContent.length());

                // HTML 응답 감지
                String trimmedContent = xmlContent.trim().toLowerCase();
                if (trimmedContent.startsWith("<!doctype html") || trimmedContent.startsWith("<html")) {
                    log.warn("⚠️ HTML 응답 감지! XML 대신 HTML 반환됨");
                    return holdings;
                }

                // JAXB 파싱
                JAXBContext jaxbContext = JAXBContext.newInstance(InformationTable.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                spf.setNamespaceAware(true);

                SAXParser saxParser = spf.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();

                InputSource inputSource = new InputSource(new StringReader(xmlContent));
                SAXSource saxSource = new SAXSource(xmlReader, inputSource);

                InformationTable infoTable = (InformationTable) unmarshaller.unmarshal(saxSource);

                LocalDate filingDate = LocalDate.now();
                String quarter = extractQuarter(filingDate);

                log.info("13F 보고서 분기: {}, 날짜: {}", quarter, filingDate);

                if (infoTable == null || infoTable.getInfoTables() == null) {
                    log.warn("InformationTable이 비어있습니다");
                    return holdings;
                }

                List<InfoTable> infoTables = infoTable.getInfoTables();
                log.info("총 {}개 보유 종목 발견", infoTables.size());

                long totalValue = infoTables.stream()
                    .mapToLong(it -> it.getValue() != null ? it.getValue() : 0L)
                    .sum();

                log.info("전체 포트폴리오 가치: ${} (천 단위)", totalValue);

                for (InfoTable info : infoTables) {
                    try {
                        String cusip = info.getCusip();
                        if (cusip == null || cusip.isEmpty()) {
                            continue;
                        }

                        String ticker = cusipService.convertCusipToTicker(cusip);
                        if (ticker == null) {
                            log.warn("CUSIP {}에 대한 Ticker를 찾을 수 없음", cusip);
                            continue;
                        }

                        Long valueInThousands = info.getValue();
                        if (valueInThousands == null || valueInThousands == 0) {
                            continue;
                        }
                        double marketValue = valueInThousands * 1000.0;
                        double portfolioWeight = (valueInThousands * 100.0) / totalValue;

                        Investor13FHolding holding = Investor13FHolding.builder()
                            .investorId(investorId)
                            .ticker(ticker)
                            .companyName(info.getNameOfIssuer())
                            .shares(info.getSshPrnamt() != null ? info.getSshPrnamt() : 0L)
                            .marketValue(marketValue)
                            .portfolioWeight(Math.round(portfolioWeight * 100.0) / 100.0)
                            .filingQuarter(quarter)
                            .filingDate(filingDate)
                            .secFilingUrl(fileUrl)
                            .build();

                        holdings.add(holding);

                    } catch (Exception e) {
                        log.error("종목 파싱 중 오류: {}", info.getNameOfIssuer(), e);
                    }
                }

                log.info("13F 파싱 완료: {} → {}개 종목",
                    investorId, holdings.size());

            }
        } catch (Exception e) {
            log.error("13F 파일 파싱 중 오류", e);
        }

        return holdings;
    }

    private String extractQuarter(LocalDate date) {
        int year = date.getYear();
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return year + "Q" + quarter;
    }

    private String find13FXmlFromIndex(String cik, String accessionNumber) {
        String indexUrl = String.format("https://www.sec.gov/Archives/edgar/data/%s/%s/index.json",
                cik, accessionNumber);
        log.debug("index.json 조회: {}", indexUrl);

        try {
            Request request = new Request.Builder()
                    .url(indexUrl)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("index.json 조회 실패: {}", response.code());
                    return null;
                }

                String jsonBody = response.body().string();
                JsonObject index = JsonParser.parseString(jsonBody).getAsJsonObject();
                JsonObject directory = index.getAsJsonObject("directory");
                JsonArray items = directory.getAsJsonArray("item");

                String largestXmlFile = null;
                long largestSize = 0;

                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    String name = item.get("name").getAsString();

                    if (name.endsWith(".xml")) {
                        String sizeStr = item.get("size").getAsString();

                        if (!sizeStr.isEmpty()) {
                            try {
                                long size = Long.parseLong(sizeStr);
                                if (size > largestSize) {
                                    largestSize = size;
                                    largestXmlFile = name;
                                }
                            } catch (NumberFormatException e) {
                                log.debug("파일 크기 파싱 실패: {} ({})", name, sizeStr);
                            }
                        }
                    }
                }

                if (largestXmlFile != null) {
                    String fileUrl = String.format("https://www.sec.gov/Archives/edgar/data/%s/%s/%s",
                            cik, accessionNumber, largestXmlFile);
                    log.info("13F XML 파일 발견: {} ({}bytes)", largestXmlFile, largestSize);
                    return fileUrl;
                } else {
                    log.warn("index.json에서 .xml 파일을 찾을 수 없습니다");
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("index.json 파싱 중 오류", e);
            return null;
        }
    }

    /**
     * 비동기 수집 시작 (컨트롤러에서 호출)
     */
    @Async
    public void startAsyncCollection() {
        log.info("🚀 비동기 13F 데이터 수집 시작 요청 수신");
        fetchAll13FData();
        log.info("✅ 비동기 13F 데이터 수집 작업 종료");
    }

    public void stopCollection() {
        if (!collecting.get()) {
            log.warn("⚠️ 실행 중인 13F 수집 작업이 없습니다");
            return;
        }
        log.info("🛑 13F 수집 중단 요청 플래그 ON");
        stopRequested.set(true);
    }

    public boolean isCollecting() {
        return collecting.get();
    }
}