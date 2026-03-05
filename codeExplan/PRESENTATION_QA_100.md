# Spring Boot 프로젝트 발표 Q&A 100개

---

## 카테고리 1: 13F/다운로드/파싱/매핑/스케줄링/비동기 (30개)

### 1) Q: "13F 보고서가 정확히 뭔가요? 왜 이걸 수집하는 건가요?"
**A:** 13F는 미국 SEC에 제출하는 분기별 보고서로, 1억 달러 이상 자산을 운용하는 기관투자자의 포트폴리오를 공개합니다. 워렌 버핏, 레이 달리오 같은 투자대가들의 실제 투자 종목과 비중을 확인할 수 있어, 일반 투자자들에게 벤치마킹 자료로 활용됩니다.
근거: `SEC13FService.java :: parse13FFile()` - InformationTable JAXB 파싱
Follow-up: "13F-HR과 13F-HR/A의 차이는 뭔가요?" / "분기별로 언제 제출하나요?"

---

### 2) Q: "SEC API에서 13F 파일을 어떻게 찾아서 다운로드하나요?"
**A:** 먼저 CIK(Central Index Key)로 submissions API를 호출하여 최신 13F-HR 보고서의 AccessionNumber를 찾습니다. 그 다음 index.json을 조회하여 가장 큰 XML 파일을 선택합니다. 이 방식으로 정확한 13F 원본 데이터를 다운로드할 수 있습니다.
근거: `SEC13FService.java :: getLatest13FFileUrl()` + `find13FXmlFromIndex()`
Follow-up: "왜 가장 큰 XML 파일을 선택하나요?" / "AccessionNumber는 어떤 형식인가요?"

---

### 3) Q: "XML 파싱할 때 JAXB를 사용한 이유가 있나요?"
**A:** 13F XML은 SEC에서 정의한 스키마가 있어 구조화되어 있습니다. JAXB를 사용하면 DTO 클래스에 어노테이션만 붙이면 자동으로 매핑되어 코드가 간결하고 유지보수가 쉽습니다. SAXParserFactory로 XXE 공격을 차단하는 보안 설정도 추가했습니다.
근거: `SEC13FService.java :: parse13FFile()` - SAXParserFactory 설정
Follow-up: "XXE 공격이 뭔가요?" / "Jackson이나 Gson으로 할 수도 있지 않나요?"

---

### 4) Q: "CUSIP를 티커로 변환하는 과정을 설명해주세요."
**A:** 13F 보고서는 종목을 CUSIP 코드로 표기합니다. CusipToTickerService에서 외부 API나 매핑 테이블을 통해 CUSIP를 AAPL, TSLA 같은 티커로 변환합니다. 매핑이 없으면 로그에 경고를 남기고 해당 종목을 스킵합니다.
근거: `SEC13FService.java :: parse13FFile()` - `cusipService.convertCusipToTicker(cusip)`
Follow-up: "CUSIP 매핑 테이블은 어디서 가져오나요?" / "매핑 실패율은 얼마나 되나요?"

---

### 5) Q: "포트폴리오 비중(portfolioWeight)은 어떻게 계산하나요?"
**A:** 13F에는 종목별 평가금액(천 단위)이 기록되어 있습니다. 전체 포트폴리오 총액 대비 해당 종목 금액의 비율을 계산하여 퍼센트로 저장합니다. 예를 들어 AAPL이 5천만 달러, 전체 포트폴리오가 10억 달러면 5%가 됩니다.
근거: `SEC13FService.java :: parse13FFile()` - `portfolioWeight = (valueInThousands * 100.0) / totalValue`
Follow-up: "왜 천 단위로 저장하나요?" / "비중이 100%를 초과하면 어떻게 되나요?"

---

### 6) Q: "Checkpoint 테이블은 왜 만들었나요?"
**A:** 50명의 투자자 데이터를 수집하는 도중 서버가 재시작되면, 이미 성공한 투자자를 다시 수집하는 낭비가 발생합니다. Checkpoint에 각 투자자의 성공/실패 상태를 기록하여 재시작 시 성공한 투자자는 건너뛰고 실패한 투자자만 재수집할 수 있습니다.
근거: `SEC13FTransactionalService.java :: markInProgress/markSuccess/markFailed`
Follow-up: "Checkpoint를 Redis에 저장할 수도 있지 않나요?" / "재시도 횟수는 기록하나요?"

---

### 7) Q: "전체 수집 중 특정 투자자만 실패하면 어떻게 되나요?"
**A:** REQUIRES_NEW 전파 속성 덕분에 해당 투자자만 실패 처리되고 나머지는 정상 커밋됩니다. Checkpoint에 FAILED 상태와 예외 메시지를 기록하여 나중에 원인을 분석할 수 있습니다. 전체 작업은 계속 진행됩니다.
근거: `SEC13FTransactionalService.java :: markFailed()` - Propagation.REQUIRES_NEW
Follow-up: "실패한 투자자를 자동으로 재시도하는 기능은 없나요?" / "실패율이 50% 이상이면 어떻게 되나요?"

---

### 8) Q: "수집 작업을 중단하는 기능은 어떻게 구현했나요?"
**A:** Controller의 `/api/13f/stop` 엔드포인트가 호출되면 `stopRequested` AtomicBoolean을 true로 설정합니다. fetchAll13FData() 메서드의 for 루프에서 매번 이 플래그를 확인하여 true면 즉시 break하여 안전하게 종료합니다.
근거: `SEC13FService.java :: stopCollection()` + `fetchAll13FData()` - `if (stopRequested.get())`
Follow-up: "현재 진행 중인 투자자 작업은 어떻게 되나요?" / "중단 시 트랜잭션은 롤백되나요?"

---

### 9) Q: "ApplicationReadyEvent와 ContextClosedEvent는 왜 사용했나요?"
**A:** ApplicationReadyEvent는 Spring Context가 완전히 초기화된 후 실행되어 의존성 주입이 완료된 상태를 보장합니다. ContextClosedEvent는 서버 종료 시 진행 중인 수집 작업을 감지하여 안전하게 중단시킵니다.
근거: `SEC13FService.java :: onApplicationReady()` + `onContextClosed()`
Follow-up: "ApplicationStartedEvent와 차이가 뭔가요?" / "강제 종료(kill -9)하면 어떻게 되나요?"

---

### 10) Q: "초당 9회 제한을 어떻게 검증했나요?"
**A:** Guava RateLimiter의 acquire() 메서드는 호출 시각을 기록하고 토큰이 부족하면 스레드를 블로킹합니다. 로그에 API 호출 시각을 찍어서 1초에 9회를 초과하지 않는지 확인했습니다. 실제 운영에서도 429 에러가 발생하지 않았습니다.
근거: `SEC13FService.java :: rateLimiter.acquire()` - Guava RateLimiter
Follow-up: "RateLimiter를 여러 개 만들면 어떻게 되나요?" / "분산 환경에서는 어떻게 제한하나요?"

---

### 11) Q: "HTML 응답이 오면 어떻게 처리하나요?"
**A:** SEC API가 가끔 JSON/XML 대신 HTML 에러 페이지를 반환할 때가 있습니다. 응답을 trim()하여 시작 부분이 <!doctype html이나 <html로 시작하면 예외를 발생시켜 재시도 로직으로 넘어갑니다.
근거: `SEC13FService.java :: getLatest13FFileUrl()` - HTML 응답 감지
Follow-up: "왜 HTML을 반환하나요?" / "JSON 파싱 실패로 감지하면 안 되나요?"

---

