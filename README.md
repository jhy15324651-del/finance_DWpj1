# Finance_DWpj1 프로젝트 코드 상세 설명서

## 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [프로젝트 구조](#프로젝트-구조)
4. [백엔드 코드 상세 설명](#백엔드-코드-상세-설명)
5. [프론트엔드 코드 상세 설명](#프론트엔드-코드-상세-설명)
6. [데이터 흐름](#데이터-흐름)

---

## 프로젝트 개요

이 프로젝트는 **금융 정보 플랫폼**으로, 다음 기능을 제공합니다:
- **주식 차트 조회**: Yahoo Finance API를 통해 주식 데이터를 가져와 캔들스틱 차트로 표시
- **투자자 비교**: 유명 투자자들의 투자 철학을 비교 분석
- **AI 포트폴리오 추천**: GPT API를 활용하여 맞춤형 포트폴리오 추천
- **금융 뉴스**: Yahoo Finance에서 뉴스를 크롤링하여 한국어로 번역/요약
- **SEC 13F 분석**: 미국 대형 투자자들의 13F 보고서 수집 및 포트폴리오 추적
- **OCR 포트폴리오 분석**: 증권사 앱 스크린샷에서 보유 종목 자동 추출 및 투자 스타일 분석
- **커뮤니티**: 회원제 게시판, 댓글/대댓글, 평점 시스템
- **주식 게시판**: 종목별 게시판 및 추천/비추천 시스템
- **사용자 인증**: Spring Security 기반 회원가입/로그인
- **마이페이지**: 내가 쓴 글/댓글, 등급 관리

---

## 기술 스택

### build.gradle 분석
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.7'    // Spring Boot 3.5.7 버전 사용
    id 'io.spring.dependency-management' version '1.1.7'  // 의존성 버전 관리
}

group = 'org.zerock'           // 프로젝트 그룹 ID
version = '0.0.1-SNAPSHOT'     // 프로젝트 버전
```

### 주요 의존성 설명
| 의존성 | 설명 |
|--------|------|
| `spring-boot-starter-web` | REST API 및 웹 애플리케이션 구축 |
| `spring-boot-starter-thymeleaf` | 서버 사이드 템플릿 엔진 |
| `spring-boot-starter-data-jpa` | JPA를 통한 데이터베이스 연동 |
| `spring-boot-starter-security` | 보안 및 인증 기능 (Spring Security 6) |
| `spring-boot-starter-validation` | Bean Validation (비밀번호 유효성 검증 등) |
| `spring-boot-starter-webflux` | WebClient for reactive HTTP (Gemini API 등) |
| `jsoup:1.17.2` | 웹 크롤링 라이브러리 |
| `openai-gpt3-java:service:0.18.2` | OpenAI GPT API 클라이언트 |
| `okhttp3:4.12.0` | HTTP 클라이언트 (한국투자증권 API) |
| `javax.xml.bind:jaxb-api:2.3.1` | XML 파싱 (SEC 13F 보고서) |
| `tess4j:5.9.0` | Tesseract OCR 라이브러리 |
| `opencv:4.9.0-0` | OpenCV 이미지 전처리 |
| `YahooFinanceAPI:3.17.0` | Yahoo Finance 주식 데이터 |
| `mariadb-java-client` | MariaDB 데이터베이스 드라이버 |
| `lombok` | 보일러플레이트 코드 자동 생성 |

---

## 프로젝트 구조

```
src/main/java/org/zerock/finance_dwpj1/
├── FinanceDWpj1Application.java    # 메인 애플리케이션 진입점
├── config/                          # 설정 클래스
│   ├── SecurityConfig.java         # 보안 설정
│   └── DataInitializer.java        # 초기 데이터 생성
├── controller/                      # 컨트롤러 (요청 처리)
│   ├── common/PageController.java
│   ├── portfolio/PortfolioApiController.java
│   ├── stock/StockChartController.java
│   └── insights/DailyNewsController.java
├── service/                         # 비즈니스 로직
│   ├── common/GPTService.java
│   ├── portfolio/InvestorComparisonService.java
│   ├── stock/StockService.java
│   └── insights/DailyNewsService.java
├── dto/                             # 데이터 전송 객체
├── entity/                          # JPA 엔티티
└── repository/                      # 데이터 접근 계층
```

---

## 백엔드 코드 상세 설명

### 1. FinanceDWpj1Application.java (메인 클래스)

```java
@SpringBootApplication    // Spring Boot 자동 설정 활성화
@EnableScheduling         // 스케줄링 기능 활성화 (뉴스 자동 크롤링 등)
public class FinanceDWpj1Application {
    public static void main(String[] args) {
        SpringApplication.run(FinanceDWpj1Application.class, args);
    }
}
```

**어노테이션 설명:**
- `@SpringBootApplication`: 다음 3개 어노테이션을 합친 것
  - `@Configuration`: 설정 클래스임을 명시
  - `@EnableAutoConfiguration`: 자동 설정 활성화
  - `@ComponentScan`: 컴포넌트 스캔 활성화
- `@EnableScheduling`: `@Scheduled` 어노테이션이 붙은 메서드를 자동 실행

---

### 2. SecurityConfig.java (보안 설정)

```java
@Configuration            // 스프링 설정 클래스
@EnableWebSecurity        // 웹 보안 활성화
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // 비밀번호 암호화에 BCrypt 알고리즘 사용
    }
```

**변수/함수 설명:**
- `PasswordEncoder`: 비밀번호를 안전하게 암호화하는 인터페이스
- `BCryptPasswordEncoder`: BCrypt 해시 함수를 사용한 구현체 (솔트 자동 생성)

```java
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // CSRF 보호 비활성화 (REST API용)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 모든 요청 허용 (개발 모드)
            );
        return http.build();
    }
}
```

**설명:**
- `SecurityFilterChain`: HTTP 요청에 대한 보안 필터 체인
- `csrf.disable()`: CSRF 토큰 검증 비활성화 (REST API는 일반적으로 CSRF 불필요)
- `anyRequest().permitAll()`: 모든 요청에 대해 인증 없이 접근 허용

---

### 3. DataInitializer.java (초기 데이터 생성)

```java
@Component                    // 스프링 빈으로 등록
@RequiredArgsConstructor      // final 필드에 대한 생성자 자동 생성 (Lombok)
@Slf4j                        // 로깅 기능 자동 주입 (Lombok)
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;  // 관리자 저장소 (의존성 주입)
    private final PasswordEncoder passwordEncoder;          // 비밀번호 암호화기
```

**변수 설명:**
- `adminUserRepository`: 관리자 사용자 테이블에 접근하는 JPA Repository
- `passwordEncoder`: 비밀번호를 암호화하는 객체

```java
    @Override
    public void run(String... args) throws Exception {
        // 관리자 계정이 없으면 기본 관리자 생성
        if (adminUserRepository.count() == 0) {    // DB에 관리자가 없으면
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin1234"));  // 비밀번호 암호화
            admin.setRole(AdminUser.Role.ADMIN);

            adminUserRepository.save(admin);       // DB에 저장
        }
    }
}
```

**개발 과정:**
1. `CommandLineRunner` 인터페이스 구현으로 앱 시작 시 자동 실행
2. `adminUserRepository.count() == 0`: 기존 관리자가 있는지 확인
3. `passwordEncoder.encode()`: 평문 비밀번호를 해시값으로 변환
4. `adminUserRepository.save()`: JPA를 통해 DB에 저장

---

### 4. PageController.java (페이지 라우팅)

```java
@Controller                   // MVC 컨트롤러 (뷰 반환)
public class PageController {

    @GetMapping("/")          // GET "/" 요청 처리
    public String index() {
        return "redirect:/index.html";  // 정적 파일로 리다이렉트
    }

    @GetMapping("/news")      // GET "/news" 요청 처리
    public String newsInsights() {
        return "news-insights";         // templates/news-insights.html 반환
    }

    @GetMapping("/portfolio") // GET "/portfolio" 요청 처리
    public String portfolioComparison() {
        return "portfolio-comparison";  // templates/portfolio-comparison.html 반환
    }
}
```

**어노테이션 설명:**
- `@Controller`: 뷰를 반환하는 컨트롤러 (vs `@RestController`: JSON 반환)
- `@GetMapping`: HTTP GET 메서드 매핑

---

### 5. PortfolioApiController.java (포트폴리오 API)

```java
@RestController                          // REST API 컨트롤러 (JSON 자동 변환)
@RequestMapping("/api/portfolio")        // 기본 URL 경로 설정
@RequiredArgsConstructor                 // 생성자 주입
@Log4j2                                  // 로깅 (Log4j2)
public class PortfolioApiController {

