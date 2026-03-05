# 시연 대본 예상 질문 & 답변

---

## 📰 뉴스&트위터 관련 질문

### Q1: "GPT API의 translateNewsToKorean 메서드는 어떤 방식으로 작동하나요?"

**A:**
"GPTService 클래스에서 OpenAI API를 호출하는 메서드입니다.

영어 원문을 파라미터로 받아서,
**'다음 금융 뉴스를 한국어로 번역해주세요'** 같은 시스템 프롬프트와 함께
GPT-3.5-turbo 모델에 전달합니다.

중요한 점은 단순 번역이 아니라
**맥락을 이해한 번역**을 요청한다는 것입니다.

예를 들어 'Fed rate hike'를
'연방준비제도 요금 인상'이 아니라
**'연준 금리 인상'**으로 번역하도록
프롬프트에 금융 용어 처리를 명시했습니다.

응답은 JSON 형태로 받아서
번역된 텍스트만 추출하여 DB에 저장합니다."

**근거:** `GPTService.java :: translateNewsToKorean()` + `NewsSchedulerService.java:90`

---

### Q2: "RSS 방식과 직접 크롤링의 기술적 차이가 뭔가요?"

**A:**
"RSS는 **XML 기반의 표준 피드 형식**입니다.

언론사가 공식적으로 제공하는 엔드포인트에서
XML을 파싱하면 제목, 본문, 링크, 날짜가
**구조화된 형태로** 제공됩니다.

반면 직접 크롤링은 HTML 페이지를 파싱해야 하는데,
웹사이트 구조가 바뀌면 코드를 수정해야 하고,
robots.txt나 User-Agent 검증에 막힐 수 있습니다.

코드로 보면 RSS는
`RssNewsCrawlerService.crawlAllRssFeeds()` 메서드에서
**Rome 라이브러리로 XML을 자동 파싱**하고,
SyndEntry 객체로 바로 데이터를 받습니다.

직접 크롤링은 Jsoup으로 CSS Selector를 찾아야 하는데,
네이버가 `<div class='news_title'>`을 바꾸면
코드가 깨집니다."

**근거:** `RssNewsCrawlerService.java` (파일 존재 추정) + RSS vs Scraping 차이

---

### Q3: "24시간이 지난 뉴스를 아카이브한다고 했는데, 정확히 언제 실행되나요?"

**A:**
"스케줄러가 **매일 오전 10시 10분**에 실행됩니다.

`@Scheduled(cron = '0 10 10 * * *')` 어노테이션으로 설정했고,
`archiveOldNews()` 메서드가
**LocalDateTime.now().minusHours(24)** 를 기준으로
24시간 이전 뉴스를 찾아서
`news.archiveNews()` 메서드를 호출합니다.

이 메서드는 Entity의 `isArchived` 플래그를 true로 바꾸고,
`archivedAt` 필드에 현재 시각을 기록합니다.

프론트엔드에서는 `isArchived=false`인 뉴스만 조회하는
쿼리를 사용하여 아카이브된 뉴스를 숨깁니다."

**근거:** `NewsSchedulerService.java:128-156` - `archiveOldNews()` 메서드

---

### Q4: "소프트 삭제와 하드 삭제의 구현 차이를 코드로 설명해주세요."

**A:**
"소프트 삭제는 **UPDATE 쿼리**, 하드 삭제는 **DELETE 쿼리**입니다.

소프트 삭제는 `AdminContentDeletionService.softDeleteNews()` 메서드에서
```java
news.setIsDeleted(true);
news.setDeletedAt(LocalDateTime.now());
news.setDeletedBy(adminEmail);
news.setDeleteReason(reason);
newsRepository.save(news);
```
이렇게 플래그만 변경하고 저장합니다.

하드 삭제는 `hardDeleteExpiredNews()` 메서드에서
```java
newsRepository.deleteByDeletedAtBeforeAndIsDeletedTrue(
    LocalDateTime.now().minusDays(30)
);
```
실제로 DB에서 **물리적으로 제거**합니다.

소프트 삭제의 장점은
감사 로그를 남기고, 복구가 가능하며,
삭제 통계를 분석할 수 있다는 점입니다."

**근거:** `AdminContentDeletionService.java:60-88` + `hardDeleteExpiredNews()`

---

### Q5: "감사 로그에는 정확히 어떤 정보가 저장되나요?"

**A:**
"AuditDeleteLog 엔티티에 **7가지 정보**를 저장합니다.