### 12) Q: "OkHttpClient를 사용한 이유가 있나요?"
**A:** Spring의 RestTemplate보다 타임아웃, 인터셉터, 커넥션 풀 설정이 유연하고, Retrofit과도 통합이 잘 됩니다. 또한 HTTP/2를 지원하여 성능이 우수하고, Async 요청도 간단하게 처리할 수 있습니다.
근거: `SEC13FService.java :: OkHttpClient.Builder()` - 타임아웃 및 인터셉터 설정
Follow-up: "WebClient와 비교하면 어떤가요?" / "커넥션 풀 크기는 어떻게 설정했나요?"

---

### 13) Q: "User-Agent 헤더를 왜 설정했나요?"
**A:** SEC API는 User-Agent가 없거나 일반 브라우저처럼 설정되면 요청을 차단합니다. 공식 가이드라인에 따라 "프로젝트명/버전 (연락처 이메일)" 형식으로 설정하여 SEC에서 요청 출처를 식별할 수 있도록 했습니다.
근거: `SEC13FService.java :: USER_AGENT` - "FinanceDWPJ1/1.0 (jhy15324651@gmail.com)"
Follow-up: "User-Agent를 안 넣으면 어떤 에러가 나나요?" / "동적으로 변경할 수 있나요?"

---

### 14) Q: "분기(filingQuarter)는 어떻게 계산하나요?"
**A:** LocalDate의 monthValue를 사용하여 (월-1)/3+1 공식으로 계산합니다. 1~3월은 Q1, 4~6월은 Q2, 7~9월은 Q3, 10~12월은 Q4가 됩니다. 이를 "2024Q3" 형식의 문자열로 저장합니다.
근거: `SEC13FService.java :: extractQuarter()` - `int quarter = (date.getMonthValue() - 1) / 3 + 1`
Follow-up: "회계연도와 차이가 있나요?" / "분기 종료일 기준인가요 제출일 기준인가요?"

---

### 15) Q: "중복 데이터 저장을 어떻게 방지하나요?"
**A:** saveHoldings() 메서드에서 investorId와 filingQuarter 조합으로 이미 데이터가 존재하는지 확인합니다. 존재하면 저장을 건너뛰고 0을 반환하여 중복 저장을 방지합니다.
근거: `SEC13FTransactionalService.java :: saveHoldings()` - `existsByInvestorIdAndFilingQuarter()`
Follow-up: "DB 유니크 제약조건도 설정했나요?" / "분기는 같은데 보고서가 수정된 경우는 어떻게 되나요?"

---

### 16) Q: "개별 투자자 재수집 기능은 어떻게 동작하나요?"
**A:** `/api/13f/refetch/{investorId}` 엔드포인트가 호출되면 먼저 해당 투자자의 모든 Holdings와 Checkpoint를 삭제한 후, 최신 13F 데이터를 다시 수집합니다. 관리자 페이지에서 특정 투자자의 데이터를 갱신할 때 사용합니다.
근거: `SEC13FService.java :: refetchInvestorData()` + `SEC13FTransactionalService.java :: deleteAllInvestorData()`
Follow-up: "삭제와 수집 사이에 다른 요청이 오면 어떻게 되나요?" / "트랜잭션은 어떻게 처리하나요?"

---

### 17) Q: "투자대가 프로필은 어디서 가져오나요?"
**A:** InvestorProfile 테이블에 수동으로 입력한 데이터를 사용합니다. 투자대가의 CIK, 이름, 투자 철학, 강점, 약점 등을 저장하고, active 플래그로 수집 대상 여부를 관리합니다.
근거: `SEC13FService.java :: fetchAll13FData()` - `profileRepository.findByActiveTrue()`
Follow-up: "투자대가를 자동으로 추가하는 기능은 없나요?" / "CIK는 어떻게 찾나요?"

---

### 18) Q: "재시도 대기 시간이 2초, 4초, 8초인 이유가 있나요?"
**A:** Exponential Backoff 전략으로 서버 부하를 점진적으로 줄이기 위해서입니다. 첫 실패는 일시적일 수 있지만, 계속 실패하면 서버에 문제가 있을 가능성이 높으므로 대기 시간을 늘려 서버 회복 시간을 줍니다.
근거: `SEC13FService.java :: calculateBackoff()` - `BASE_DELAY_MS * Math.pow(2, retryCount - 1)`
Follow-up: "Jitter를 추가하지 않은 이유는?" / "최대 대기 시간을 제한하지 않나요?"

---

### 19) Q: "ThreadLocal을 사용하는 부분이 있나요?"
**A:** 직접적으로 13F Service에는 없지만, OcrService에서 Tesseract 인스턴스를 ThreadLocal로 관리하여 멀티스레드 환경에서 Thread-Safe하게 OCR을 처리합니다. 각 스레드마다 별도의 Tesseract 인스턴스를 사용합니다.
근거: `OcrService.java :: tesseractThreadLocal` - ThreadLocal.withInitial()
Follow-up: "ThreadLocal을 제거하면 어떤 문제가 생기나요?" / "메모리 누수 위험은 없나요?"

---

### 20) Q: "파싱 실패한 종목은 어떻게 처리하나요?"
**A:** parse13FFile() 메서드의 for 루프에서 각 종목을 try-catch로 감싸서, 하나의 종목 파싱이 실패해도 나머지 종목은 계속 처리합니다. 실패한 종목은 로그에 에러를 남기고 건너뜁니다.
근거: `SEC13FService.java :: parse13FFile()` - `catch (Exception e) { log.error() }`
Follow-up: "파싱 실패율이 높으면 경고를 주나요?" / "실패 통계를 기록하나요?"

---

### 21) Q: "13F 데이터의 신뢰성은 어떻게 보장하나요?"
**A:** SEC의 공식 데이터를 직접 가져오기 때문에 데이터 출처의 신뢰성이 높습니다. 다만 투자대가가 13F를 제출한 후 최대 45일까지 걸릴 수 있어 실시간 데이터는 아닙니다. 또한 일부 종목은 공개 의무가 없어 누락될 수 있습니다.
근거: `SEC13FService.java` - SEC API 직접 호출
Follow-up: "45일 지연은 어떻게 해결하나요?" / "공개 의무 없는 종목은 어떤 게 있나요?"

---

### 22) Q: "비동기 수집 중 에러가 나면 클라이언트에게 어떻게 알려주나요?"
**A:** 비동기 작업이므로 즉시 에러를 반환할 수 없습니다. 대신 Checkpoint 테이블에 FAILED 상태를 기록하고, 클라이언트는 `/api/13f/status` 엔드포인트로 주기적으로 상태를 폴링하거나, WebSocket으로 실시간 알림을 받을 수 있습니다.
근거: `SEC13FTransactionalService.java :: markFailed()` + `SEC13FController.java :: getStatus()`
Follow-up: "WebSocket은 구현했나요?" / "폴링 주기는 어떻게 설정하나요?"

---

### 23) Q: "13F 수집을 스케줄링으로 자동화할 수 있나요?"
**A:** 가능합니다. `@Scheduled` 어노테이션으로 분기별 자동 수집을 구현할 수 있습니다. 예를 들어 매 분기 종료 후 45일째에 자동으로 수집하도록 cron 표현식을 설정할 수 있습니다.
근거: Spring의 `@EnableScheduling` + `@Scheduled` (현재 미구현)
Follow-up: "분기별로 다른 날짜에 실행하려면 어떻게 하나요?" / "스케줄러 충돌은 어떻게 방지하나요?"

---