    private final InvestorComparisonService investorComparisonService;  // 서비스 주입
```

**어노테이션 설명:**
- `@RestController`: `@Controller` + `@ResponseBody` (JSON 자동 직렬화)
- `@RequestMapping("/api/portfolio")`: 모든 메서드의 기본 경로

```java
    @PostMapping("/compare")  // POST "/api/portfolio/compare"
    public List<InvestorComparisonDTO> compareInvestors(
            @RequestBody InvestorComparisonRequest request) {  // JSON 요청 본문 파싱
        log.info("투자자 비교 요청: " + request.getInvestors());
        return investorComparisonService.compareInvestors(request.getInvestors());
    }
```

**파라미터 설명:**
- `@RequestBody`: HTTP 요청 본문(JSON)을 Java 객체로 변환
- `request.getInvestors()`: 비교할 투자자 ID 목록 추출

```java
    @PostMapping("/search")   // 투자자 검색
    public InvestorSearchResponse searchInvestor(
            @RequestBody InvestorSearchRequest request) {
        return investorComparisonService.searchInvestor(request.getName());
    }

    @PostMapping("/recommend") // AI 포트폴리오 추천
    public PortfolioRecommendationResponse generatePortfolioRecommendation(
            @RequestBody InvestorComparisonRequest request) {
        return investorComparisonService.generatePortfolioRecommendation(
            request.getInvestors());
    }
}
```

---

### 6. StockChartController.java (주식 차트 API)

```java
@Controller                              // 뷰 + API 혼합 컨트롤러
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j                                   // 로깅 (SLF4J)
public class StockChartController {

    private final StockService stockService;  // 주식 데이터 서비스
```

```java
    @GetMapping("/chart")     // 차트 페이지 뷰 반환
    public String chartPage() {
        return "stock-chart";  // templates/stock-chart.html
    }

    @GetMapping("/api/info/{ticker}")     // 종목 정보 API
    public ResponseEntity<StockInfoDTO> getStockInfo(
            @PathVariable String ticker) {  // URL 경로 변수

        try {
            StockInfoDTO stockInfo = stockService.getStockInfo(ticker);

            if (stockInfo == null) {
                return ResponseEntity.notFound().build();  // 404 응답
            }

            return ResponseEntity.ok(stockInfo);           // 200 응답 + 데이터

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();  // 500 응답
        }
    }
```

**변수/어노테이션 설명:**
- `@PathVariable`: URL의 `{ticker}` 부분을 변수로 추출
- `ResponseEntity<T>`: HTTP 응답 상태코드 + 본문을 함께 반환
- `ResponseEntity.ok()`: 200 OK 응답
- `ResponseEntity.notFound()`: 404 Not Found 응답

```java
    @GetMapping("/api/candles/{ticker}")  // 캔들 데이터 API
    public ResponseEntity<List<StockCandleDTO>> getCandleData(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "D") String timeframe,  // 쿼리 파라미터
            @RequestParam(defaultValue = "120") int count) {

        List<StockCandleDTO> candles = stockService.getCandleData(
            ticker, timeframe, count);
        return ResponseEntity.ok(candles);
    }
```

**파라미터 설명:**
- `@RequestParam`: URL 쿼리 파라미터 (`?timeframe=D&count=120`)
- `defaultValue`: 파라미터가 없을 때 기본값

---

### 7. DailyNewsController.java (뉴스 API)

```java
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class DailyNewsController {

    private final DailyNewsService dailyNewsService;       // 뉴스 비즈니스 로직
    private final NewsSchedulerService schedulerService;   // 크롤링 스케줄러
```

```java
    @GetMapping("/daily")     // 24시간 이내 뉴스 조회
    public ResponseEntity<List<DailyNewsDTO>> getDailyNews() {
        List<DailyNewsDTO> newsList = dailyNewsService.getDailyNews();
        return ResponseEntity.ok(newsList);
    }

    @GetMapping("/daily/page") // 페이징된 뉴스 조회
    public ResponseEntity<Page<DailyNewsDTO>> getDailyNewsPage(
            @RequestParam(defaultValue = "0") int page,    // 페이지 번호 (0부터)
            @RequestParam(defaultValue = "20") int size) { // 페이지 크기

        Page<DailyNewsDTO> newsPage = dailyNewsService.getDailyNews(page, size);
        return ResponseEntity.ok(newsPage);
    }
```

**변수 설명:**
- `Page<T>`: Spring Data의 페이징 결과 객체
- `page`: 0부터 시작하는 페이지 인덱스
- `size`: 한 페이지당 항목 수

```java
    @GetMapping("/{newsId}")  // 뉴스 상세 조회 (조회수 증가)
    public ResponseEntity<DailyNewsDTO> getNewsDetail(
            @PathVariable Long newsId) {
        DailyNewsDTO newsDTO = dailyNewsService.getNewsDetail(newsId);
        return ResponseEntity.ok(newsDTO);
    }

    @PostMapping("/{newsId}/comments")  // 댓글 작성
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long newsId,
            @RequestBody CommentDTO commentDTO) {

        commentDTO.setNewsId(newsId);  // 뉴스 ID 설정
        CommentDTO savedComment = dailyNewsService.addComment(commentDTO);
        return ResponseEntity.ok(savedComment);
    }
```

---

### 8. InvestorComparisonService.java (투자자 비교 서비스)

```java
@Service                      // 비즈니스 로직 계층
@RequiredArgsConstructor
@Log4j2
public class InvestorComparisonService {

    private final GPTService gptService;  // GPT API 서비스 주입
```

```java
    public List<InvestorComparisonDTO> compareInvestors(List<String> investorIds) {
        List<InvestorComparisonDTO> comparisons = new ArrayList<>();

        for (String investorId : investorIds) {
            InvestorComparisonDTO comparison;

            // 커스텀 투자자 확인 (custom_로 시작하는 ID)
            if (investorId.startsWith("custom_")) {
                comparison = generateCustomInvestorPhilosophy(investorId);
            } else {
                comparison = getInvestorPhilosophy(investorId);  // 미리 정의된 투자자
            }

            // GPT API로 인사이트 생성
            String insights = gptService.analyzeInvestorPhilosophy(investorId);
            comparison.setInsights(insights);

            comparisons.add(comparison);
        }

        return comparisons;
    }
```

**개발 과정:**
1. 투자자 ID 목록을 반복 처리
2. `custom_`으로 시작하면 커스텀 투자자로 처리
3. 그렇지 않으면 미리 정의된 투자자 데이터 사용
4. GPT API로 추가 인사이트 생성

```java
    private InvestorComparisonDTO getInvestorPhilosophy(String investorId) {
        return switch (investorId) {  // Java 17 switch 표현식
            case "wood" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("혁신 기술")
                                    .percentage(95)
                                    .build(),
                            // ... 더 많은 항목
                    ))
                    .insights("캐시 우드는 AI, 블록체인 등 혁신 기술에 집중 투자합니다.")
                    .build();

            case "buffett" -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(Arrays.asList(
                            InvestorComparisonDTO.PhilosophyItem.builder()
                                    .category("가치투자")
                                    .percentage(85)
                                    .build(),
                            // ... 더 많은 항목
                    ))
                    .insights("워렌 버핏은 내재가치보다 낮은 가격의 우량주를 장기 보유합니다.")
                    .build();

            // ... 다른 투자자들

            default -> InvestorComparisonDTO.builder()
                    .investorId(investorId)
                    .philosophy(new ArrayList<>())
                    .insights("알 수 없는 투자자입니다.")
                    .build();
        };
    }
```

**switch 표현식 설명:**
- Java 17의 새로운 switch 문법
- `->`: 화살표 연산자 (break 불필요)
- 표현식으로 값을 반환 가능

---

### 9. GPTService.java (OpenAI API 연동)

```java
@Service
@Log4j2
public class GPTService {

    @Value("${openai.api.key:your-api-key-here}")  // 설정 파일에서 API 키 주입
    private String apiKey;
```

**변수 설명:**
- `@Value`: application.properties의 값을 주입
- `${...}`: 프로퍼티 키
- `:your-api-key-here`: 기본값 (키가 없을 때)

```java
    public String translateAndSummarizeNews(String newsText) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

            List<ChatMessage> messages = new ArrayList<>();