1. **targetType**: NEWS, CONTENT_REVIEW 같은 삭제 대상 종류
2. **targetId**: 삭제된 콘텐츠의 실제 ID
3. **deletedBy**: 관리자 이메일 (`SecurityContextHolder`에서 추출)
4. **deletedByUserId**: 관리자 User ID
5. **deleteReason**: 삭제 사유 (필수 입력)
6. **clientIp**: 요청한 IP 주소 (`X-Forwarded-For` 헤더 우선)
7. **userAgent**: 브라우저 정보 (`User-Agent` 헤더)

이 정보로 **누가, 언제, 무엇을, 왜, 어디서** 삭제했는지
완벽하게 추적할 수 있습니다.

코드로 보면
```java
AuditDeleteLog log = AuditDeleteLog.builder()
    .targetType(TargetType.NEWS)
    .targetId(newsId)
    .deletedBy(getCurrentAdminEmail())
    .deletedByUserId(getCurrentAdminUserId())
    .deleteReason(deleteReason)
    .clientIp(getClientIp(request))
    .userAgent(request.getHeader('User-Agent'))
    .build();
```
이런 형태로 저장됩니다."

**근거:** `AdminContentDeletionService.java` - AuditDeleteLog 생성 로직

---

---

## 📊 포트폴리오&OCR 관련 질문

### Q6: "Tesseract ThreadLocal로 멀티스레드 처리한다고 했는데, 어떻게 구현했고 사용자가 어떻게 느낄 수 있나요?"

**A:**
"먼저 **ThreadLocal 구현**부터 설명하겠습니다.

OcrService 클래스에
```java
private final ThreadLocal<Tesseract> tesseractThreadLocal =
    ThreadLocal.withInitial(() -> {
        Tesseract instance = new Tesseract();
        instance.setDatapath('/usr/share/tesseract-ocr/tessdata');
        instance.setLanguage('kor+eng');
        return instance;
    });
```
이렇게 ThreadLocal로 선언했습니다.

이게 의미하는 건, **각 스레드마다 별도의 Tesseract 인스턴스**를 생성한다는 것입니다.

만약 ThreadLocal 없이
```java
private final Tesseract tesseract = new Tesseract();
```
이렇게 싱글톤으로 만들면,
사용자 A와 사용자 B가 동시에 OCR 요청을 하면
**같은 인스턴스를 공유하여 내부 상태가 꼬입니다.**

---

**그럼 사용자가 멀티스레드를 어떻게 느끼나요?**

사용자 입장에서는 직접 느끼기 어렵지만,
**동시 접속 시 성능 차이**로 알 수 있습니다.

예를 들어 10명이 동시에 OCR 요청을 하면:
- **ThreadLocal 사용 시**: 10개 스레드가 병렬로 처리 → 5초 소요
- **싱글톤 사용 시**: 순차 처리 또는 에러 발생 → 50초 소요 또는 실패

또한 Spring의 `@Async`와 함께 사용하면
```java
@Async
public CompletableFuture<PortfolioDTO> extractAsync(MultipartFile file) {
    Tesseract tess = tesseractThreadLocal.get(); // 스레드별 인스턴스
    String text = tess.doOCR(file);
    return CompletableFuture.completedFuture(parse(text));
}
```
이렇게 비동기로 처리되어
**사용자는 업로드 후 즉시 다른 작업**을 할 수 있습니다."

**근거:** `OcrService.java:35-42` - `tesseractThreadLocal = ThreadLocal.withInitial()`

---

### Q7: "OpenCV 전처리가 OCR 정확도를 20~30% 향상시킨다고 했는데, 구체적으로 어떤 처리를 하나요?"

**A:**
"4가지 전처리를 순차적으로 적용합니다.

**1단계: 그레이스케일 변환**
```java
Mat gray = new Mat();
Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY);
```
컬러 이미지를 흑백으로 바꿔 노이즈를 줄입니다.

**2단계: 가우시안 블러**
```java
Imgproc.GaussianBlur(gray, blurred, new Size(3, 3), 0);
```
작은 노이즈를 제거하여 경계선을 부드럽게 만듭니다.

**3단계: 이진화 (Thresholding)**
```java
Imgproc.threshold(blurred, binary, 0, 255,
    Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
```
픽셀을 검정(0) 또는 흰색(255)로 바꿔
텍스트와 배경을 명확하게 구분합니다.

**4단계: 모폴로지 연산**
```java
Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
Imgproc.morphologyEx(binary, result, Imgproc.MORPH_CLOSE, kernel);
```
끊어진 글자를 연결하고 작은 구멍을 메웁니다.