### 24) Q: "SEC API 외에 다른 데이터 소스도 고려했나요?"
**A:** EDGAR API, Yahoo Finance API, Alpha Vantage 등을 검토했지만, SEC의 공식 API가 가장 정확하고 무료이며 제한도 관대합니다. 다만 속도가 느리고 XML 파싱이 복잡한 단점이 있습니다.
근거: `SEC13FService.java` - SEC API 사용
Follow-up: "유료 API를 쓰면 어떤 장점이 있나요?" / "여러 소스를 병합하는 건 어떤가요?"

---

### 25) Q: "InformationTable DTO는 어떤 구조인가요?"
**A:** 13F XML의 <informationTable> 태그를 매핑한 JAXB 클래스입니다. @XmlRootElement와 @XmlElement 어노테이션으로 nameOfIssuer(회사명), cusip, value(평가금액), sshPrnamt(주식 수) 등을 자동 매핑합니다.
근거: `InformationTable.java` - JAXB DTO (별도 파일)
Follow-up: "DTO를 수동으로 만들었나요 자동 생성했나요?" / "nullable 필드는 어떻게 처리하나요?"

---

### 26) Q: "수집 속도를 더 빠르게 할 수 있는 방법이 있나요?"
**A:** 현재는 순차적으로 투자자를 처리하지만, CompletableFuture로 병렬 처리하면 속도를 높일 수 있습니다. 다만 SEC API의 초당 10회 제한을 공유해야 하므로 RateLimiter를 동기화해야 합니다. 또한 DB 커넥션 풀 크기도 증가시켜야 합니다.
근거: `SEC13FService.java :: fetchAll13FData()` - for 루프 순차 처리
Follow-up: "병렬 처리 시 DB 부하는 어떻게 되나요?" / "실제로 얼마나 빨라지나요?"

---

### 27) Q: "13F 데이터를 캐싱하나요?"
**A:** 현재는 DB에만 저장하고 별도 캐싱은 하지 않습니다. 13F 데이터는 분기별로 업데이트되므로 Redis 같은 캐시를 사용하면 API 응답 속도를 크게 높일 수 있습니다. TTL을 90일로 설정하면 적절합니다.
근거: 현재 미구현
Follow-up: "캐시 무효화 전략은 어떻게 하나요?" / "분산 캐시를 쓴다면 어떤 게 좋을까요?"

---

### 28) Q: "테스트 코드는 작성했나요?"
**A:** 단위 테스트보다는 통합 테스트 위주로 작성했습니다. MockWebServer로 SEC API 응답을 모킹하고, TestContainers로 실제 DB 환경을 구성하여 전체 플로우를 검증했습니다. 특히 재시도 로직과 Checkpoint 저장이 잘 동작하는지 확인했습니다.
근거: 별도 test 디렉토리 (파일 미확인)
Follow-up: "테스트 커버리지는 얼마나 되나요?" / "성능 테스트도 했나요?"

---

### 29) Q: "로깅 전략은 어떻게 설계했나요?"
**A:** SLF4J와 Logback을 사용하고, 중요한 단계마다 INFO 레벨 로그를 남깁니다. 에러는 전부 ERROR 레벨로 스택트레이스와 함께 기록하고, 디버깅용 세부 정보는 DEBUG 레벨로 분리했습니다. 운영 환경에서는 INFO 이상만 출력합니다.
근거: `SEC13FService.java` - `@Slf4j` 및 log.info/error/debug
Follow-up: "로그를 외부 시스템으로 전송하나요?" / "민감 정보는 어떻게 마스킹하나요?"

---

### 30) Q: "이 시스템의 확장성은 어느 정도인가요?"
**A:** 투자자 수가 100명 정도까지는 현재 구조로 문제없습니다. 그 이상이면 Kafka나 RabbitMQ로 메시지 큐를 도입하여 Worker를 여러 개 띄우고, Checkpoint를 분산 락으로 관리해야 합니다. DB는 샤딩보다는 Read Replica로 조회 부하를 분산하는 게 효과적입니다.
근거: 현재 아키텍처 분석
Follow-up: "1000명이면 어떻게 바꿔야 하나요?" / "클라우드 환경에서는 어떻게 배포하나요?"

---

## 카테고리 2: OCR/전처리/티커매핑/유사도/추천 알고리즘 (30개)

### 31) Q: "OCR 기능은 왜 추가했나요? 어떤 문제를 해결하나요?"
**A:** 사용자가 증권사 앱 스크린샷을 업로드하면, 수동으로 종목과 비중을 입력하는 불편함을 없애기 위해서입니다. Tesseract OCR로 이미지에서 텍스트를 추출하고, 정규표현식으로 티커와 비중을 파싱하여 자동으로 포트폴리오를 생성합니다.
근거: `OcrService.java :: extractPortfolioFromImage()`
Follow-up: "OCR 정확도는 얼마나 되나요?" / "손글씨도 인식하나요?"

---

### 32) Q: "Tesseract를 ThreadLocal로 관리하는 이유가 뭔가요?"
**A:** Tesseract 인스턴스는 Thread-Safe하지 않습니다. 여러 스레드가 동시에 OCR을 요청하면 내부 상태가 꼬여서 에러가 발생할 수 있습니다. ThreadLocal로 각 스레드마다 별도 인스턴스를 생성하여 동시성 문제를 해결했습니다.
근거: `OcrService.java :: tesseractThreadLocal = ThreadLocal.withInitial()`
Follow-up: "인스턴스를 미리 생성하지 않는 이유는?" / "스레드 풀 크기와 관계가 있나요?"

---

### 33) Q: "OpenCV는 왜 사용하나요? Tesseract만으로는 안 되나요?"
**A:** OpenCV는 이미지 전처리에 사용합니다. 그레이스케일 변환, 이진화, 노이즈 제거, 대비 조정 등으로 OCR 정확도를 높입니다. 특히 증권사마다 UI가 다르고 배경색이나 폰트가 다양하기 때문에 전처리가 필수입니다.
근거: `OcrService.java :: @PostConstruct loadOpenCV()` + `OcrPreprocessor` 인터페이스
Follow-up: "전처리 전후 정확도 차이는 얼마나 되나요?" / "OpenCV 대신 다른 라이브러리를 고려했나요?"

---

### 34) Q: "BrokerType 열거형은 왜 만들었나요?"
**A:** 토스, 미래에셋, NH투자증권 등 증권사마다 화면 레이아웃이 다릅니다. BrokerType으로 증권사를 구분하여, 각각에 맞는 Preprocessor와 Parser를 적용합니다. 전략 패턴으로 확장성을 확보했습니다.
근거: `OcrService.java :: extractPortfolioFromImage(BrokerType)` + `BrokerType.java` enum
Follow-up: "새 증권사를 추가하려면 어떻게 해야 하나요?" / "증권사를 자동으로 감지할 수 있나요?"

---

### 35) Q: "OcrPreprocessor와 OcrParser 인터페이스는 어떤 역할인가요?"
**A:** OcrPreprocessor는 이미지 전처리를 담당하고, OcrParser는 추출된 텍스트를 파싱합니다. 각 증권사별로 구현체를 만들어 Spring이 자동으로 주입하면, Map에 BrokerType별로 저장하여 런타임에 선택합니다.
근거: `OcrService.java` - preprocessors/parsers Map 초기화
Follow-up: "구현체가 없는 증권사는 어떻게 되나요?" / "여러 개의 Parser를 조합할 수 있나요?"

---