            // 시스템 메시지: AI의 역할 정의
            messages.add(new ChatMessage("system",
                "당신은 금융 뉴스 번역 및 요약 전문가입니다..."));

            // 사용자 메시지: 실제 요청
            messages.add(new ChatMessage("user", String.format(
                "다음 뉴스를 번역하고 분석해주세요:\n\n%s\n\n" +
                "응답 형식:\n제목: [번역된 제목]\n...", newsText)));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")    // 사용할 모델
                    .messages(messages)        // 대화 메시지
                    .temperature(0.7)          // 창의성 (0~2, 높을수록 다양한 응답)
                    .maxTokens(500)            // 최대 토큰 수
                    .build();

            String response = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            service.shutdownExecutor();  // 리소스 정리
            return response;

        } catch (Exception e) {
            log.error("GPT API 호출 오류", e);
            return null;
        }
    }
```

**개발 과정:**
1. `OpenAiService` 객체 생성 (API 키, 타임아웃 설정)
2. `ChatMessage` 리스트 구성 (system: 역할, user: 요청)
3. `ChatCompletionRequest` 빌더로 요청 객체 생성
4. API 호출 및 응답 추출
5. 리소스 정리 (`shutdownExecutor`)

```java
    public String generatePortfolioRecommendation(List<String> investorIds) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(90));

            // 투자자 이름 리스트 생성
            StringBuilder investorNames = new StringBuilder();
            for (String id : investorIds) {
                investorNames.append(getInvestorName(id)).append(", ");
            }

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system",
                "당신은 세계적인 투자 전문가입니다..."));
            messages.add(new ChatMessage("user", String.format(
                "다음 4명의 투자자 철학을 각각 25%%씩 반영한 추천 포트폴리오를 만들어주세요:\n\n" +
                "투자자: %s\n\n...", investorsStr)));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-4")           // GPT-4 모델 사용 (더 정교한 분석)
                    .messages(messages)
                    .temperature(0.8)
                    .maxTokens(1500)
                    .build();

            // ...
        }
    }
```

```java
    private String getInvestorName(String investorId) {
        return switch (investorId) {
            case "wood" -> "캐시 우드 (Cathie Wood)";
            case "buffett" -> "워렌 버핏 (Warren Buffett)";
            case "lynch" -> "피터 린치 (Peter Lynch)";
            case "dalio" -> "레이 달리오 (Ray Dalio)";
            // ... 다른 투자자들
            default -> investorId;  // 커스텀 투자자는 ID 그대로 반환
        };
    }
}
```

---

### 10. DailyNewsService.java (뉴스 비즈니스 로직)

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // 읽기 전용 트랜잭션 (성능 최적화)
public class DailyNewsService {

    private final NewsRepository newsRepository;       // 뉴스 저장소
    private final CommentRepository commentRepository; // 댓글 저장소
```

**어노테이션 설명:**
- `@Transactional(readOnly = true)`: 기본적으로 읽기 전용 (쓰기 작업은 개별 설정)
- 읽기 전용 트랜잭션은 플러시 생략으로 성능 향상

```java
    public List<DailyNewsDTO> getDailyNews() {
        List<News> newsList = newsRepository.findDailyNews();  // DB 조회

        return newsList.stream()
                .map(news -> {
                    Long commentCount = commentRepository.countByNewsId(news.getId());
                    return DailyNewsDTO.fromEntity(news, commentCount);  // Entity -> DTO 변환
                })
                .collect(Collectors.toList());
    }
```

**Stream API 설명:**
- `stream()`: 리스트를 스트림으로 변환
- `map()`: 각 요소를 변환
- `collect()`: 결과를 다시 리스트로 수집

```java
    @Transactional  // 쓰기 작업이므로 readOnly=false
    public DailyNewsDTO getNewsDetail(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + newsId));

        // 조회수 증가
        news.incrementViewCount();
        newsRepository.save(news);

        Long commentCount = commentRepository.countByNewsId(newsId);
        return DailyNewsDTO.fromEntity(news, commentCount);
    }
```

**개발 과정:**
1. `findById()`: ID로 엔티티 조회 (Optional 반환)
2. `orElseThrow()`: 없으면 예외 발생
3. `incrementViewCount()`: 조회수 1 증가
4. `save()`: 변경사항 DB에 저장

```java
    @Transactional
    public void deleteNews(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + newsId));

        news.softDelete();  // 실제 삭제가 아닌 소프트 삭제
        newsRepository.save(news);
    }
```

**소프트 삭제:**
- 데이터를 실제로 삭제하지 않고 `isDeleted` 플래그만 변경
- 데이터 복구 가능, 감사 추적 용이

---

### 11. NewsScrapingService.java (뉴스 크롤링)

```java
@Service
@RequiredArgsConstructor
@Log4j2
public class NewsScrapingService {

    private final GPTService gptService;
```

```java
    public List<NewsDTO> scrapeYahooFinanceNews(String category) {
        List<NewsDTO> newsList = new ArrayList<>();

        try {
            // 카테고리에 따른 URL 결정
            String url = category.equals("hot-topics")
                ? "https://finance.yahoo.com/topic/stock-market-news/"
                : "https://finance.yahoo.com/";

            // Jsoup으로 웹 페이지 가져오기
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")  // 브라우저처럼 위장
                    .timeout(10000)  // 10초 타임아웃
                    .get();          // GET 요청

            // CSS 선택자로 뉴스 요소 선택
            Elements newsElements = doc.select("div.Ov\\(h\\) > h3 > a, article h3 a");

            int count = 0;
            for (Element newsElement : newsElements) {
                if (count >= 10) break;  // 최대 10개

                String title = newsElement.text();            // 제목 텍스트
                String newsUrl = newsElement.attr("abs:href"); // 절대 URL

                if (title.isEmpty() || newsUrl.isEmpty()) continue;

                // 뉴스 본문 가져오기
                String content = fetchNewsContent(newsUrl);

                // GPT로 번역 및 요약
                String gptResponse = gptService.translateAndSummarizeNews(
                        "Title: " + title + "\n\nContent: " + content.substring(0, 500));

                NewsDTO newsDTO = parseGPTResponse(gptResponse, newsUrl, category);
                if (newsDTO != null) {
                    newsList.add(newsDTO);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("뉴스 스크래핑 오류", e);
        }

        // 실제 데이터가 없으면 샘플 데이터 반환
        if (newsList.isEmpty()) {
            newsList = getSampleNews(category);
        }

        return newsList;
    }
```

**Jsoup 메서드 설명:**
- `Jsoup.connect(url)`: URL에 연결
- `.userAgent()`: User-Agent 헤더 설정 (봇 차단 우회)
- `.timeout()`: 연결 타임아웃 설정
- `.get()`: HTTP GET 요청 실행
- `.select()`: CSS 선택자로 요소 찾기
- `.text()`: 요소의 텍스트 추출
- `.attr("abs:href")`: 절대 URL 속성 추출

---

### 12. Entity 클래스들

#### News.java (뉴스 엔티티)

```java
@Entity                       // JPA 엔티티
@Table(name = "news", indexes = {
        @Index(name = "idx_status_created_at", columnList = "status, created_at DESC"),
        @Index(name = "idx_view_count", columnList = "view_count DESC"),
        @Index(name = "idx_url", columnList = "url", unique = true)
})
@Getter @Setter               // Lombok getter/setter 자동 생성
@NoArgsConstructor            // 기본 생성자
@AllArgsConstructor           // 전체 필드 생성자
@Builder                      // 빌더 패턴 지원
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 자동 증가 ID
    private Long id;

    @Column(nullable = false, length = 500)  // NOT NULL, 최대 500자
    private String title;

    @Column(columnDefinition = "TEXT")       // TEXT 타입 (긴 문자열)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;                   // GPT 요약

    @Column(nullable = false, unique = true, length = 1000)  // 유니크 제약
    private String url;

    @Column(length = 100)
    private String source;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp                        // 생성 시 자동으로 현재 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "view_count")
    @Builder.Default                          // 빌더 사용 시 기본값
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)             // 열거형을 문자열로 저장
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NewsStatus status = NewsStatus.DAILY;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
```

**어노테이션 설명:**
- `@Entity`: JPA 엔티티로 지정 (테이블과 매핑)
- `@Table`: 테이블 이름 및 인덱스 정의
- `@Index`: 데이터베이스 인덱스 생성 (검색 성능 향상)
- `@Id`: 기본 키
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: 자동 증가
- `@Column`: 컬럼 속성 정의
- `@CreationTimestamp`: 생성 시 자동 타임스탬프
- `@Enumerated(EnumType.STRING)`: Enum을 문자열로 저장 (가독성)

