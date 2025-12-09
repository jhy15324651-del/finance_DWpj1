package org.zerock.finance_dwpj1.service.portfolio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.entity.portfolio.Investor13FHolding;
import org.zerock.finance_dwpj1.entity.portfolio.InvestorProfile;
import org.zerock.finance_dwpj1.repository.portfolio.Investor13FHoldingRepository;
import org.zerock.finance_dwpj1.dto.sec.*;
import org.zerock.finance_dwpj1.repository.portfolio.InvestorProfileRepository;

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
 * SEC EDGAR API를 통해 투자대가들의 포트폴리오 데이터를 가져옵니다
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SEC13FService {

    private final InvestorProfileRepository profileRepository;
    private final Investor13FHoldingRepository holdingRepository;
    private final CusipToTickerService cusipService;


    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean collecting = new AtomicBoolean(false);


    private static final String SEC_SUBMISSIONS_API = "https://data.sec.gov/submissions/CIK%s.json";
    private static final String SEC_13F_DETAIL_BASE = "https://www.sec.gov/cgi-bin/browse-edgar";

    // SEC 권장 User-Agent (실제 앱 이름과 이메일 필수, )
    private static final String USER_AGENT = "FinanceDWPJ1/1.0 (jhy15324651@gmail.com)";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                // SEC 권장 헤더 추가 (Host는 자동 설정됨)
                Request request = chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/xml, text/xml, application/json, */*")
                        .build();
                try {
                    // SEC API rate limit 방지: 초당 10회 이하 (500ms 딜레이)
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("SEC API 요청 중 인터럽트 발생", e);
                }
                return chain.proceed(request);
            })
            .build();

    /**
     * 특정 투자대가의 최신 13F 데이터 수집
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public int fetch13FDataForInvestor(String investorId) {
        log.info("=== {} 투자대가 13F 데이터 수집 시작 ===", investorId);

        InvestorProfile profile = profileRepository.findById(investorId)
                .orElseThrow(() -> new IllegalArgumentException("투자대가를 찾을 수 없습니다: " + investorId));

        String latest13FUrl = getLatest13FFileUrl(profile.getCik());
        if (latest13FUrl == null) {
            log.warn("{}의 13F 파일을 찾을 수 없습니다", profile.getName());
            return 0;
        }

        List<Investor13FHolding> holdings = parse13FFile(latest13FUrl, investorId);

        if (!holdings.isEmpty()) {
            String quarter = holdings.get(0).getFilingQuarter();
            if (holdingRepository.existsByInvestorIdAndFilingQuarter(investorId, quarter)) {
                log.info("{}의 {} 데이터가 이미 존재합니다. 건너뜁니다.", profile.getName(), quarter);
                return 0;
            }

            holdingRepository.saveAll(holdings);
            holdingRepository.flush();
            log.info("{}의 13F 데이터 {}건 저장 완료 (분기: {})",
                    profile.getName(), holdings.size(), quarter);
            return holdings.size();
        }

        return 0;
    }

    /**
     * 모든 투자대가의 13F 데이터 수집
     */
    // 전체 투자자 13F 수집 (실제 작업 메서드, 내부에서만 호출)
    private void fetchAll13FData() {

        // 이미 실행 중이면 바로 리턴
        if (!collecting.compareAndSet(false, true)) {
            log.warn("⚠️ 이미 13F 데이터 수집이 실행 중입니다");
            return;
        }

        stopRequested.set(false);

        List<InvestorProfile> profiles = profileRepository.findByActiveTrue();
        log.info("=== 전체 투자대가 13F 데이터 수집 시작 ({} 명) ===", profiles.size());

        int totalCount = 0;
        int processedCount = 0;

        try {
            for (InvestorProfile profile : profiles) {

                // 중단 플래그 확인
                if (stopRequested.get()) {
                    log.warn("⏸️ 중단 요청 감지 - 현재까지 {}/{}명 처리 완료",
                            processedCount, profiles.size());
                    break;
                }

                try {
                    log.info("처리 중: {} ({}/{})", profile.getName(),
                            processedCount + 1, profiles.size());

                    int count = fetch13FDataForInvestor(profile.getInvestorId());
                    totalCount += count;
                    processedCount++;

                } catch (Exception e) {
                    log.error("{}의 13F 데이터 수집 실패", profile.getName(), e);
                    processedCount++;
                }
            }

            log.info("=== 전체 13F 데이터 수집 완료: 총 {}건 ({}/{}명 처리) ===",
                    totalCount, processedCount, profiles.size());

        } finally {
            // 무조건 collecting false로 돌려줌
            collecting.set(false);
        }
    }



    /**
     * SEC API에서 최신 13F 파일 URL 가져오기
     */
    private String getLatest13FFileUrl(String cik) {
        String paddedCik = String.format("%010d", Integer.parseInt(cik));
        String url = String.format(SEC_SUBMISSIONS_API, paddedCik);
        log.info("SEC API 호출: {}", url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                // 상세 응답 로그 (디버깅용)
                int statusCode = response.code();
                String contentType = response.header("Content-Type", "unknown");
                log.info("SEC API 응답 - Status: {}, Content-Type: {}", statusCode, contentType);

                if (!response.isSuccessful() || response.body() == null) {
                    log.error("SEC API 호출 실패: {} ({})", statusCode, response.message());
                    return null;
                }

                String responseBody = response.body().string();

                // 응답 미리보기 로그 (처음 500자)
                String preview = responseBody.substring(0, Math.min(500, responseBody.length()));
                log.debug("SEC API 응답 미리보기 (처음 500자): {}", preview);

                // HTML 응답 감지 (SEC가 차단/에러 시 HTML 반환)
                String trimmedBody = responseBody.trim().toLowerCase();
                if (trimmedBody.startsWith("<!doctype html") || trimmedBody.startsWith("<html")) {
                    log.warn("⚠️ SEC API가 HTML 페이지를 반환했습니다!");
                    log.warn("가능한 원인: Rate Limit, IP 차단, User-Agent 거부, 또는 SEC 서버 오류");
                    log.warn("응답 시작 부분: {}", preview);
                    return null;
                }

                // XSSI (Cross-Site Script Inclusion) prefix 제거
                // 일부 API는 ")]}',\n" 같은 프리픽스를 추가함
                String jsonBody = responseBody;
                if (jsonBody.startsWith(")]}',")) {
                    log.debug("XSSI prefix 감지, 제거 중...");
                    jsonBody = jsonBody.substring(5).trim();
                }

                // JSON 파싱 (관대 모드 사용)
                JsonObject root;
                try {
                    root = JsonParser.parseString(jsonBody).getAsJsonObject();
                } catch (com.google.gson.JsonSyntaxException e) {
                    log.error("❌ JSON 파싱 실패! 응답이 유효한 JSON이 아닙니다.");
                    log.error("응답 시작 부분: {}", preview);
                    log.error("Content-Type: {}", contentType);
                    log.error("파싱 에러: {}", e.getMessage());
                    return null;
                }

                JsonObject filings = root.getAsJsonObject("filings");
                JsonObject recent = filings.getAsJsonObject("recent");

                JsonArray forms = recent.getAsJsonArray("form");
                JsonArray accessionNumbers = recent.getAsJsonArray("accessionNumber");
                JsonArray primaryDocuments = recent.getAsJsonArray("primaryDocument");

                // 최신 13F-HR 찾기
                for (int i = 0; i < forms.size(); i++) {
                    String form = forms.get(i).getAsString();
                    if ("13F-HR".equals(form) || "13F-HR/A".equals(form)) {
                        String accessionNumber = accessionNumbers.get(i).getAsString().replace("-", "");

                        // index.json에서 실제 13F XML 파일 찾기
                        // primaryDocument는 XSLT 변환 파일이므로 사용하지 않음
                        String fileUrl = find13FXmlFromIndex(cik, accessionNumber);

                        if (fileUrl != null) {
                            log.info("최신 13F 파일 찾음: {}", fileUrl);
                            return fileUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("SEC API 호출 중 오류", e);
        }

        return null;
    }

    /**
     * 13F XML 파일 파싱 (JAXB 사용)
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

                // HTML 응답 감지 및 스킵 (SEC가 XML 대신 HTML 페이지를 반환한 경우)
                String trimmedContent = xmlContent.trim().toLowerCase();
                if (trimmedContent.startsWith("<!doctype html") || trimmedContent.startsWith("<html")) {
                    log.warn("⚠️ HTML 응답 감지! SEC가 XML 대신 HTML 페이지를 반환했습니다.");
                    log.warn("URL: {}", fileUrl);
                    log.warn("가능한 원인: User-Agent 부족, Rate Limit, 파일이 실제로 HTML, 또는 SEC 접근 제한");
                    log.warn("이 투자자의 13F 데이터는 건너뜁니다.");
                    return holdings; // 빈 리스트 반환
                }

                // JAXB로 XML 파싱 - InformationTable 직접 파싱
                // 46994.xml은 <informationTable>만 포함하므로 EdgarSubmission이 아닌 InformationTable로 파싱
                JAXBContext jaxbContext = JAXBContext.newInstance(InformationTable.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                // XXE(XML External Entity) 공격 방지 및 외부 DTD 로딩 비활성화
                // SEC XML은 외부 DTD를 HTTP로 참조하는데, Java 보안 정책상 차단됨
                // SAXParser를 사용하여 외부 엔티티 및 DTD 로딩을 비활성화
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

                // 분기 및 날짜 계산 (CoverPage가 없으므로 현재 분기 사용)
                LocalDate filingDate = LocalDate.now();
                String quarter = extractQuarter(filingDate);

                log.info("13F 보고서 분기: {}, 날짜: {}", quarter, filingDate);

                // InformationTable에서 보유 종목 추출
                if (infoTable == null || infoTable.getInfoTables() == null) {
                    log.warn("InformationTable이 비어있습니다");
                    return holdings;
                }

                List<InfoTable> infoTables = infoTable.getInfoTables();
                log.info("총 {}개 보유 종목 발견", infoTables.size());

                // 전체 포트폴리오 가치 계산 (비중 계산용)
                long totalValue = infoTables.stream()
                    .mapToLong(it -> it.getValue() != null ? it.getValue() : 0L)
                    .sum();

                log.info("전체 포트폴리오 가치: ${} (천 단위)", totalValue);

                // 각 종목을 Holding 엔티티로 변환
                for (InfoTable info : infoTables) {
                    try {
                        String cusip = info.getCusip();
                        if (cusip == null || cusip.isEmpty()) {
                            continue;
                        }

                        // CUSIP를 Ticker로 변환
                        String ticker = cusipService.convertCusipToTicker(cusip);
                        if (ticker == null) {
                            log.warn("CUSIP {}에 대한 Ticker를 찾을 수 없음. 건너뜀.", cusip);
                            continue;
                        }

                        // 시장 가치 (천 단위 → 실제 단위)
                        Long valueInThousands = info.getValue();
                        if (valueInThousands == null || valueInThousands == 0) {
                            continue;
                        }
                        double marketValue = valueInThousands * 1000.0;

                        // 포트폴리오 비중 계산 (%)
                        double portfolioWeight = (valueInThousands * 100.0) / totalValue;

                        Investor13FHolding holding = Investor13FHolding.builder()
                            .investorId(investorId)
                            .ticker(ticker)
                            .companyName(info.getNameOfIssuer())
                            .shares(info.getSshPrnamt() != null ? info.getSshPrnamt() : 0L)
                            .marketValue(marketValue)
                            .portfolioWeight(Math.round(portfolioWeight * 100.0) / 100.0) // 소수점 2자리
                            .filingQuarter(quarter)
                            .filingDate(filingDate)
                            .secFilingUrl(fileUrl)
                            .build();

                        holdings.add(holding);

                    } catch (Exception e) {
                        log.error("종목 파싱 중 오류: {}", info.getNameOfIssuer(), e);
                    }
                }

                log.info("13F 파싱 완료: {} → {}개 종목 ({}개 Ticker 변환 성공)",
                    investorId, infoTables.size(), holdings.size());

            }
        } catch (Exception e) {
            log.error("13F 파일 파싱 중 오류", e);
        }

        return holdings;
    }

    /**
     * 보고서 날짜 파싱 (MM-DD-YYYY → LocalDate)
     */
    private LocalDate parseReportDate(String reportDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            return LocalDate.parse(reportDate, formatter);
        } catch (Exception e) {
            log.error("날짜 파싱 실패: {}", reportDate, e);
            return LocalDate.now();
        }
    }

    /**
     * LocalDate로부터 분기 추출
     */
    private String extractQuarter(LocalDate date) {
        int year = date.getYear();
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return year + "Q" + quarter;
    }

    private String extractQuarterFromXml(String xml) {
        // 간단한 정규식으로 분기 추출
        // 실제로는 XML 파서 사용 권장
        try {
            if (xml.contains("<periodOfReport>")) {
                int start = xml.indexOf("<periodOfReport>") + 16;
                int end = xml.indexOf("</periodOfReport>");
                String date = xml.substring(start, end); // YYYY-MM-DD

                LocalDate reportDate = LocalDate.parse(date);
                int year = reportDate.getYear();
                int quarter = (reportDate.getMonthValue() - 1) / 3 + 1;

                return year + "Q" + quarter;
            }
        } catch (Exception e) {
            log.error("분기 추출 실패", e);
        }

        return LocalDate.now().getYear() + "Q1";
    }

    /**
     * index.json에서 실제 13F Information Table XML 파일 찾기
     * SEC EDGAR는 primaryDocument에 XSLT 변환 파일 경로를 반환하므로,
     * index.json을 파싱하여 실제 데이터 XML 파일을 찾아야 함
     */
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

                // .xml 파일 중 가장 큰 파일 찾기 (Information Table은 보통 가장 큼)
                String largestXmlFile = null;
                long largestSize = 0;

                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    String name = item.get("name").getAsString();

                    // .xml 파일만 확인
                    if (name.endsWith(".xml")) {
                        String sizeStr = item.get("size").getAsString();

                        // size가 빈 문자열이 아닌 경우에만 파싱
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


    // 컨트롤러에서 호출하는 **비동기 시작 메서드**
    @Async
    public void startAsyncCollection() {
        log.info("🚀 비동기 13F 데이터 수집 시작 요청 수신");

        fetchAll13FData();   // 위에서 만든 실제 작업 메서드 호출

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

    // 상태 조회 (Controller에서 /status에 사용)
    public boolean isCollecting() {
        return collecting.get();
    }

}