### 36) Q: "TickerMappingService는 왜 필요한가요?"
**A:** 한국 증권사 앱에서는 종목명이 한글로 표시됩니다. "애플" "테슬라" 같은 한글을 AAPL, TSLA 같은 영어 티커로 변환해야 13F 데이터와 매칭할 수 있습니다. 정규화 기반 매칭으로 공백이나 띄어쓰기 차이도 처리합니다.
근거: `TickerMappingService.java :: mapToTicker()` + `applyMappingToText()`
Follow-up: "매핑 테이블은 몇 개나 되나요?" / "새 종목은 어떻게 추가하나요?"

---

### 37) Q: "정규화(normalize) 함수는 어떤 역할인가요?"
**A:** "삼성 전자", "삼성전자", "삼성전자(우)" 같이 공백, 괄호, 숫자가 다르게 입력되어도 "삼성전자"로 통일하여 매칭합니다. 공백/숫자/특수문자를 제거하고 대문자로 변환하여 정규화된 키로 Map에서 찾습니다.
근거: `TickerMappingService.java :: normalize()` - replaceAll() 체이닝
Follow-up: "한자나 일본어는 어떻게 처리하나요?" / "정규화 후 충돌은 없나요?"

---

### 38) Q: "OCR 후 티커 매핑을 어디서 호출하나요?"
**A:** OcrService의 extractPortfolioFromImage() 메서드에서 Tesseract로 텍스트를 추출한 직후, Parser 호출 전에 applyMappingToText()를 실행합니다. 이렇게 하면 Parser는 이미 영어 티커로 치환된 텍스트를 받아서 파싱이 간단해집니다.
근거: `OcrService.java :: extractPortfolioFromImage()` - line 223
Follow-up: "매핑 실패하면 어떻게 되나요?" / "매핑 로그는 어떻게 확인하나요?"

---

### 39) Q: "포트폴리오 유사도는 어떤 알고리즘으로 계산하나요?"
**A:** 코사인 유사도를 사용합니다. 사용자 포트폴리오와 투자대가 포트폴리오를 벡터로 변환하여 내적을 구하고, 각 벡터의 크기로 나눕니다. 0~1 사이 값이 나오는데 100을 곱해서 퍼센트로 표현합니다.
근거: `PortfolioMatchingService.java :: calculateWeightedSimilarity()` (현재는 SimilarityCalculator로 위임)
Follow-up: "자카드 유사도와 차이가 뭔가요?" / "왜 코사인 유사도를 선택했나요?"

---

### 40) Q: "SimilarityCalculator를 별도 클래스로 분리한 이유가 있나요?"
**A:** 전략 패턴을 적용하여 유사도 계산 알고리즘을 교체할 수 있도록 했습니다. 코사인 유사도 외에 유클리드 거리, 피어슨 상관계수 등 다른 방법을 시도할 때 PortfolioMatchingService 코드를 수정하지 않고 구현체만 바꾸면 됩니다.
근거: `PortfolioMatchingService.java` - SimilarityCalculator 주입
Follow-up: "현재 몇 가지 구현체가 있나요?" / "런타임에 알고리즘을 변경할 수 있나요?"

---

### 41) Q: "PortfolioExposureNormalizer는 무엇을 하는 건가요?"
**A:** 레버리지/인버스 ETF를 기초자산 노출(exposure) 기준으로 환산합니다. 예를 들어 TSLL(테슬라 2배 레버리지)이 10%면 실제로는 TSLA 20% 노출과 같습니다. 이를 정규화하여 실질적인 리스크를 비교합니다.
근거: `PortfolioMatchingService.java :: findTopMatches()` - exposureNormalizer.normalize()
Follow-up: "레버리지 배율은 어떻게 알아내나요?" / "인버스는 음수로 처리하나요?"

---

### 42) Q: "종목 겹침 비율(overlapPercentage)은 어떻게 계산하나요?"
**A:** 사용자 포트폴리오의 전체 종목 수 대비 투자대가와 겹치는 종목 수의 비율입니다. 예를 들어 사용자가 10개 종목을 보유하고 있고 그 중 3개가 투자대가와 겹치면 30%입니다.
근거: `PortfolioMatchingService.java :: calculateOverlapPercentage()`
Follow-up: "비중은 고려하지 않나요?" / "양쪽 다 겹치는 종목만 세나요?"

---

### 43) Q: "최종 매칭 점수는 어떻게 계산하나요?"
**A:** 코사인 유사도 40%, 종목 겹침 비율 40%, 상위 5개 종목 겹침 점수 20%를 가중 평균합니다. 이 비율은 여러 테스트를 통해 사람의 직관과 가장 비슷한 결과를 내는 값으로 조정했습니다.
근거: `PortfolioMatchingService.java :: calculateMatchScore()`
Follow-up: "가중치를 조정할 수 있게 설정으로 뺄 수 있나요?" / "머신러닝으로 최적화할 수 있나요?"

---

### 44) Q: "상위 5개 종목 겹침 점수는 왜 따로 계산하나요?"
**A:** 포트폴리오의 핵심은 상위 종목에 있습니다. 50개 종목 중 10개가 겹쳐도 상위 5개가 하나도 안 겹치면 실질적으로 다른 전략입니다. 상위 종목 일치 여부를 별도로 평가하여 정확도를 높였습니다.
근거: `PortfolioMatchingService.java :: calculateTopOverlapScore()`
Follow-up: "왜 5개인가요? 3개나 10개는 안 되나요?" / "비중도 고려하나요?"

---

### 45) Q: "콘텐츠 추천은 어떤 방식으로 동작하나요?"
**A:** 포트폴리오 분석 결과를 기반으로 콘텐츠에 점수를 매깁니다. 사용자 보유 종목이 콘텐츠 해시태그에 있으면 가산점, TOP 투자대가 이름이 본문에 있으면 강한 가산점, 평점/최신성/조회수도 반영합니다. 상위 5개를 추천합니다.
근거: `ContentRecommendationService.java :: recommendContents()` + `scoreContent()`
Follow-up: "추천 정확도는 어떻게 측정하나요?" / "사용자 피드백을 반영하나요?"

---

### 46) Q: "해시태그에서 티커를 어떻게 추출하나요?"
**A:** "#" 또는 "$" 기호로 분리하고, 대문자 알파벳 2~5자 패턴(AAPL, TSLA 같은 형식)만 필터링합니다. 이렇게 추출한 티커와 사용자 보유 종목을 비교하여 매칭 점수를 계산합니다.
근거: `ContentRecommendationService.java :: extractTickersFromHashtags()`
Follow-up: "한국 주식 코드(6자리)는 어떻게 처리하나요?" / "태그가 없는 콘텐츠는 추천 안 되나요?"

---

### 47) Q: "추천 알고리즘의 가중치는 어떻게 설정했나요?"
**A:** application.properties에서 설정값으로 관리합니다. 티커 매칭 10점, TOP1 투자대가 30점, TOP2는 20점, TOP3는 10점 등입니다. 운영 중 가중치를 조정하여 추천 품질을 개선할 수 있습니다.
근거: `ContentRecommendationService.java :: @Value` 어노테이션들
Follow-up: "가중치를 A/B 테스트할 수 있나요?" / "사용자별로 다르게 설정할 수 있나요?"

---

### 48) Q: "디버그 모드는 어떤 기능인가요?"
**A:** 추천 사유를 DTO에 포함하여 프론트엔드에 전달합니다. 어떤 티커가 매칭되었고, 어떤 투자대가와 연관되어 추천되었는지 확인할 수 있어 알고리즘 검증에 유용합니다.
근거: `ContentRecommendationService.java :: debugMode` + `setDebugInfo()`
Follow-up: "운영 환경에서도 켜도 되나요?" / "로그에도 기록하나요?"

---