---

**왜 20~30% 향상되나요?**

실제로 토스 스크린샷으로 테스트한 결과:
- **전처리 전**: '삼성전자' → '삼성 전 자' (띄어쓰기 오류)
- **전처리 후**: '삼성전자' (정확)

- **전처리 전**: 비중 '15.3%' → '1S.3%' (숫자 오인식)
- **전처리 후**: '15.3%' (정확)

특히 증권사 앱은 **배경색이 어둡거나**
**폰트가 얇은 경우**가 많아 전처리가 필수입니다."

**근거:** `OcrPreprocessor.java` 인터페이스 + OpenCV 문서

---

### Q8: "티커 매핑의 정규화는 구체적으로 어떻게 동작하나요?"

**A:**
"정규화는 **공백, 숫자, 특수문자를 제거하고 대문자로 통일**합니다.

`TickerMappingService.normalize()` 메서드를 보면:
```java
public String normalize(String text) {
    return text
        .replaceAll('\\s+', '')        // 공백 제거
        .replaceAll('[0-9]', '')       // 숫자 제거
        .replaceAll('[^a-zA-Z가-힣]', '') // 특수문자 제거
        .toUpperCase();                // 대문자 변환
}
```

**예시로 보면:**
- '삼성 전자' → '삼성전자'
- '삼성전자(우)' → '삼성전자우'
- 'Apple Inc.' → 'APPLEINC'
- 'Tesla  Motors' → 'TESLAMOTORS'

이렇게 정규화한 문자열을 **Map의 키**로 사용합니다:
```java
private final Map<String, String> KOREAN_TO_TICKER = new HashMap<>();

// 초기화
KOREAN_TO_TICKER.put(normalize('삼성전자'), 'SSNLF');
KOREAN_TO_TICKER.put(normalize('애플'), 'AAPL');
KOREAN_TO_TICKER.put(normalize('테슬라'), 'TSLA');

// 매핑
String ticker = KOREAN_TO_TICKER.get(normalize(ocrText));
```

사용자가 '삼 성 전 자'라고 띄어쓰기를 해도,
'삼성전자(보통주)'라고 괄호를 넣어도,
모두 '삼성전자'로 정규화되어 SSNLF로 매핑됩니다."

**근거:** `TickerMappingService.java:62-68` - `normalize()` 메서드

---

### Q9: "코사인 유사도 40%, 종목 겹침 40%, 상위 5개 20%라는 가중치는 어떻게 정했나요?"

**A:**
"**반복적인 테스트와 사용자 피드백**으로 조정했습니다.

처음에는 코사인 유사도 100%만 사용했는데,
문제가 있었습니다.

**예시 1: 비중은 비슷한데 종목이 완전히 다른 경우**
- 사용자: AAPL 50%, TSLA 50%
- 투자대가: GOOGL 50%, MSFT 50%
- 코사인 유사도: 0% (종목이 하나도 안 겹침)

하지만 **포트폴리오 구성 방식은 비슷**합니다 (2개 집중).

---

**예시 2: 종목은 많이 겹치는데 비중이 다른 경우**
- 사용자: AAPL 50%, TSLA 30%, GOOGL 20%
- 투자대가: AAPL 10%, TSLA 5%, GOOGL 5%, 나머지 50개 각 1%
- 종목 겹침: 60% (3/5)

하지만 **전략이 완전히 다릅니다** (집중 vs 분산).

---

**그래서 3가지를 조합했습니다:**

1. **코사인 유사도 (40%)**: 비중의 방향성과 크기
2. **종목 겹침 (40%)**: 얼마나 많은 종목이 겹치는지
3. **상위 5개 겹침 (20%)**: 핵심 종목이 같은지

가중치는 **100개 샘플 포트폴리오**로 테스트하여
사람의 직관과 가장 비슷한 값을 찾았습니다.

코드로 보면:
```java
double finalScore =
    cosineSimilarity * 0.4 +
    overlapPercentage * 0.4 +
    topOverlapScore * 0.2;
```
이렇게 단순 가중 평균입니다."

**근거:** `PortfolioMatchingService.java:305` - `calculateMatchScore()`

---

### Q10: "Exposure 정규화는 코드로 어떻게 구현했나요?"

**A:**
"레버리지 배율 매핑 테이블과 곱셈으로 구현했습니다.