```java
    public void incrementViewCount() {
        this.viewCount++;
    }

    public boolean isOver24Hours() {
        return createdAt.plusHours(24).isBefore(LocalDateTime.now());
    }

    public void archiveNews() {
        this.status = NewsStatus.ARCHIVE;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public enum NewsStatus {
        DAILY,    // 24시간 이내
        ARCHIVE   // 24시간 이상
    }
}
```

---

#### Comment.java (댓글 엔티티)

```java
@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_news_id_created_at", columnList = "news_id, created_at DESC")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)      // 다대일 관계, 지연 로딩
    @JoinColumn(name = "news_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comment_news"))
    private News news;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    public void softDelete() {
        this.isDeleted = true;
    }
}
```

**연관관계 설명:**
- `@ManyToOne`: 다대일 관계 (여러 댓글 → 하나의 뉴스)
- `fetch = FetchType.LAZY`: 지연 로딩 (실제 사용 시점에 쿼리)
- `@JoinColumn`: 외래 키 컬럼 지정
- `@ForeignKey`: 외래 키 제약조건 이름

---

### 13. Repository 인터페이스

#### NewsRepository.java

```java
public interface NewsRepository extends JpaRepository<News, Long> {

    // URL로 뉴스 조회 (중복 체크용)
    Optional<News> findByUrl(String url);

    // URL 존재 여부 확인
    boolean existsByUrl(String url);

    // JPQL 쿼리: 데일리 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.status = 'DAILY' AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<News> findDailyNews();

    // 페이징된 데일리 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.status = 'DAILY' AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<News> findDailyNews(Pageable pageable);

    // 금주의 뉴스 (조회수 TOP N)
    @Query("SELECT n FROM News n WHERE n.isDeleted = false AND n.createdAt >= :weekAgo ORDER BY n.viewCount DESC")
    List<News> findTopNewsByViewCount(@Param("weekAgo") LocalDateTime weekAgo, Pageable pageable);

    // 제목으로 검색
    @Query("SELECT n FROM News n WHERE n.isDeleted = false AND n.title LIKE %:keyword% ORDER BY n.createdAt DESC")
    Page<News> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
}
```

**JPA Repository 설명:**
- `JpaRepository<News, Long>`: News 엔티티, Long 타입 ID
- 메서드 이름 규칙으로 쿼리 자동 생성: `findByUrl` → `WHERE url = ?`
- `@Query`: JPQL 직접 작성
- `@Param`: 쿼리 파라미터 바인딩
- `Pageable`: 페이징 정보 (페이지 번호, 크기, 정렬)

---

### 14. DTO 클래스들

#### InvestorComparisonDTO.java

```java
@Data                         // @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorComparisonDTO {

    private String investorId;                    // 투자자 고유 ID
    private List<PhilosophyItem> philosophy;      // 투자 철학 목록
    private String insights;                      // GPT 인사이트

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhilosophyItem {          // 내부 클래스
        private String category;                  // 카테고리 (예: "가치투자")
        private int percentage;                   // 점수 (0-100)
    }
}
```

**DTO 역할:**
- Data Transfer Object: 계층 간 데이터 전송용 객체
- Entity와 분리하여 API 응답 형태를 자유롭게 구성
- 민감한 정보 노출 방지

#### DailyNewsDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyNewsDTO {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private String url;
    private String source;
    private String publishedAt;       // 포맷팅된 날짜 문자열
    private String createdAt;
    private Long viewCount;
    private String status;
    private Long commentCount;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Entity → DTO 변환 (정적 팩토리 메서드)
    public static DailyNewsDTO fromEntity(News news) {
        return DailyNewsDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .summary(news.getSummary())
                .url(news.getUrl())
                .source(news.getSource())
                .publishedAt(news.getPublishedAt() != null ?
                        news.getPublishedAt().format(FORMATTER) : null)
                .createdAt(news.getCreatedAt().format(FORMATTER))
                .viewCount(news.getViewCount())
                .status(news.getStatus().name())
                .build();
    }

    // Entity → DTO 변환 (댓글 개수 포함)
    public static DailyNewsDTO fromEntity(News news, Long commentCount) {
        DailyNewsDTO dto = fromEntity(news);
        dto.setCommentCount(commentCount);
        return dto;
    }

    // DTO → Entity 변환
    public News toEntity() {
        return News.builder()
                .title(this.title)
                .content(this.content)
                .summary(this.summary)
                .url(this.url)
                .source(this.source)
                .publishedAt(this.publishedAt != null ?
                        LocalDateTime.parse(this.publishedAt, FORMATTER) : null)
                .build();
    }
}
```

**변환 메서드 설명:**
- `fromEntity()`: Entity → DTO 변환 (정적 메서드)
- `toEntity()`: DTO → Entity 변환 (인스턴스 메서드)
- `DateTimeFormatter`: 날짜를 문자열로 포맷팅

---

## 프론트엔드 코드 상세 설명

### 1. portfolio-comparison.html

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">  <!-- Thymeleaf 네임스페이스 -->
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>포트폴리오 & 투자자 비교</title>
    <style>
        /* CSS 변수 대신 직접 값 사용 */
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Malgun Gothic', sans-serif;
            line-height: 1.6;
            color: #333;
            background: #f8f9fa;
        }
```

**CSS 주요 클래스 설명:**

```css
.investor-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);  /* 그라데이션 배경 */
    color: white;
    padding: 20px;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;    /* 부드러운 전환 효과 */
    position: relative;
}

.investor-card:hover {
    transform: translateY(-5px);  /* 호버 시 위로 이동 */
    box-shadow: 0 12px 24px rgba(102, 126, 234, 0.3);  /* 그림자 효과 */
}

.investor-card.selected {
    border: 3px solid #fbbf24;  /* 선택된 카드 강조 */
    box-shadow: 0 0 20px rgba(251, 191, 36, 0.5);
}
```

```html
<body>
    <!-- 네비게이션 바 -->
    <nav class="navbar">
        <div class="navbar-content">
            <div class="navbar-logo" onclick="location.href='/'">InvestHub</div>
            <div class="navbar-buttons">
                <button class="btn-nav" onclick="location.href='/'">홈으로</button>
                <button class="btn-nav" onclick="location.href='/news'">뉴스 인사이트</button>
            </div>
        </div>
    </nav>

    <!-- 투자자 선택 섹션 -->
    <div class="investor-selector">
        <h2>비교할 투자자 선택 (최대 4명)</h2>
        <div class="investor-grid" id="selected-investors">
            <!-- JavaScript로 동적 생성 -->
        </div>
    </div>

    <!-- JavaScript 모듈 로드 -->
    <script type="module" src="/js/portfolio-comparison/main.js"></script>
</body>
```

**`type="module"` 설명:**
- ES6 모듈 시스템 사용
- `import/export` 구문 사용 가능
- 자동 strict mode

---

### 2. main.js (메인 진입점)

```javascript
/**
 * 포트폴리오 비교 페이지 메인 진입점
 */
import { InvestorManager } from './investor-manager.js';  // 투자자 관리
import { APIService } from './api-service.js';            // API 통신
import { UIController } from './ui-controller.js';        // UI 제어
import { AutocompleteHandler } from './autocomplete-handler.js';  // 자동완성
```

**ES6 import 설명:**
- 상대 경로로 모듈 가져오기
- `{ InvestorManager }`: Named export 가져오기
- 각 파일은 독립적인 스코프

```javascript
class PortfolioComparisonApp {
    constructor() {
        // 모듈 초기화 - 의존성 주입 패턴
        this.investorManager = new InvestorManager();
        this.apiService = new APIService();
        this.uiController = new UIController(this.investorManager, this.apiService);
        this.autocompleteHandler = new AutocompleteHandler(this.investorManager, this.apiService);
    }

    /**
     * 애플리케이션 초기화
     */
    init() {
        // 1. 기본 투자자 설정 (4명)
        this.investorManager.setDefaultInvestors();

        // 2. UI 초기 렌더링
        this.uiController.updateSelectedInvestors();
        this.uiController.displayComparison();
        this.uiController.updateGenerateButton();

        // 3. 자동완성 초기화
        this.autocompleteHandler.init();

        // 4. 이벤트 리스너 등록
        this.attachEventListeners();
    }
```