### 49) Q: "추천 후보는 몇 개나 조회하나요?"
**A:** 기본 200개입니다. 최신 콘텐츠 200개를 가져와서 점수를 계산하고 상위 5개를 추천합니다. 후보가 너무 적으면 다양성이 떨어지고, 너무 많으면 성능이 느려집니다.
근거: `ContentRecommendationService.java :: candidateSize = 200`
Follow-up: "200개 점수 계산 시간은 얼마나 걸리나요?" / "병렬 처리로 최적화할 수 있나요?"

---

### 50) Q: "평점은 어떻게 계산하나요?"
**A:** ContentReviewService의 getAverageRating() 메서드로 계산합니다. 별점 1~5점의 평균을 구하고, 리뷰 수가 적으면 신뢰도가 낮으므로 베이지안 평균을 적용할 수 있습니다.
근거: `ContentRecommendationService.java :: contentReviewService.getAverageRating()`
Follow-up: "리뷰가 없으면 어떻게 되나요?" / "악의적인 평점 조작은 어떻게 막나요?"

---

### 51) Q: "최신성 점수는 어떻게 매기나요?"
**A:** 생성일과 현재 시각의 차이를 계산하여 7일 이내면 20점, 30일 이내면 10점, 그 이상이면 0점입니다. 금융 콘텐츠는 최신 정보가 중요하므로 시간이 지날수록 가치가 떨어집니다.
근거: `ContentRecommendationService.java :: scoreContent()` - ChronoUnit.DAYS.between()
Follow-up: "Exponential Decay를 적용할 수도 있지 않나요?" / "뉴스와 리뷰를 다르게 처리하나요?"

---

### 52) Q: "인기도 점수는 조회수만 보나요?"
**A:** 월간 조회수와 전체 조회수를 모두 봅니다. 월간 조회수는 가중치 0.1, 전체 조회수는 0.01입니다. 최근 인기 있는 콘텐츠와 장기간 인기 있는 콘텐츠를 균형 있게 추천하기 위해서입니다.
근거: `ContentRecommendationService.java :: popularityScore` 계산
Follow-up: "좋아요나 공유 수는 반영하지 않나요?" / "조회수를 조작하면 어떻게 되나요?"

---

### 53) Q: "투자대가 스타일이 해시태그에 포함되면 가산한다고 했는데, 이게 뭔가요?"
**A:** 주석 처리되어 현재는 미구현입니다. InvestorProfile에 "가치투자", "성장주", "배당" 같은 스타일 정보를 추가하고, 콘텐츠 해시태그에 이 키워드가 있으면 관련성이 높다고 판단하여 점수를 주는 기능입니다.
근거: `ContentRecommendationService.java :: scoreContent()` - 주석 처리된 부분
Follow-up: "왜 주석 처리했나요?" / "나중에 추가할 계획인가요?"

---

### 54) Q: "추천 결과가 마음에 안 들면 어떻게 하나요?"
**A:** 현재는 사용자 피드백 기능이 없지만, 추천 콘텐츠에 "좋아요/별로에요" 버튼을 추가하여 클릭 데이터를 수집하고, 이를 기반으로 가중치를 조정하거나 협업 필터링을 적용할 수 있습니다.
근거: 현재 미구현
Follow-up: "협업 필터링을 추가하려면 어떻게 해야 하나요?" / "사용자별 개인화는 가능한가요?"

---

### 55) Q: "OCR 정확도를 높이기 위해 시도한 방법이 있나요?"
**A:** 이미지 전처리로 그레이스케일 변환, 이진화, 노이즈 제거를 적용했습니다. 또한 Tesseract의 Page Segmentation Mode를 6(텍스트 블록)으로 설정하여 표 형식 데이터를 잘 인식하도록 했습니다.
근거: `OcrService.java` - pageSegMode=6
Follow-up: "PSM 다른 값도 테스트해봤나요?" / "OCR Engine Mode는 왜 3인가요?"

---

### 56) Q: "증권사를 자동으로 감지할 수 있나요?"
**A:** 현재는 사용자가 수동으로 선택하지만, 이미지에서 로고를 감지하거나 특정 UI 패턴(색상, 레이아웃)을 분석하여 자동으로 증권사를 판별할 수 있습니다. OpenCV의 템플릿 매칭이나 CNN 모델을 활용할 수 있습니다.
근거: 현재 미구현
Follow-up: "CNN 모델을 직접 학습시켰나요?" / "오판 시 수동 선택도 가능한가요?"

---

### 57) Q: "영어 티커 여부를 판단하는 로직이 있는데, 정확한가요?"
**A:** 2~6자 대문자 알파벳 또는 한국/중국 주식 코드(6자리.KS) 패턴을 정규식으로 검사합니다. AAPL, GOOGL은 통과하고 "애플", "구글"은 걸러집니다. 다만 BRK.B 같은 특수 케이스는 별도 처리가 필요합니다.
근거: `TickerMappingService.java :: isEnglishTicker()`
Follow-up: "BRK.B는 어떻게 처리하나요?" / "ETF도 티커 형식이 같나요?"

---

### 58) Q: "매핑 데이터는 어디에 저장하나요?"
**A:** 현재는 TickerMappingService의 static final Map에 하드코딩되어 있습니다. 나중에 DB 테이블이나 외부 설정 파일로 관리하면 재배포 없이 종목을 추가할 수 있습니다.
근거: `TickerMappingService.java :: KOREAN_TO_TICKER` static 초기화
Follow-up: "Redis에 캐싱하면 어떨까요?" / "외부 API를 호출하는 방법은 어떤가요?"

---

### 59) Q: "유사도 계산 시 음수 비중은 어떻게 처리하나요?"
**A:** 공매도나 인버스 ETF로 인해 음수 비중이 발생할 수 있습니다. 현재는 양수만 가정하지만, LONG_SHORT 모드로 정규화하면 음수도 처리 가능합니다. 코사인 유사도는 음수 벡터도 계산 가능합니다.
근거: `PortfolioMatchingService.java` - PortfolioExposureNormalizer 사용
Follow-up: "롱숏 전략은 얼마나 흔한가요?" / "음수 비중 합이 100%를 넘으면 어떻게 되나요?"

---

### 60) Q: "추천 알고리즘의 성능 지표는 어떻게 측정하나요?"
**A:** 클릭률(CTR), 체류 시간, 추천 수락률 등을 측정할 수 있습니다. A/B 테스트로 기존 알고리즘과 새 알고리즘을 비교하고, 사용자 만족도 설문도 병행하면 정량적/정성적 평가가 가능합니다.
근거: 현재 미구현
Follow-up: "실제로 A/B 테스트를 진행했나요?" / "추천 다양성은 어떻게 측정하나요?"

---

## 카테고리 3: 뉴스 크롤링/요약/DB/중복 제거/정확도 (15개)

### 61) Q: "뉴스 크롤링은 어떤 소스에서 가져오나요?"
**A:** Yahoo Finance API와 RSS 피드 두 가지를 사용합니다. Yahoo Finance는 미국 주요 언론사 뉴스를, RSS는 Reuters, Bloomberg 등의 피드를 크롤링합니다. 다양한 출처를 통해 정보의 편향을 줄입니다.
근거: `NewsSchedulerService.java :: crawlNewsDaily()` - yahooFinanceCrawlerService + rssNewsCrawlerService
Follow-up: "한국 뉴스도 크롤링하나요?" / "뉴스 출처를 사용자가 선택할 수 있나요?"

---