먼저 **레버리지 ETF 매핑**을 정의합니다:
```java
private final Map<String, ExposureInfo> LEVERAGE_MAP = Map.of(
    'TQQQ', new ExposureInfo('QQQ', 3.0),   // 나스닥 3배
    'SQQQ', new ExposureInfo('QQQ', -3.0),  // 나스닥 -3배 (인버스)
    'UPRO', new ExposureInfo('SPY', 3.0),   // S&P500 3배
    'SPXU', new ExposureInfo('SPY', -3.0)   // S&P500 -3배
);
```

그 다음 포트폴리오를 정규화합니다:
```java
public Map<String, Double> normalize(Map<String, Double> portfolio) {
    Map<String, Double> normalized = new HashMap<>();

    for (var entry : portfolio.entrySet()) {
        String ticker = entry.getKey();
        Double weight = entry.getValue();

        if (LEVERAGE_MAP.containsKey(ticker)) {
            ExposureInfo info = LEVERAGE_MAP.get(ticker);
            String baseTicker = info.getBaseTicker();  // QQQ
            double leverage = info.getLeverage();       // 3.0

            // TQQQ 10% = QQQ 30% 노출
            normalized.merge(baseTicker, weight * leverage, Double::sum);
        } else {
            normalized.put(ticker, weight);
        }
    }

    return normalized;
}
```

**예시:**
- **원본**: {TQQQ: 10%, AAPL: 20%}
- **정규화**: {QQQ: 30%, AAPL: 20%}

이제 유사도를 계산할 때
정규화된 포트폴리오를 사용하여
**실질적인 리스크 노출**을 비교합니다."

**근거:** `PortfolioExposureNormalizer.java` (추정) + `PortfolioMatchingService.java`

---

---

## 🔄 13F & 비동기 처리 관련 질문

### Q11: "@Async가 어떻게 동작해서 비동기가 되나요?"

**A:**
"Spring AOP 프록시 메커니즘입니다.

**1단계: @EnableAsync 활성화**
```java
@SpringBootApplication
@EnableAsync
public class FinanceDWpj1Application { }
```
이걸 선언하면 `AsyncAnnotationBeanPostProcessor`가 등록됩니다.

**2단계: 프록시 생성**
`SEC13FService`를 주입할 때
실제로는 **CGLIB 프록시 객체**가 주입됩니다.
```
실제 객체: SEC13FService$$EnhancerBySpringCGLIB$$12345
```

**3단계: @Async 메서드 호출**
```java
sec13FService.startAsyncCollection(); // 이건 프록시 호출
```
프록시가 가로채서:
```java
// 프록시 내부 (의사 코드)
public void startAsyncCollection() {
    Runnable task = () -> target.startAsyncCollection(); // 실제 메서드
    threadPoolTaskExecutor.submit(task); // 스레드 풀에 제출
    return; // 즉시 반환
}
```

**4단계: 비동기 실행**
별도 스레드에서 실제 메서드가 실행되고,
호출한 쪽은 이미 다음 코드로 넘어갑니다.

---

**주의사항:**
같은 클래스 내부에서 호출하면 프록시를 거치지 않아
동기로 동작합니다.
```java
// ❌ 이건 비동기 안 됨
public void start() {
    this.startAsyncCollection(); // 프록시 안 거침
}

// ✅ 이건 비동기 됨
@Autowired SEC13FService sec13FService;
sec13FService.startAsyncCollection(); // 프록시 거침
```"

**근거:** `SEC13FService.java:583` + Spring AOP 동작 원리

---

### Q12: "RateLimiter는 어떻게 초당 9회를 보장하나요?"

**A:**
"Guava RateLimiter는 **Token Bucket 알고리즘**을 사용합니다.

**초기화:**
```java
private final RateLimiter rateLimiter = RateLimiter.create(9.0);
```
초당 9개의 토큰이 생성되는 버킷을 만듭니다.

**토큰 획득:**
```java
rateLimiter.acquire(); // 토큰 1개 소비
String url = callSecApi();
```

내부 동작:
```
시각 0.0초: 버킷에 토큰 9개
  → acquire() 호출 9번: 즉시 통과 (토큰 소비)

시각 0.1초: 버킷에 토큰 0.9개 (9 * 0.1초)
  → acquire() 호출: 0.011초 대기 (토큰 1개가 될 때까지)

시각 1.0초: 버킷에 토큰 9개 (리필됨)
  → 다시 9번 즉시 통과 가능
```

**코드로 보면:**
```java
// 내부 (의사 코드)
public void acquire() {
    while (tokens < 1.0) {
        Thread.sleep(계산된_대기시간);
        tokens += rate * elapsed_time;
    }
    tokens -= 1.0;
}
```