**클래스 구조 설명:**
- `constructor()`: 생성자, 인스턴스 생성 시 실행
- `this.xxx`: 인스턴스 속성
- 의존성 주입으로 모듈 간 결합도 낮춤

```javascript
    /**
     * 이벤트 리스너 등록 - 이벤트 위임 패턴
     */
    attachEventListeners() {
        // 투자자 제거 버튼 (이벤트 위임)
        document.getElementById('selected-investors').addEventListener('click', (event) => {
            if (event.target.classList.contains('remove-btn')) {
                const investorId = event.target.dataset.investorId;  // data-* 속성
                this.removeInvestor(investorId);
            }
        });

        // 모달 닫기 버튼
        document.querySelector('.close').addEventListener('click', () => {
            this.uiController.closeModal();
        });

        // 포트폴리오 생성 버튼
        document.getElementById('generateBtn').addEventListener('click', () => {
            this.generatePortfolio();
        });
    }
```

**이벤트 위임 설명:**
- 부모 요소에 리스너를 등록하고, 자식 요소의 이벤트를 처리
- 동적으로 생성된 요소도 처리 가능
- `event.target`: 실제 클릭된 요소
- `dataset.investorId`: `data-investor-id` 속성 값

```javascript
    /**
     * 투자자 추가
     */
    addInvestor(investorId) {
        const result = this.investorManager.addInvestor(investorId);

        if (!result.success) {
            alert(result.message);  // 실패 시 알림
            return;
        }

        // 성공 시 UI 업데이트
        this.uiController.updateSelectedInvestors();
        this.uiController.displayComparison();
        this.uiController.updateGenerateButton();
        this.uiController.closeModal();
    }

    /**
     * 포트폴리오 생성 (비동기)
     */
    async generatePortfolio() {
        await this.uiController.generatePortfolio();  // await으로 완료 대기
    }
}

// DOM 로드 완료 후 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    const app = new PortfolioComparisonApp();
    app.init();
});
```

**async/await 설명:**
- `async`: 비동기 함수 선언
- `await`: Promise 완료까지 대기
- DOMContentLoaded: HTML 파싱 완료 시점

---

### 3. investor-manager.js (투자자 데이터 관리)

```javascript
/**
 * 투자자 데이터 및 선택 관리 모듈
 */
export class InvestorManager {
    constructor() {
        this.selectedInvestors = [];     // 선택된 투자자 ID 배열
        this.maxInvestors = 4;           // 최대 선택 가능 수
        this.customInvestors = [];       // 커스텀 투자자 배열

        // 사용 가능한 투자자 목록 (하드코딩 데이터)
        this.availableInvestors = [
            {
                id: 'wood',
                name: '캐시 우드',
                style: '파괴적 혁신 투자',
                description: 'ARK Invest 창립자, AI와 블록체인 등 혁신 기술 중심 투자'
            },
            {
                id: 'buffett',
                name: '워렌 버핏',
                style: '가치투자',
                description: '장기 가치투자의 대가, 내재가치보다 낮은 가격의 우량주를 선호'
            },
            // ... 더 많은 투자자
        ];
    }
```

```javascript
    /**
     * 기본 투자자 설정
     */
    setDefaultInvestors() {
        this.selectedInvestors = ['wood', 'soros', 'thiel', 'fink'];
    }

    /**
     * 투자자 추가 - 유효성 검사 포함
     */
    addInvestor(investorId) {
        // 최대 인원 체크
        if (this.selectedInvestors.length >= this.maxInvestors) {
            return { success: false, message: '최대 4명까지만 비교할 수 있습니다.' };
        }

        // 중복 체크
        if (this.selectedInvestors.includes(investorId)) {
            return { success: false, message: '이미 선택된 투자자입니다.' };
        }

        this.selectedInvestors.push(investorId);  // 배열에 추가
        return { success: true };
    }

    /**
     * 투자자 제거
     */
    removeInvestor(investorId) {
        // filter: 조건에 맞지 않는 요소만 남김
        this.selectedInvestors = this.selectedInvestors.filter(id => id !== investorId);
        return { success: true };
    }
```

```javascript
    /**
     * 커스텀 투자자 추가 (검색으로 추가된 투자자)
     */
    addCustomInvestor(investorData) {
        const customId = 'custom_' + Date.now();  // 고유 ID 생성 (타임스탬프)
        const customInvestor = {
            id: customId,
            name: investorData.name,
            style: investorData.style || '커스텀 투자자',
            description: investorData.description || '검색으로 추가된 투자자'
        };

        this.customInvestors.push(customInvestor);
        return customInvestor;
    }

    /**
     * ID로 투자자 찾기
     */
    getInvestorById(investorId) {
        // 먼저 기본 목록에서 찾기
        let investor = this.availableInvestors.find(inv => inv.id === investorId);

        // 없으면 커스텀 목록에서 찾기
        if (!investor) {
            investor = this.customInvestors.find(inv => inv.id === investorId);
        }
        return investor;
    }

    /**
     * 투자자 필터링 (검색)
     */
    filterInvestors(searchTerm) {
        if (!searchTerm) return [];

        const term = searchTerm.toLowerCase();  // 소문자로 변환
        return this.availableInvestors.filter(investor =>
            investor.name.toLowerCase().includes(term) ||
            investor.style.toLowerCase().includes(term) ||
            investor.description.toLowerCase().includes(term)
        );
    }

    /**
     * 포트폴리오 생성 가능 여부
     */
    canGeneratePortfolio() {
        return this.selectedInvestors.length === this.maxInvestors;  // 정확히 4명
    }
}
```

---

### 4. api-service.js (API 통신)

```javascript
/**
 * API 통신 담당 모듈
 */
export class APIService {
    constructor() {
        this.baseUrl = '';  // 같은 도메인이므로 빈 문자열
    }

    /**
     * 투자자 비교 데이터 가져오기
     */
    async fetchComparison(investorIds) {
        try {
            const response = await fetch('/api/portfolio/compare', {
                method: 'POST',                           // HTTP 메서드
                headers: {
                    'Content-Type': 'application/json',   // 요청 본문 타입
                },
                body: JSON.stringify({ investors: investorIds })  // JSON 직렬화
            });

            if (!response.ok) {
                throw new Error('비교 데이터를 가져오는데 실패했습니다.');
            }

            return await response.json();  // JSON 파싱
        } catch (error) {
            console.error('비교 데이터 로딩 오류:', error);
            throw error;  // 에러 전파
        }
    }
```

**Fetch API 설명:**
- `fetch()`: HTTP 요청 수행 (Promise 반환)
- `method`: HTTP 메서드 (GET, POST, PUT, DELETE 등)
- `headers`: 요청 헤더
- `body`: 요청 본문 (문자열)
- `JSON.stringify()`: 객체 → JSON 문자열
- `response.json()`: 응답 본문을 JSON으로 파싱

```javascript
    /**
     * AI 포트폴리오 추천 생성
     */
    async generatePortfolio(investorIds) {
        try {
            const response = await fetch('/api/portfolio/recommend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ investors: investorIds })
            });

            if (!response.ok) {
                throw new Error('포트폴리오 생성에 실패했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('포트폴리오 생성 오류:', error);
            throw error;
        }
    }

    /**
     * 투자자 검색
     */
    async searchInvestor(investorName) {
        try {
            const response = await fetch('/api/portfolio/search', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: investorName })
            });

            if (!response.ok) {
                throw new Error('투자자 정보를 가져올 수 없습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('투자자 검색 오류:', error);
            throw error;
        }
    }
}
```

---

### 5. stock-chart.html (주식 차트)

```javascript
let chart = null;                // 차트 객체
let candlestickSeries = null;    // 캔들스틱 시리즈
let sma20Series = null;          // 20일 이동평균선
let sma60Series = null;          // 60일 이동평균선
let sma120Series = null;         // 120일 이동평균선
let currentTicker = null;        // 현재 선택된 티커
let currentTimeframe = 'D';      // 현재 시간프레임 (D=일봉)

/**
 * 차트 초기화 - TradingView Lightweight Charts 사용
 */
function initChart() {
    const chartContainer = document.getElementById('chart-container');
    chartContainer.innerHTML = '';  // 기존 내용 제거

    // 차트 생성
    chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 600,
        layout: {
            background: { color: '#ffffff' },
            textColor: '#333',
        },
        grid: {
            vertLines: { color: '#f0f0f0' },
            horzLines: { color: '#f0f0f0' },
        },
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,  // 십자선 모드
        },
        rightPriceScale: {
            borderColor: '#e0e0e0',
        },
        timeScale: {
            borderColor: '#e0e0e0',
            timeVisible: true,
        },
    });
```