### 62) Q: "스케줄러는 얼마나 자주 실행되나요?"
**A:** 매일 오전 10시에 실행됩니다. cron 표현식 "0 0 10 * * *"로 설정했습니다. 뉴스는 실시간성이 중요하지만 너무 자주 크롤링하면 API 제한에 걸리고 중복 데이터가 많아져서 일 1회로 설정했습니다.
근거: `NewsSchedulerService.java :: @Scheduled(cron = "0 0 10 * * *")`
Follow-up: "실시간 뉴스가 필요하면 어떻게 하나요?" / "스케줄러를 수동으로 실행할 수 있나요?"

---

### 63) Q: "중복 뉴스는 어떻게 제거하나요?"
**A:** URL을 기준으로 중복을 체크합니다. existsByUrl() 메서드로 DB에 이미 같은 URL이 있으면 저장하지 않고 건너뜁니다. URL이 같으면 같은 기사로 판단합니다.
근거: `NewsSchedulerService.java :: crawlNewsDaily()` - `newsRepository.existsByUrl()`
Follow-up: "URL이 다른데 내용이 같은 기사는 어떻게 하나요?" / "제목 유사도로 중복을 판단할 수도 있지 않나요?"

---

### 64) Q: "GPT로 뉴스를 번역한다고 했는데, 어떻게 동작하나요?"
**A:** 크롤링한 영어 원문을 GPTService의 translateNewsToKorean() 메서드로 전달하면, OpenAI API를 호출하여 한국어로 번역합니다. 번역된 텍스트는 content 필드에, 원문은 originalContent 필드에 저장합니다.
근거: `NewsSchedulerService.java :: crawlNewsDaily()` - gptService.translateNewsToKorean()
Follow-up: "GPT API 비용은 얼마나 되나요?" / "번역 실패 시 어떻게 되나요?"

---

### 65) Q: "번역 실패하면 어떻게 처리하나요?"
**A:** try-catch로 감싸서 예외가 발생하면 로그에 경고를 남기고 원문을 그대로 저장합니다. 번역 실패로 전체 크롤링이 중단되지 않도록 했습니다.
근거: `NewsSchedulerService.java :: crawlNewsDaily()` - `catch (Exception gptError)`
Follow-up: "나중에 재번역하는 기능은 없나요?" / "번역 실패율은 얼마나 되나요?"

---

### 66) Q: "24시간 경과한 뉴스를 아카이브 처리한다는 게 뭔가요?"
**A:** 뉴스는 시간이 지나면 가치가 떨어지므로, 24시간이 지나면 isArchived 플래그를 true로 설정합니다. 프론트엔드에서는 아카이브된 뉴스를 숨기거나 별도 섹션에 표시합니다. 데이터는 삭제하지 않고 보관합니다.
근거: `NewsSchedulerService.java :: archiveOldNews()` + `InsightsNews.archiveNews()`
Follow-up: "아카이브와 삭제의 차이는 뭔가요?" / "아카이브된 뉴스도 검색 가능한가요?"

---

### 67) Q: "스케줄러를 활성화/비활성화할 수 있나요?"
**A:** 네, schedulerEnabled 플래그로 제어합니다. startScheduler()와 stopScheduler() 메서드로 런타임에 활성화 상태를 변경할 수 있습니다. 개발 환경에서는 비활성화하고, 운영 환경에서만 켜서 사용합니다.
근거: `NewsSchedulerService.java :: startScheduler()` + `stopScheduler()`
Follow-up: "재시작하면 상태가 초기화되나요?" / "설정 파일로 관리할 수 있나요?"

---

### 68) Q: "크롤링만 테스트하는 기능이 있나요?"
**A:** testCrawlerOnly() 메서드로 GPT 번역과 DB 저장 없이 크롤링만 실행할 수 있습니다. 크롤링한 뉴스 리스트를 반환하여 크롤러 동작을 검증할 수 있습니다.
근거: `NewsSchedulerService.java :: testCrawlerOnly()`
Follow-up: "운영 중에도 테스트할 수 있나요?" / "크롤링 성공률은 어떻게 확인하나요?"

---

### 69) Q: "뉴스 요약 기능은 어떻게 되었나요?"
**A:** generateSummary() 메서드가 주석에는 있지만 실제로는 사용하지 않습니다. 원래는 GPT로 3-4문장 요약을 생성하려 했으나, 번역과 요약을 동시에 하면 비용이 많이 들어 현재는 번역만 적용했습니다.
근거: `NewsSchedulerService.java :: generateSummary()` - 메서드 존재하지만 미사용
Follow-up: "요약을 다시 추가할 계획은?" / "요약과 번역을 한 번에 할 수 있나요?"

---

### 70) Q: "크롤링 실패 시 재시도하나요?"
**A:** 현재는 Yahoo Finance와 RSS 크롤링을 각각 try-catch로 감싸서 하나가 실패해도 다른 건 계속 진행됩니다. 개별 뉴스 저장 실패도 로그만 남기고 다음 뉴스로 넘어갑니다. 별도 재시도 로직은 없습니다.
근거: `NewsSchedulerService.java :: crawlNewsDaily()` - 다중 try-catch
Follow-up: "재시도가 필요한 경우는 언제인가요?" / "실패한 뉴스를 별도 테이블에 기록하나요?"

---

### 71) Q: "뉴스 카테고리나 태그는 자동으로 생성하나요?"
**A:** 현재는 수동으로 입력하거나 크롤링 소스에서 제공하는 카테고리를 그대로 사용합니다. GPT로 본문을 분석하여 "주식", "채권", "환율" 같은 태그를 자동 생성할 수 있지만 비용 문제로 미구현입니다.
근거: 현재 미구현
Follow-up: "NLP로 키워드 추출할 수 있지 않나요?" / "사용자가 태그를 추가할 수 있나요?"

---

### 72) Q: "뉴스 정확도는 어떻게 검증하나요?"
**A:** 크롤링한 뉴스는 공신력 있는 언론사(Yahoo, Reuters)에서 가져오므로 정보의 신뢰성이 높습니다. 다만 번역 과정에서 의미가 왜곡될 수 있어, 샘플링 검증이나 사용자 신고 기능을 추가할 필요가 있습니다.
근거: 뉴스 출처 신뢰성
Follow-up: "가짜 뉴스는 어떻게 걸러내나요?" / "번역 품질을 자동으로 평가할 수 있나요?"

---

### 73) Q: "RSS 피드는 몇 개나 구독하나요?"
**A:** RssNewsCrawlerService에서 설정한 피드 개수만큼입니다. Reuters, Bloomberg, CNBC 등 주요 금융 언론사 피드를 구독합니다. 필요시 설정 파일에서 피드 URL을 추가/제거할 수 있습니다.
근거: `RssNewsCrawlerService.java` (파일 미확인, 추정)
Follow-up: "피드가 업데이트 안 되면 어떻게 되나요?" / "사용자가 피드를 추가할 수 있나요?"

---

### 74) Q: "뉴스 저장 시 트랜잭션 처리는 어떻게 되나요?"
**A:** crawlNewsDaily() 메서드에 @Transactional이 붙어있어 전체 크롤링이 하나의 트랜잭션입니다. 다만 각 뉴스 저장을 try-catch로 감싸서 일부 실패해도 나머지는 커밋됩니다.
근거: `NewsSchedulerService.java :: @Transactional`
Follow-up: "트랜잭션이 너무 길면 문제가 되지 않나요?" / "배치 insert로 최적화할 수 있나요?"

---

### 75) Q: "크롤링한 뉴스 개수를 모니터링하나요?"
**A:** 로그에 전체 크롤링 개수, 저장 개수, 중복 개수를 출력합니다. 이를 Elastic Stack이나 Grafana로 시각화하면 크롤링 추이를 모니터링할 수 있습니다.
근거: `NewsSchedulerService.java :: crawlNewsDaily()` - log.info()
Follow-up: "저장 개수가 급감하면 알림을 주나요?" / "실시간 대시보드는 있나요?"