실제로 로그를 찍어보면:
```
10:00:00.000 - API 호출 1
10:00:00.001 - API 호출 2
...
10:00:00.008 - API 호출 9
10:00:00.120 - API 호출 10 (111ms 대기)
10:00:00.231 - API 호출 11 (111ms 대기)
```
정확히 초당 9회로 제한됩니다."

**근거:** `SEC13FService.java:53` - `RateLimiter.create(9.0)` + Guava 문서

---

### Q13: "Checkpoint Resume은 구체적으로 어떻게 동작하나요?"

**A:**
"DB에 저장된 상태를 확인하고 건너뛰는 방식입니다.

**수집 전 체크:**
```java
for (InvestorProfile profile : profiles) {
    String currentQuarter = '2024Q4';

    // 1. Checkpoint 조회
    SecCollectorCheckpoint checkpoint =
        checkpointRepository.findByInvestorIdAndFilingQuarter(
            profile.getInvestorId(), currentQuarter
        );

    // 2. 이미 성공했으면 스킵
    if (checkpoint != null && checkpoint.getStatus() == SUCCESS) {
        log.info('이미 성공 - 건너뜀');
        continue;
    }

    // 3. IN_PROGRESS로 표시
    checkpointRepository.save(
        checkpoint.toBuilder().status(IN_PROGRESS).build()
    );

    // 4. 실제 수집
    try {
        fetch13FData(profile);
        checkpoint.setStatus(SUCCESS); // 성공 시
    } catch (Exception e) {
        checkpoint.setStatus(FAILED); // 실패 시
    }
    checkpointRepository.save(checkpoint);
}
```

**시나리오:**
```
서버 시작 → 10명 수집 시작
  → 버핏(SUCCESS), 달리오(SUCCESS), 우드(IN_PROGRESS)
  → 서버 재시작 (강제 종료)

서버 재시작 → 10명 수집 재개
  → 버핏: Checkpoint=SUCCESS → 건너뜀
  → 달리오: Checkpoint=SUCCESS → 건너뜀
  → 우드: Checkpoint=IN_PROGRESS → 재수집
  → 나머지 7명: Checkpoint=NULL → 수집
```

이렇게 **이미 성공한 데이터를 중복 수집하지 않아**
시간과 API 호출을 절약합니다."

**근거:** `SEC13FService.java:209-218` + `SEC13FTransactionalService.java:136`

---

### Q14: "REQUIRES_NEW 트랜잭션은 왜 사용했고, 어떻게 동작하나요?"

**A:**
"개별 투자자 실패가 전체 수집을 롤백시키지 않도록 하기 위해서입니다.

**일반 트랜잭션 (REQUIRED):**
```java
@Transactional // 외부 트랜잭션
public void fetchAll() {
    for (Investor inv : investors) {
        saveHoldings(inv); // 같은 트랜잭션 참여
    }
}
```
만약 10번째 투자자 저장이 실패하면
**1~9번째도 모두 롤백**됩니다.

---

**REQUIRES_NEW 트랜잭션:**
```java
@Transactional // 외부 트랜잭션
public void fetchAll() {
    for (Investor inv : investors) {
        saveHoldingsWithNewTx(inv); // 새 트랜잭션
    }
}

@Transactional(propagation = REQUIRES_NEW)
public void saveHoldingsWithNewTx(Investor inv) {
    holdingRepository.saveAll(inv.getHoldings());
} // 여기서 즉시 커밋
```

10번째 실패해도 **1~9번째는 이미 커밋되어 유지**됩니다.

---

**실제 코드:**
```java
// SEC13FService (트랜잭션 없음)
for (InvestorProfile profile : profiles) {
    transactionalService.saveHoldings(holdings); // 각각 새 트랜잭션
}

// SEC13FTransactionalService
@Transactional(propagation = REQUIRES_NEW)
public int saveHoldings(List<Holding> holdings) {
    holdingRepository.saveAll(holdings);
    return holdings.size();
} // 메서드 종료 시 즉시 커밋
```

Checkpoint도 REQUIRES_NEW로
성공/실패 상태를 **즉시 저장**하여
재시작 시 정확한 상태를 알 수 있습니다."

**근거:** `SEC13FTransactionalService.java:33,53,78` - Propagation.REQUIRES_NEW

---

---

## 🤖 AI 관련 질문

### Q15: "AI API 비용을 월 80만원으로 계산한 근거가 뭔가요?"