**TradingView Lightweight Charts 설명:**
- 금융 차트 전문 라이브러리
- `createChart()`: 차트 인스턴스 생성
- `CrosshairMode`: 십자선 동작 모드

```javascript
    // 캔들스틱 시리즈 추가
    candlestickSeries = chart.addCandlestickSeries({
        upColor: '#ef5350',       // 상승 캔들 색상 (빨강)
        downColor: '#26a69a',     // 하락 캔들 색상 (초록)
        borderVisible: false,
        wickUpColor: '#ef5350',   // 상승 캔들 심지 색상
        wickDownColor: '#26a69a', // 하락 캔들 심지 색상
    });

    // 이동평균선 시리즈 추가
    sma20Series = chart.addLineSeries({
        color: '#2962FF',         // 파란색
        lineWidth: 2,
        title: 'SMA 20',
    });

    sma60Series = chart.addLineSeries({
        color: '#FF6D00',         // 주황색
        lineWidth: 2,
        title: 'SMA 60',
    });

    // 반응형 크기 조정
    window.addEventListener('resize', () => {
        chart.applyOptions({
            width: chartContainer.clientWidth,
        });
    });
}
```

```javascript
/**
 * 종목 로드 - 비동기 함수
 */
async function loadStock(ticker) {
    currentTicker = ticker.toUpperCase();  // 대문자로 변환
    document.getElementById('ticker-input').value = currentTicker;

    try {
        // 1. 종목 정보 API 호출
        const infoResponse = await fetch(`/stock/api/info/${currentTicker}`);
        if (!infoResponse.ok) {
            throw new Error('종목을 찾을 수 없습니다.');
        }
        const stockInfo = await infoResponse.json();
        displayStockInfo(stockInfo);

        // 2. 차트 데이터 로드
        await loadChartData();

    } catch (error) {
        console.error('종목 로드 실패:', error);
        resetChart();  // 차트 변수 초기화
        document.getElementById('chart-container').innerHTML = `
            <div class="error">${error.message}</div>
        `;
    }
}
```

**템플릿 리터럴 설명:**
- 백틱(`)으로 감싸는 문자열
- `${...}`: 변수 삽입
- 여러 줄 문자열 지원

```javascript
/**
 * 차트 데이터 로드
 */
async function loadChartData() {
    if (!currentTicker) return;

    try {
        const response = await fetch(
            `/stock/api/candles/${currentTicker}?timeframe=${currentTimeframe}&count=120`
        );

        const candles = await response.json();

        // 차트가 없으면 초기화
        if (!chart || !candlestickSeries) {
            initChart();
        }

        // 캔들 데이터 변환 - 배열의 map 메서드
        const candleData = candles.map(c => ({
            time: c.date,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close,
        }));

        // 이동평균 데이터 변환 - filter로 null 제거
        const sma20Data = candles
            .filter(c => c.sma20)      // sma20이 있는 것만
            .map(c => ({ time: c.date, value: c.sma20 }));

        // 차트에 데이터 설정
        candlestickSeries.setData(candleData);
        sma20Series.setData(sma20Data);
        sma60Series.setData(sma60Data);
        sma120Series.setData(sma120Data);

        chart.timeScale().fitContent();  // 전체 데이터가 보이도록 조정

    } catch (error) {
        console.error('차트 로드 실패:', error);
    }
}
```

**배열 메서드 설명:**
- `map()`: 각 요소를 변환하여 새 배열 반환
- `filter()`: 조건에 맞는 요소만 새 배열로 반환
- 화살표 함수 축약: `c => ({...})` (객체 반환 시 괄호 필요)

```javascript
/**
 * 숫자 포맷팅
 */
function formatNumber(num) {
    if (!num) return '0';
    return num.toLocaleString('ko-KR', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });
}

/**
 * 시가총액 포맷팅
 */
function formatMarketCap(num) {
    if (!num) return '0';
    if (num >= 1e12) return (num / 1e12).toFixed(1) + '조';  // 1조 이상
    if (num >= 1e8) return (num / 1e8).toFixed(0) + '억';    // 1억 이상
    return formatNumber(num);
}

/**
 * 이동평균선 토글
 */
function toggleSMA(period) {
    if (!chart) return;

    const isChecked = document.getElementById(`sma${period}-toggle`).checked;

    switch(period) {
        case 20:
            sma20Series.applyOptions({ visible: isChecked });  // 시리즈 표시/숨김
            break;
        case 60:
            sma60Series.applyOptions({ visible: isChecked });
            break;
        case 120:
            sma120Series.applyOptions({ visible: isChecked });
            break;
    }
}
```

---

## 데이터 흐름

### 1. 투자자 비교 흐름

```
[사용자] → [HTML 버튼 클릭]
    ↓
[main.js] addInvestor()
    ↓
[investor-manager.js] addInvestor() - 유효성 검사
    ↓
[api-service.js] fetchComparison() - POST /api/portfolio/compare
    ↓
[PortfolioApiController] compareInvestors()
    ↓
[InvestorComparisonService] compareInvestors()
    ↓
[GPTService] analyzeInvestorPhilosophy() - OpenAI API 호출
    ↓
[응답] InvestorComparisonDTO 리스트
    ↓
[ui-controller.js] 화면에 결과 렌더링
```

### 2. 주식 차트 흐름

```
[사용자] → [검색 버튼 클릭]
    ↓
[stock-chart.html] loadStock(ticker)
    ↓
fetch('/stock/api/info/{ticker}')
    ↓
[StockChartController] getStockInfo()
    ↓
[StockService] getStockInfo() - Yahoo Finance API 호출
    ↓
[응답] StockInfoDTO
    ↓
displayStockInfo() - 종목 정보 표시
    ↓
loadChartData() - 캔들 데이터 로드
    ↓
[TradingView Charts] 차트 렌더링
```

### 3. 뉴스 크롤링 흐름

```
[스케줄러 또는 수동 요청]
    ↓
[NewsSchedulerService] crawlNews()
    ↓
[NewsScrapingService] scrapeYahooFinanceNews()
    ↓
Jsoup으로 Yahoo Finance 크롤링
    ↓
[GPTService] translateAndSummarizeNews() - 번역/요약
    ↓
[NewsRepository] save() - DB 저장
    ↓
[DailyNewsController] getDailyNews() - 클라이언트 요청 시
    ↓
[응답] DailyNewsDTO 리스트
```

---

## 마무리

이 문서는 finance_DWpj1 프로젝트의 모든 주요 코드를 함수 단위, 변수 단위로 상세하게 설명합니다. 각 코드 블록의 역할, 어노테이션의 의미, 개발 과정에서의 선택 이유를 포함하였습니다.

### 핵심 설계 패턴
1. **계층 분리**: Controller → Service → Repository
2. **DTO 패턴**: Entity와 API 응답 분리
3. **의존성 주입**: `@RequiredArgsConstructor`로 생성자 주입
4. **이벤트 위임**: 동적 요소 처리
5. **모듈 패턴**: ES6 import/export로 코드 분리

---

## 추가 기능 상세 설명

### 15. SEC 13F 파일링 분석 시스템

**SEC 13F란?**
- 미국 SEC(증권거래위원회)에서 요구하는 분기별 보고서
- 1억 달러 이상 자산을 운용하는 기관투자자의 보유 종목 공개
- 워렌 버핏, 레이 달리오 등 유명 투자자들의 포트폴리오 추적 가능

#### SEC13FController.java (SEC 13F 데이터 수집 API)

```java
@RestController
@RequestMapping("/api/13f")
@RequiredArgsConstructor
@Slf4j
public class SEC13FController {

    private final SEC13FService sec13FService;