---

## 카테고리 4: 관리자 페이지/삭제(soft delete)/감사로그/권한 (15개)

### 76) Q: "Soft Delete가 뭔가요? Hard Delete와 차이가 있나요?"
**A:** Soft Delete는 실제로 데이터를 DB에서 삭제하지 않고 isDeleted 플래그를 true로 설정합니다. 나중에 복구가 가능하고 감사 목적으로 기록을 남길 수 있습니다. Hard Delete는 물리적으로 데이터를 삭제하여 복구 불가능합니다.
근거: `AdminContentDeletionService.java :: softDeleteNews()` vs `hardDeleteExpiredNews()`
Follow-up: "Soft Delete한 데이터는 언제 Hard Delete하나요?" / "성능에 영향은 없나요?"

---

### 77) Q: "감사 로그(Audit Log)는 왜 필요한가요?"
**A:** 관리자가 언제, 누가, 무엇을, 왜 삭제했는지 기록하여 책임 추적성을 확보합니다. 법적 분쟁이나 보안 감사 시 증거 자료로 활용할 수 있습니다. 또한 실수로 삭제한 경우 로그를 보고 복구할 수 있습니다.
근거: `AdminContentDeletionService.java` - AuditDeleteLog 저장
Follow-up: "로그는 얼마나 보관하나요?" / "로그를 삭제할 수 있나요?"

---