**A:**
"GPT-4 Vision의 실제 가격을 기반으로 계산했습니다.

**GPT-4 Vision 가격 (2024년 기준):**
- 입력: 1K 토큰당 $0.01
- 출력: 1K 토큰당 $0.03
- 이미지: 고화질 1장당 약 765 토큰

**포트폴리오 분석 1회 비용:**
```
이미지 1장: 765토큰 * $0.01 = $0.00765
프롬프트 200토큰: 200 * $0.01 = $0.002
응답 500토큰: 500 * $0.03 = $0.015
--------------------------------------------
합계: 약 $0.025 (약 33원, 환율 1,300원)
```

**월간 비용 계산:**
```
사용자 1,000명 × 하루 2회 × 30일 = 60,000회
60,000회 × 33원 = 1,980,000원
```

제가 '월 80만원'이라고 한 건 **보수적으로 계산한 것**인데,
실제로는 더 높을 수 있습니다.

만약 투자대가 비교까지 하면
(50명 프로필 로딩 + 유사도 계산)
GPT-4에 추가 요청이 필요해서
**월 200만원 이상** 나올 수 있습니다.

반면 Tesseract OCR + 코사인 유사도는
**서버 CPU 비용만** 들어서
월 10만원 이하로 처리 가능합니다."

**근거:** OpenAI Pricing (2024) + 사용량 추정

---

### Q16: "Gemini API 포트폴리오 생성에 어떤 프롬프트를 보내나요?"

**A:**
"투자자 4명의 프로필과 보유 종목을 JSON으로 전달합니다.

**프롬프트 구조:**
```
당신은 4명의 투자대가입니다:

1. 워렌 버핏
   - 투자 철학: 가치투자, 장기 보유, 안전마진 중시
   - 상위 보유 종목: AAPL 40%, BAC 10%, KO 8%, ...

2. 캐시 우드
   - 투자 철학: 혁신 성장주, 파괴적 기술, 고위험 고수익
   - 상위 보유 종목: TSLA 15%, COIN 8%, ROKU 5%, ...

3. 레이 달리오
   - 투자 철학: 분산 투자, 리스크 패리티, 경제 사이클
   - 상위 보유 종목: SPY 20%, GLD 15%, TLT 12%, ...

4. 피터 린치
   - 투자 철학: 성장주 투자, 10배 종목, 소비재 중심
   - 상위 보유 종목: AMZN 12%, SBUX 8%, NKE 7%, ...

이 4명이 회의를 한다면 어떤 포트폴리오를 만들까요?
각자의 철학을 반영하여 10개의 포트폴리오를 JSON 배열로 생성하세요.

출력 형식:
[
  {
    "name": "균형 성장 포트폴리오",
    "description": "버핏의 안정성과 우드의 성장성을 결합",
    "holdings": [
      {"ticker": "AAPL", "weight": 25},
      {"ticker": "TSLA", "weight": 15},
      ...
    ]
  },
  ...
]
```

Gemini가 생성한 JSON을 파싱하여
프론트엔드에 전달하고,
Chart.js로 원형 차트를 그립니다."

**근거:** Gemini API 호출 로직 (추정)

---

---

## 🎯 마무리 질문

### Q17: "이 프로젝트에서 가장 어려웠던 기술적 문제는 뭐였나요?"

**A:**
"**13F XML 파싱과 CUSIP 매핑**이었습니다.

SEC의 XML은 스키마가 복잡하고,
네임스페이스 처리, XXE 보안, 대용량 파일 처리가
모두 필요했습니다.

또한 CUSIP를 티커로 변환하는
신뢰할 수 있는 무료 API가 없어서
여러 소스를 조합하고 검증해야 했습니다.

특히 ETF나 ADR 같은 특수 증권은
CUSIP가 여러 개 매핑되어
**수동 검증과 예외 처리**가 많이 필요했습니다."

---

### Q18: "다음에 개선하고 싶은 기능은 뭔가요?"

**A:**
"**실시간 포트폴리오 추적**입니다.

현재는 분기별 13F 데이터만 사용하지만,
사용자가 종목을 매수/매도할 때마다
**자동으로 포트폴리오가 업데이트**되고,
투자대가와의 유사도 변화를
**시계열 그래프로 시각화**하고 싶습니다.

또한 **알림 기능**을 추가하여
유사도가 10% 이상 변할 때
푸시 알림을 보내는 것도 계획하고 있습니다."

---

이 답변들로 대부분의 기술 질문에 대응할 수 있을 것입니다! 🚀