    @PostMapping("/start")
    public ResponseEntity<?> startCollection() {
        // 비동기로 전체 투자자 13F 데이터 수집 시작
        sec13FService.startAsyncCollection();
        return ResponseEntity.ok(createResponse(true, "13F 데이터 수집이 시작되었습니다"));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopCollection() {
        // 진행 중인 수집 중단
        sec13FService.stopCollection();
        return ResponseEntity.ok(createResponse(true, "수집 중단 요청이 전송되었습니다"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        // 현재 수집 상태 확인
        boolean isRunning = sec13FService.isCollecting();
        return ResponseEntity.ok(Map.of("isRunning", isRunning));
    }
}
```

**주요 기능:**
- `/start`: SEC 웹사이트에서 13F 보고서 XML 파일 크롤링 시작
- `/stop`: 실행 중인 크롤링 작업 중단
- `/status`: 현재 크롤링 진행 상태 확인
- 비동기 처리로 서버 블로킹 방지

#### Investor13FHolding.java (13F 보유 종목 엔티티)

```java
@Entity
@Table(name = "investor_13f_holding")
@Getter @Setter
@Builder
public class Investor13FHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String investorId;        // 투자자 고유 ID
    private String cusip;             // CUSIP 번호 (미국 증권 식별 번호)
    private String nameOfIssuer;      // 발행사 이름
    private String titleOfClass;      // 증권 클래스 (주식, 채권 등)
    private Long shares;              // 보유 주식 수
    private Long marketValue;         // 시장 가치 (달러)
    private String filingDate;        // 보고 날짜

    @Column(columnDefinition = "TEXT")
    private String votingAuthority;   // 의결권 정보 (JSON 형태)
}
```

**데이터 흐름:**
1. SEC EDGAR 웹사이트에서 13F XML 파일 다운로드
2. JAXB로 XML 파싱 (EdgarSubmission DTO로 변환)
3. CUSIP → 티커 변환 (CusipToTickerService)
4. DB에 저장 (Investor13FHolding)
5. 관리자 페이지에서 투자자별 포트폴리오 조회

---

### 16. OCR 포트폴리오 분석

**기능 개요:**
- 증권사 앱 스크린샷을 업로드하면 보유 종목 자동 추출
- Tesseract OCR + OpenCV 이미지 전처리
- 증권사별 커스텀 파서 지원 (토스증권, 키움증권 등)
- 추출한 포트폴리오를 유명 투자자와 비교하여 투자 스타일 분석

#### OcrService.java (OCR 핵심 서비스)

```java
@Service
@Slf4j
public class OcrService {

    // ThreadLocal: 멀티스레드 환경에서 각 스레드마다 별도의 Tesseract 인스턴스 생성
    private final ThreadLocal<Tesseract> tesseractThreadLocal;

    private final Map<BrokerType, OcrPreprocessor> preprocessors;  // 증권사별 전처리기
    private final Map<BrokerType, OcrParser> parsers;              // 증권사별 파서

    public List<PortfolioStock> extractPortfolioFromImage(
            MultipartFile imageFile, BrokerType brokerType) {

        // 1. 이미지 로드
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        // 2. 증권사별 전처리 (OpenCV 사용)
        OcrPreprocessor preprocessor = preprocessors.get(brokerType);
        BufferedImage preprocessedImage = preprocessor.preprocess(image);

        // 3. OCR 실행 (ThreadLocal에서 현재 스레드의 Tesseract 인스턴스 가져오기)
        String extractedText = tesseractThreadLocal.get().doOCR(preprocessedImage);

        // 4. 한국어 종목명 → 영어 티커 변환
        String mappedText = tickerMappingService.applyMappingToText(extractedText);

        // 5. 증권사별 파싱
        OcrParser parser = parsers.get(brokerType);
        List<PortfolioStock> stocks = parser.parse(mappedText);

        return stocks;
    }
}
```

**ThreadLocal 사용 이유:**
- Tesseract 객체는 thread-safe하지 않음
- 각 요청마다 새로운 Tesseract 인스턴스 생성 시 오버헤드 발생
- ThreadLocal로 스레드당 하나의 인스턴스만 재사용

#### TossPreprocessor.java (토스증권 전용 전처리)

```java
@Component
public class TossOpenCVPreprocessor implements OcrPreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage originalImage) {
        // BufferedImage → OpenCV Mat 변환
        Mat src = bufferedImageToMat(originalImage);

        // 1. 그레이스케일 변환
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // 2. 가우시안 블러 (노이즈 제거)
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // 3. 적응형 임계화 (배경 제거, 텍스트 강조)
        Mat threshold = new Mat();
        Imgproc.adaptiveThreshold(blurred, threshold, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 11, 2);

        // 4. 형태학 연산 (작은 노이즈 제거)
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Mat morph = new Mat();
        Imgproc.morphologyEx(threshold, morph, Imgproc.MORPH_CLOSE, kernel);

        // Mat → BufferedImage 변환
        return matToBufferedImage(morph);
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }
}
```

**OpenCV 전처리 단계:**
1. **그레이스케일**: 컬러 제거, OCR 정확도 향상
2. **가우시안 블러**: 이미지 노이즈 제거
3. **적응형 임계화**: 배경 하얗게, 텍스트 검게 변환
4. **형태학 연산**: 작은 점 노이즈 제거

#### TossOcrParser.java (토스증권 텍스트 파싱)

```java
@Component
public class TossOcrParser implements OcrParser {

    @Override
    public List<OcrService.PortfolioStock> parse(String extractedText) {
        List<OcrService.PortfolioStock> stocks = new ArrayList<>();
        String[] lines = extractedText.split("\\n");

        // 토스증권 형식: "AAPL 10 $1,500"
        Pattern pattern = Pattern.compile("([A-Z]{1,5})\\s+(\\d+(?:\\.\\d+)?)\\s+\\$?([\\d,]+)");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String ticker = matcher.group(1);      // AAPL
                double shares = Double.parseDouble(matcher.group(2));  // 10
                long amount = parseCurrency(matcher.group(3));         // 1500

                stocks.add(new PortfolioStock(ticker, shares, amount));
            }
        }

        return stocks;
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }
}
```

---

### 17. 커뮤니티 시스템 (Content)

#### ContentPostController.java (게시글 컨트롤러)

```java
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentPostController {

    private final ContentReviewService contentReviewService;
    private final ContentCommentService contentCommentService;

    @GetMapping("/post/{id}")
    public String detail(@PathVariable Long id,
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        Model model) {

        // 게시글 조회 (삭제된 글은 복구 페이지로 리다이렉트)
        ContentReview post = contentReviewService.getContentById(id);
        if (post.getIsDeleted()) {
            return "redirect:/content/restore-page/" + id;
        }

        // 조회수 증가 포함
        post = contentReviewService.getContentDetail(id);
        model.addAttribute("post", post);

        // 원댓글 + 대댓글 구조로 조립
        model.addAttribute("comments", contentCommentService.getCommentsWithReplies(id));

        // 평균 평점 계산
        double avgRating = contentCommentService.getAverageRating(id);
        model.addAttribute("avgRating", avgRating);

        return "content/post-detail";
    }

    @PostMapping("/comment")
    @ResponseBody
    public String writeComment(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestBody ContentCommentWriteDTO dto) {

        if (user == null) return "NOT_LOGIN";

        try {
            contentCommentService.write(user.getId(), user.getNickname(), dto);
            return "SUCCESS";
        } catch (IllegalArgumentException e) {
            return "NO_RATING";  // 평점 없음
        }
    }
}
```

#### ContentComment.java (댓글 엔티티 - 대댓글 지원)

```java
@Entity
@Getter @Setter
public class ContentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;              // 게시글 ID
    private Long userId;              // 작성자 ID
    private String writer;            // 작성자 닉네임
    private String content;           // 댓글 내용

    @Column
    private Double rating;            // 평점 (0.0 ~ 5.0, 0.5 단위)

    @Column(name = "parent_comment_id")
    private Long parentCommentId;     // 부모 댓글 ID (대댓글용)

    @Transient
    private List<ContentComment> replies = new ArrayList<>();  // 자식 댓글 (대댓글)

    private LocalDateTime createdDate;
}
```

**대댓글 구현 방식:**
- `parentCommentId`가 null이면 원댓글, 값이 있으면 대댓글
- Service 계층에서 원댓글 조회 후 대댓글 매핑
- `@Transient`: DB에 저장하지 않고 메모리상에서만 사용

---

### 18. 사용자 인증 시스템 (Spring Security)

#### CustomUserDetailsService.java

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다"));

        // 비활성화된 계정 체크
        if (!user.getIsActive()) {
            throw new RuntimeException("비활성화된 계정입니다");
        }

        // Spring Security User 객체 반환
        return new CustomUserDetails(user);
    }
}
```

#### SecurityConfig.java (보안 설정 - 최신 버전)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(new CustomLoginSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Spring Security 6.x 주요 변경사항:**
- `authorizeRequests()` → `authorizeHttpRequests()`
- `antMatchers()` → `requestMatchers()`
- 람다 DSL 스타일 권장
- `WebSecurityConfigurerAdapter` deprecated → `SecurityFilterChain` 사용