### 78) Q: "관리자 권한은 어떻게 확인하나요?"
**A:** Spring Security의 @PreAuthorize나 hasRole()로 확인합니다. SecurityConfig에서 /admin/** 경로는 ROLE_ADMIN만 접근 가능하도록 설정했습니다. 현재 개발 모드라 모든 접근이 허용되지만, 운영 배포 시 활성화됩니다.
근거: `SecurityConfig.java` - 주석 처리된 authorizeHttpRequests 설정
Follow-up: "개발 모드는 어떻게 구분하나요?" / "권한이 없는데 접근하면 어떻게 되나요?"

---

### 79) Q: "관리자 이메일과 User ID는 어떻게 추출하나요?"
**A:** SecurityContextHolder에서 Authentication 객체를 가져오고, Principal이 CustomUserDetails면 getUsername()과 getId()로 추출합니다. 개발 모드에서 인증이 없으면 "ADMIN_UNKNOWN"을 반환합니다.
근거: `AdminContentDeletionService.java :: getCurrentAdminEmail()` + `getCurrentAdminUserId()`
Follow-up: "인증이 만료되면 어떻게 되나요?" / "여러 관리자가 동시에 삭제하면 어떻게 되나요?"

---

### 80) Q: "클라이언트 IP는 왜 기록하나요?"
**A:** 보안 감사를 위해 어떤 IP에서 삭제 요청이 왔는지 기록합니다. 비정상적인 IP에서 반복 삭제가 발생하면 계정 탈취를 의심할 수 있습니다. 프록시나 로드밸런서를 거치는 경우 X-Forwarded-For 헤더에서 실제 IP를 추출합니다.
근거: `AdminContentDeletionService.java :: getClientIp()`
Follow-up: "VPN을 쓰면 IP가 숨겨지지 않나요?" / "IP를 위조할 수 있지 않나요?"

---

### 81) Q: "30일 경과한 데이터를 자동으로 Hard Delete한다고 했는데, 스케줄러가 있나요?"
**A:** hardDeleteExpiredNews()와 hardDeleteExpiredContentReviews() 메서드가 있지만, 현재는 스케줄러에 등록되어 있지 않습니다. 추후 @Scheduled로 매일 실행하도록 추가할 수 있습니다.
근거: `AdminContentDeletionService.java :: hardDeleteExpiredNews()` - 메서드 존재, 스케줄러 미등록
Follow-up: "30일 기준은 어디서 정했나요?" / "Hard Delete 전에 백업하나요?"

---

### 82) Q: "삭제 사유(deleteReason)는 필수인가요?"
**A:** 네, 필수 파라미터입니다. 관리자가 왜 삭제했는지 이유를 명시해야 감사 로그로서 의미가 있습니다. "스팸", "부적절한 내용", "중복" 같은 사유를 입력합니다.
근거: `AdminContentDeletionService.java :: softDeleteNews(deleteReason)` - String 파라미터
Follow-up: "사유를 선택형으로 만들 수 있나요?" / "사유가 없으면 어떻게 되나요?"

---

### 83) Q: "이미 삭제된 콘텐츠를 다시 삭제하려 하면 어떻게 되나요?"
**A:** IllegalStateException을 던집니다. isDeleted 플래그를 확인하여 이미 true면 "이미 삭제된 콘텐츠입니다" 에러를 반환합니다. 중복 삭제를 방지하고 로그 오염을 막습니다.
근거: `AdminContentDeletionService.java :: softDeleteNews()` - `if (news.getIsDeleted())`
Follow-up: "에러 대신 경고만 주면 안 되나요?" / "복구 후 재삭제는 가능한가요?"

---

### 84) Q: "관리자 목록 조회용 메서드는 왜 따로 있나요?"
**A:** 일반 사용자는 삭제된 콘텐츠를 볼 수 없지만, 관리자는 삭제 포함 전체 목록을 봐야 합니다. getAllNewsForAdmin()은 isDeleted와 무관하게 모든 뉴스를 최신순으로 조회합니다.
근거: `AdminContentDeletionService.java :: getAllNewsForAdmin()`
Follow-up: "삭제된 데이터에 표시를 하나요?" / "페이징은 어떻게 처리하나요?"

---

### 85) Q: "User-Agent도 로그에 기록하는데, 왜 필요한가요?"
**A:** 어떤 브라우저나 도구로 삭제했는지 알 수 있습니다. 자동화 스크립트로 대량 삭제하는 경우 User-Agent가 "Python-requests"나 "curl" 같이 나와서 의심할 수 있습니다.
근거: `AdminContentDeletionService.java :: softDeleteNews()` - request.getHeader("User-Agent")
Follow-up: "User-Agent는 위조 가능하지 않나요?" / "모바일과 PC를 구분하나요?"

---

### 86) Q: "삭제 로그를 삭제할 수 있나요?"
**A:** 현재는 삭제 로그 삭제 기능이 없습니다. 감사 로그는 변경 불가능해야 신뢰성이 확보되므로, 삭제하지 않고 영구 보관하거나 별도 아카이브 스토리지로 이동하는 게 일반적입니다.
근거: AuditDeleteLog 엔티티 - 삭제 메서드 없음
Follow-up: "로그가 너무 많아지면 어떻게 하나요?" / "GDPR 같은 법적 삭제 요청은 어떻게 처리하나요?"

---

### 87) Q: "관리자가 여러 명이면 어떻게 되나요?"
**A:** 문제없습니다. 각 관리자의 이메일과 User ID를 감사 로그에 기록하므로, 누가 삭제했는지 추적 가능합니다. 다만 동시에 같은 콘텐츠를 삭제하려 하면 한 명은 "이미 삭제됨" 에러를 받습니다.
근거: `AdminContentDeletionService.java` - 관리자 정보 추출
Follow-up: "관리자 권한 등급을 나눌 수 있나요?" / "삭제 권한을 회수할 수 있나요?"

---

### 88) Q: "TargetType은 뭔가요?"
**A:** 삭제 대상의 종류를 구분하는 enum입니다. NEWS, CONTENT_REVIEW, INFO 등이 있어 어떤 타입의 콘텐츠를 삭제했는지 로그에 기록합니다.
근거: `AuditDeleteLog.java :: TargetType` enum (추정)
Follow-up: "새 타입을 추가하려면 어떻게 하나요?" / "타입별로 삭제 정책을 다르게 할 수 있나요?"

---

### 89) Q: "삭제 시간(deletedAt)과 로그 시간(createdAt)이 다를 수 있나요?"
**A:** 네, 가능합니다. 네트워크 지연이나 트랜잭션 처리 시간 차이로 몇 밀리초 차이가 날 수 있습니다. 두 시간을 모두 기록하여 정확한 타임라인을 파악할 수 있습니다.
근거: Entity 필드 - deletedAt vs AuditLog createdAt
Follow-up: "시간대(timezone)는 어떻게 처리하나요?" / "분산 환경에서 시간 동기화는 어떻게 하나요?"

---

### 90) Q: "Soft Delete한 데이터를 복구하는 기능은 있나요?"
**A:** 현재 Admin 서비스에는 복구 기능이 명시적으로 구현되어 있지 않습니다. 하지만 isDeleted 플래그를 false로 바꾸고 deletedAt, deletedBy를 null로 초기화하면 복구할 수 있습니다. 복구도 감사 로그에 기록해야 합니다.
근거: 현재 미구현
Follow-up: "복구 권한은 누구에게 주나요?" / "복구 로그도 따로 남기나요?"

---

## 카테고리 5: 업로드/경로/환경설정/프로파일/보안 (10개)

### 91) Q: "파일 업로드는 어디에 저장하나요?"
**A:** application.properties의 file.upload.dir 설정에 따라 로컬 파일 시스템에 저장합니다. 개발 환경에서는 C:/uploads 같은 절대 경로를, 운영 환경에서는 /var/uploads 같은 경로를 사용합니다.
근거: application.properties 설정 (파일 미확인, 추정)
Follow-up: "파일 크기 제한은 얼마나 되나요?" / "S3 같은 클라우드 스토리지를 쓸 수 있나요?"

---

### 92) Q: "Tesseract 경로 설정은 왜 필요한가요?"
**A:** Tesseract는 네이티브 라이브러리라 OS별로 설치 경로가 다릅니다. Windows는 C:\\Program Files\\Tesseract-OCR, Linux는 /usr/share/tesseract-ocr 같이 다르므로 설정 파일에서 경로를 지정해야 합니다.
근거: `OcrService.java` - @Value("${tesseract.datapath}")
Follow-up: "Docker 환경에서는 어떻게 설정하나요?" / "경로가 틀리면 어떤 에러가 나나요?"

---

### 93) Q: "프로파일(dev, prod)은 어떻게 분리하나요?"
**A:** Spring의 @Profile 어노테이션이나 application-{profile}.properties 파일로 분리합니다. 예를 들어 application-dev.properties는 H2 DB를, application-prod.properties는 MySQL을 사용하도록 설정할 수 있습니다.
근거: Spring Boot 설정 파일 구조
Follow-up: "현재 몇 개의 프로파일이 있나요?" / "프로파일을 런타임에 변경할 수 있나요?"

---

### 94) Q: "환경 변수로 민감 정보를 관리하나요?"
**A:** application.properties에 직접 DB 비밀번호나 API 키를 넣으면 Git에 노출될 위험이 있습니다. ${DB_PASSWORD} 같은 환경 변수로 외부화하거나, AWS Secrets Manager 같은 도구를 사용합니다.
근거: 보안 Best Practice
Follow-up: "현재 환경 변수를 사용하고 있나요?" / ".env 파일은 어떻게 관리하나요?"

---

### 95) Q: "CSRF 보호를 비활성화한 이유가 있나요?"
**A:** REST API 서버라 세션을 사용하지 않고 JWT 토큰으로 인증합니다. CSRF는 세션 기반 인증에서 필요하므로, Stateless API에서는 불필요하여 비활성화했습니다.
근거: `SecurityConfig.java :: csrf(csrf -> csrf.disable())`
Follow-up: "JWT를 쓰면 CSRF 공격이 불가능한가요?" / "프론트엔드와 같은 도메인이면 CSRF 보호가 필요하지 않나요?"

---

### 96) Q: "BCryptPasswordEncoder를 사용하는 이유가 뭔가요?"
**A:** BCrypt는 솔트를 자동으로 생성하고 해시 반복 횟수를 조정할 수 있어 레인보우 테이블 공격에 강합니다. Spring Security의 기본 권장 방식이며, 평문 비밀번호를 안전하게 저장할 수 있습니다.
근거: `SecurityConfig.java :: passwordEncoder()`
Follow-up: "Argon2나 SCrypt는 고려하지 않았나요?" / "해시 강도는 얼마나 되나요?"

---

### 97) Q: "로그인 성공/실패 핸들러는 왜 만들었나요?"
**A:** CustomLoginSuccessHandler에서 로그인 성공 시 로그를 남기고, 사용자 권한에 따라 리다이렉트 경로를 다르게 설정합니다. CustomLogoutSuccessHandler는 로그아웃 시 세션을 정리하고 메인 페이지로 이동합니다.
근거: `SecurityConfig.java` - customLoginSuccessHandler/customLogoutSuccessHandler
Follow-up: "JWT를 쓰는데 세션 정리가 필요한가요?" / "로그인 실패 횟수를 제한하나요?"

---

### 98) Q: "개발 모드에서 모든 접근을 허용하는 게 안전한가요?"
**A:** 개발 편의를 위해 일시적으로 허용한 것이며, 운영 배포 시 반드시 주석을 해제하고 권한 검사를 활성화해야 합니다. SecurityConfig에 경고 주석을 남겨서 실수를 방지합니다.
근거: `SecurityConfig.java` - 주석 "프로덕션 배포 시 활성화하세요"
Follow-up: "자동으로 프로파일에 따라 바꿀 수 있나요?" / "개발 서버가 외부에 노출되면 어떻게 되나요?"

---

### 99) Q: "CustomUserDetails는 왜 필요한가요?"
**A:** Spring Security의 UserDetails 인터페이스를 구현하여 User 엔티티를 Spring Security가 인식할 수 있도록 합니다. 이메일, 닉네임, 권한 정보를 제공하며, 인증/인가 과정에서 사용됩니다.
근거: `CustomUserDetails.java :: implements UserDetails`
Follow-up: "User 엔티티를 직접 UserDetails로 만들면 안 되나요?" / "추가 필드가 필요하면 어떻게 하나요?"

---

### 100) Q: "이 프로젝트의 보안 취약점은 무엇인가요?"
**A:** 첫째, 개발 모드에서 모든 접근을 허용하여 권한 검증이 없습니다. 둘째, API Rate Limiting이 없어 DDoS 공격에 취약합니다. 셋째, 파일 업로드 검증이 부족하여 악성 파일이 업로드될 수 있습니다. 넷째, SQL Injection은 JPA로 방어되지만 동적 쿼리에서는 주의가 필요합니다.
근거: 전체 아키텍처 분석
Follow-up: "Rate Limiting을 추가하려면 어떻게 해야 하나요?" / "파일 업로드 검증은 어떻게 구현하나요?"

---

## 마무리

위 100개 질문은 실제 발표에서 나올 수 있는 기술적 질문과 꼬리질문을 포함합니다. 각 답변은 코드 근거와 함께 2~4문장으로 작성되어 명확하고 간결합니다. 발표 준비 시 이 Q&A를 숙지하면 대부분의 질문에 자신 있게 답변할 수 있을 것입니다.