#### PasswordValidator.java (비밀번호 검증)

```java
@Component
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        return UPPERCASE.matcher(password).find() &&
               LOWERCASE.matcher(password).find() &&
               DIGIT.matcher(password).find() &&
               SPECIAL.matcher(password).find();
    }
}
```

**비밀번호 정책:**
- 최소 8자 이상
- 대문자 1개 이상
- 소문자 1개 이상
- 숫자 1개 이상
- 특수문자 1개 이상

---

### 19. Stock Board (주식 게시판)

#### StockBoard.java

```java
@Entity
@Table(name = "stock_board")
@Getter @Setter
public class StockBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ticker;            // 종목 티커 (005930, TSLA 등)

    @Column(nullable = false, length = 200)
    private String title;             // 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;           // 내용

    @Column(nullable = false, length = 50)
    private String writer;            // 작성자

    private LocalDateTime regDate;    // 등록일
    private LocalDateTime modDate;    // 수정일

    @Column(nullable = false)
    private int recommend = 0;        // 추천 수

    @Column(nullable = false)
    private int unrecommend = 0;      // 비추천 수

    @Column(nullable = false)
    private int view = 0;             // 조회수
}
```

**주요 기능:**
- 종목별 게시판 (ticker 필드로 구분)
- 추천/비추천 시스템
- 조회수 카운트
- 수정일 자동 업데이트 (`@PreUpdate`)

---

### 20. 한국투자증권 API 연동

#### KisApiTokenService.java (토큰 관리)

```java
@Service
@RequiredArgsConstructor
public class KisApiTokenService {

    private final OkHttpClient okHttpClient;

    @Value("${kis.api.appkey}")
    private String appKey;

    @Value("${kis.api.appsecret}")
    private String appSecret;

    private String accessToken;
    private LocalDateTime tokenExpiry;

    public String getAccessToken() {
        // 토큰이 만료되었거나 없으면 새로 발급
        if (accessToken == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() {
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("appkey", appKey)
                .add("appsecret", appSecret)
                .build();

        Request request = new Request.Builder()
                .url("https://openapi.koreainvestment.com:9443/oauth2/tokenP")
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                accessToken = json.get("access_token").getAsString();

                // 토큰 만료 시간 설정 (보통 24시간)
                tokenExpiry = LocalDateTime.now().plusHours(23);
            }
        } catch (IOException e) {
            throw new RuntimeException("토큰 발급 실패", e);
        }
    }
}
```

**토큰 관리 전략:**
- 토큰을 메모리에 캐싱 (accessToken 필드)
- 만료 시간 추적 (tokenExpiry)
- 요청 시마다 만료 체크 후 자동 갱신

---

## 새로운 데이터 흐름

### 4. OCR 포트폴리오 분석 흐름

```
[사용자] → [이미지 업로드]
    ↓
[portfolio-analyzer.html] → FormData 생성
    ↓
POST /api/portfolio/ocr/analyze
    ↓
[OcrService] extractPortfolioFromImage()
    ↓
┌─────────────────────────────────────┐
│ 1. BufferedImage 로드               │
│ 2. OpenCV 전처리 (증권사별)         │
│ 3. Tesseract OCR 실행               │
│ 4. 한국어 → 영어 티커 매핑          │
│ 5. 정규표현식 파싱 (증권사별)       │
└─────────────────────────────────────┘
    ↓
[응답] List<PortfolioStock>
    ↓
[GPTService] analyzePortfolioSimilarity()
    ↓
"당신의 포트폴리오는 워렌 버핏 스타일과 85% 유사합니다"
```

### 5. SEC 13F 수집 흐름

```
[관리자] → [수집 시작 버튼 클릭]
    ↓
POST /api/13f/start
    ↓
[SEC13FService] startAsyncCollection() - @Async
    ↓
┌────────────────────────────────────────────┐
│ for (투자자 : 투자자목록) {                 │
│   1. SEC EDGAR 웹사이트 접속              │
│   2. 13F XML 파일 다운로드                │
│   3. JAXB로 XML → EdgarSubmission 파싱    │
│   4. CUSIP → 티커 변환                    │
│   5. DB 저장 (Investor13FHolding)         │
│ }                                          │
└────────────────────────────────────────────┘
    ↓
[로그] "총 150개 종목 수집 완료"
```

### 6. 커뮤니티 댓글 작성 흐름

```
[사용자] → [댓글 작성 + 평점 선택]
    ↓
POST /content/comment
    ↓
[ContentPostController] writeComment()
    ↓
[@AuthenticationPrincipal] → CustomUserDetails (로그인 체크)
    ↓
[ContentCommentService] write()
    ↓
┌────────────────────────────────────┐
│ 1. 평점 유효성 검사 (필수)         │
│ 2. ContentComment 생성             │
│ 3. DB 저장                         │
│ 4. 평균 평점 재계산                │
└────────────────────────────────────┘
    ↓
[응답] "SUCCESS" or "NO_RATING"
    ↓
[화면] 댓글 목록 갱신
```

---

## 프로젝트 완료 후 추가된 주요 개선사항

### 1. 성능 최적화
- **캐싱**: Yahoo Finance API 응답을 StockQuoteCache, StockCandleCache에 캐싱
- **ThreadLocal**: OCR Tesseract 인스턴스를 스레드별로 재사용
- **비동기 처리**: SEC 13F 수집을 `@Async`로 백그라운드 처리
- **인덱스 최적화**: 자주 검색되는 컬럼에 DB 인덱스 추가

### 2. 사용자 경험 개선
- **대댓글 시스템**: 댓글에 답글 달기 기능
- **평점 시스템**: 0.5 단위의 별점 평가
- **소프트 삭제**: 삭제된 게시글 복구 가능
- **실시간 검색**: 자동완성으로 투자자 검색

### 3. 보안 강화
- **Spring Security 6**: 최신 버전으로 업그레이드
- **비밀번호 정책**: 복잡도 검증 (대소문자, 숫자, 특수문자)
- **CSRF 토큰**: POST 요청에 CSRF 토큰 검증
- **세션 관리**: 로그인 세션 만료 시간 설정

### 4. 관리자 기능
- **관리자 대시보드**: 뉴스, 콘텐츠, 13F 데이터 관리
- **통계 조회**: 회원 수, 게시글 수, 댓글 수 통계
- **콘텐츠 관리**: 부적절한 게시글/댓글 삭제
- **13F 수집 제어**: 수집 시작/중단/상태 확인

---

## 기술적 도전과 해결책

### 문제 1: OCR 정확도 낮음
**원인:** 증권사 앱 UI에 배경 그라데이션, 다양한 폰트 크기
**해결:** OpenCV 전처리 파이프라인 구축
- 그레이스케일 → 가우시안 블러 → 적응형 임계화 → 형태학 연산
- 증권사별 커스텀 전처리기 (TossPreprocessor, KiwoomPreprocessor)

### 문제 2: CUSIP → 티커 변환
**원인:** SEC 13F 파일에는 CUSIP만 있고 티커가 없음
**해결:** OpenFIGI API 활용
- CUSIP를 OpenFIGI에 전송하여 티커 조회
- 응답 캐싱으로 API 호출 최소화

### 문제 3: Tesseract thread-safety
**원인:** 멀티스레드 환경에서 동시 OCR 요청 시 충돌
**해결:** ThreadLocal 패턴
- 각 스레드마다 별도의 Tesseract 인스턴스 생성
- 스레드 풀 크기만큼만 인스턴스 존재

### 문제 4: 대댓글 N+1 문제
**원인:** 원댓글 조회 → 각 원댓글의 대댓글 조회 (N번)
**해결:** Batch Fetching
- 원댓글 전체 조회 → 대댓글 전체 조회 (2번)
- 메모리상에서 부모-자식 매핑

---

## 마무리 (추가)

이 문서는 프로젝트 초기부터 완료까지의 모든 주요 코드와 추가 기능을 상세히 설명합니다.

### 프로젝트에서 사용된 고급 기술
- **비동기 프로그래밍**: `@Async`, CompletableFuture
- **함수형 프로그래밍**: Stream API, Optional
- **멀티스레딩**: ThreadLocal, Thread-Safe 설계
- **이미지 처리**: OpenCV, Tesseract OCR
- **XML 파싱**: JAXB
- **정규표현식**: 복잡한 텍스트 파싱
- **캐싱 전략**: 인메모리 캐싱
- **RESTful API 설계**: HATEOAS 원칙
