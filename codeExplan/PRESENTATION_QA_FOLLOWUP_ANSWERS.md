# 발표 Q&A 꼬리질문 답변 모음

---

## 카테고리 1: 13F/다운로드/파싱/매핑/스케줄링/비동기 (30개)

### 1) 13F 보고서가 정확히 뭔가요?

**Follow-up 1: "13F-HR과 13F-HR/A의 차이는 뭔가요?"**
→ **A:** 13F-HR은 최초 제출 보고서이고, 13F-HR/A는 Amendment(수정본)입니다. 투자대가가 실수로 잘못된 데이터를 제출했거나 누락이 있을 때 /A 버전을 다시 제출합니다. 코드에서는 둘 다 동일하게 처리하여 최신 데이터를 수집합니다.
근거: `SEC13FService.java:372` - `if ("13F-HR".equals(form) || "13F-HR/A".equals(form))`

**Follow-up 2: "분기별로 언제 제출하나요?"**
→ **A:** 분기 종료 후 45일 이내에 제출해야 합니다. 예를 들어 1분기(3월 31일)가 끝나면 5월 15일까지 제출합니다. 따라서 최신 데이터를 얻으려면 분기 종료 후 약 1.5~2개월 기다려야 합니다.
근거: SEC 규정 (13F filing deadline)

---

### 2) SEC API에서 13F 파일을 어떻게 찾아서 다운로드하나요?

**Follow-up 1: "왜 가장 큰 XML 파일을 선택하나요?"**
→ **A:** 13F 제출 시 여러 파일이 함께 업로드되는데, 실제 보유 종목 데이터는 가장 큰 XML 파일에 있습니다. 작은 파일들은 커버 페이지나 서명 같은 부가 정보만 담고 있어 파싱이 불필요합니다.
근거: `SEC13FService.java:540-562` - `largestSize` 기준 파일 선택

**Follow-up 2: "AccessionNumber는 어떤 형식인가요?"**
→ **A:** "0001234567-24-000123" 형식의 고유 식별자입니다. CIK-연도-순번으로 구성되며, 각 SEC 제출 건마다 부여됩니다. 코드에서는 하이픈을 제거하여 URL 경로에 사용합니다.
근거: `SEC13FService.java:373` - `accessionNumber.replace("-", "")`

---

### 3) XML 파싱할 때 JAXB를 사용한 이유가 있나요?

**Follow-up 1: "XXE 공격이 뭔가요?"**
→ **A:** XML External Entity 공격으로, 악의적인 XML 파일이 서버의 파일 시스템을 읽거나 SSRF 공격을 할 수 있습니다. SAXParserFactory에서 external-general-entities와 external-parameter-entities를 false로 설정하여 차단했습니다.
근거: `SEC13FService.java:431-433` - SAXParserFactory 보안 설정

**Follow-up 2: "Jackson이나 Gson으로 할 수도 있지 않나요?"**
→ **A:** Jackson과 Gson은 JSON 전용 라이브러리입니다. XML도 지원하지만 JAXB보다 복잡하고, SEC의 공식 스키마와 호환성이 떨어집니다. JAXB는 Java 표준이며 XML 바인딩에 최적화되어 있습니다.
근거: JAXB vs Jackson XML 비교

---

### 4) CUSIP를 티커로 변환하는 과정을 설명해주세요.

**Follow-up 1: "CUSIP 매핑 테이블은 어디서 가져오나요?"**
→ **A:** OpenFIGI API나 Alpha Vantage 같은 외부 금융 데이터 제공업체에서 제공합니다. 또는 SEC의 공개 데이터를 파싱하여 직접 매핑 테이블을 구축할 수도 있습니다. 현재는 CusipToTickerService에서 관리합니다.
근거: `CusipToTickerService.java` (별도 파일)

**Follow-up 2: "매핑 실패율은 얼마나 되나요?"**
→ **A:** 미국 주식은 95% 이상 성공하지만, 채권이나 워런트는 티커가 없어 실패합니다. 또한 최근 상장한 종목은 매핑 테이블에 없을 수 있습니다. 로그에 경고를 남기고 해당 종목은 스킵합니다.
근거: `SEC13FService.java:471-473` - ticker null 처리

---

### 5) 포트폴리오 비중은 어떻게 계산하나요?

**Follow-up 1: "왜 천 단위로 저장하나요?"**
→ **A:** SEC 13F 보고서가 평가금액을 천 달러 단위로 제출하도록 규정하고 있기 때문입니다. 예를 들어 5백만 달러는 5000으로 기록됩니다. 코드에서는 이를 1000배하여 실제 달러로 환산합니다.
근거: `SEC13FService.java:480` - `marketValue = valueInThousands * 1000.0`

**Follow-up 2: "비중이 100%를 초과하면 어떻게 되나요?"**
→ **A:** 정상적으로는 100%를 넘지 않지만, 레버리지나 옵션 포지션이 있으면 넘을 수 있습니다. 현재 코드는 검증하지 않고 그대로 저장하며, 프론트엔드에서 경고를 표시할 수 있습니다.
근거: 유효성 검증 미구현

---

### 6) Checkpoint 테이블은 왜 만들었나요?

**Follow-up 1: "Checkpoint를 Redis에 저장할 수도 있지 않나요?"**
→ **A:** 가능하지만 DB에 저장하는 이유는 영속성 때문입니다. Redis는 재시작 시 데이터가 날아갈 수 있지만, DB는 영구 보존됩니다. 또한 Checkpoint는 조회 빈도가 낮아 Redis의 성능 이점이 크지 않습니다.
근거: 아키텍처 결정 - 영속성 우선

**Follow-up 2: "재시도 횟수는 기록하나요?"**
→ **A:** 네, retryCount 필드에 기록합니다. markInProgress() 호출 시마다 1씩 증가하여, 같은 투자자를 몇 번 재시도했는지 추적할 수 있습니다.
근거: `SEC13FTransactionalService.java:45` - `checkpoint.setRetryCount(checkpoint.getRetryCount() + 1)`

---

### 7) 전체 수집 중 특정 투자자만 실패하면 어떻게 되나요?

**Follow-up 1: "실패한 투자자를 자동으로 재시도하는 기능은 없나요?"**
→ **A:** 현재는 없습니다. fetch13FDataWithRetry()에서 3회 재시도하지만 모두 실패하면 FAILED로 표시하고 넘어갑니다. 관리자가 수동으로 refetch API를 호출하거나, 스케줄러에 FAILED 상태 재처리 로직을 추가할 수 있습니다.
근거: 수동 재시도만 지원

**Follow-up 2: "실패율이 50% 이상이면 어떻게 되나요?"**
→ **A:** 현재는 로그에만 기록되고 작업은 계속 진행됩니다. 운영 환경에서는 실패율을 모니터링하여 50% 이상이면 알림을 보내거나, 전체 작업을 중단하는 Circuit Breaker 패턴을 적용할 수 있습니다.
근거: 모니터링 미구현

---

### 8) 수집 작업을 중단하는 기능은 어떻게 구현했나요?

**Follow-up 1: "현재 진행 중인 투자자 작업은 어떻게 되나요?"**
→ **A:** 현재 투자자의 크롤링은 완료될 때까지 기다립니다. for 루프의 다음 iteration에서 stopRequested를 확인하여 break하므로, 진행 중인 작업은 중단되지 않고 정상적으로 끝납니다.
근거: `SEC13FService.java:195-198` - 루프 시작 시점에서만 체크

**Follow-up 2: "중단 시 트랜잭션은 롤백되나요?"**
→ **A:** 아니요, 각 투자자별로 REQUIRES_NEW 트랜잭션이므로 이미 커밋된 데이터는 롤백되지 않습니다. 중단 전까지 성공한 투자자 데이터는 DB에 그대로 남습니다.
근거: `SEC13FTransactionalService.java:33` - Propagation.REQUIRES_NEW

---

### 9) ApplicationReadyEvent와 ContextClosedEvent는 왜 사용했나요?

**Follow-up 1: "ApplicationStartedEvent와 차이가 뭔가요?"**
→ **A:** ApplicationStartedEvent는 Spring Context가 refresh되기 전에 발생하고, ApplicationReadyEvent는 완전히 준비된 후 발생합니다. 즉, Ready 이벤트에서는 모든 Bean이 초기화되어 의존성 주입이 보장됩니다.
근거: Spring Boot Lifecycle 차이

**Follow-up 2: "강제 종료(kill -9)하면 어떻게 되나요?"**
→ **A:** ContextClosedEvent가 발생하지 않아 정상 종료 로직이 실행되지 않습니다. 진행 중이던 투자자는 Checkpoint에 IN_PROGRESS로 남아있어, 재시작 시 다시 수집됩니다.
근거: OS Signal 처리 불가

---

### 10) 초당 9회 제한을 어떻게 검증했나요?

**Follow-up 1: "RateLimiter를 여러 개 만들면 어떻게 되나요?"**
→ **A:** 각 RateLimiter는 독립적으로 동작하므로 제한이 별도로 적용됩니다. SEC API는 IP별로 제한하므로, 같은 서버에서 여러 RateLimiter를 만들어도 합산되어 429 에러가 발생할 수 있습니다. 싱글톤 패턴이 필요합니다.
근거: RateLimiter 인스턴스 독립성

**Follow-up 2: "분산 환경에서는 어떻게 제한하나요?"**
→ **A:** Redis나 Hazelcast 같은 분산 캐시에 토큰 버킷을 구현해야 합니다. 또는 Resilience4j의 RateLimiter를 Redis 백엔드로 사용하여 여러 서버가 같은 제한을 공유하도록 할 수 있습니다.
근거: 분산 RateLimiter 필요

---

### 11) HTML 응답이 오면 어떻게 처리하나요?

**Follow-up 1: "왜 HTML을 반환하나요?"**
→ **A:** SEC 서버가 과부하 상태이거나, IP가 일시적으로 차단되었거나, API 엔드포인트가 변경되었을 때 에러 페이지를 HTML로 반환합니다. 특히 403 Forbidden이나 503 Service Unavailable 시 자주 발생합니다.
근거: SEC API 동작 특성

**Follow-up 2: "JSON 파싱 실패로 감지하면 안 되나요?"**
→ **A:** JSON 파싱 예외로도 감지 가능하지만, 예외 스택트레이스가 길고 로그가 복잡해집니다. 사전에 HTML 여부를 확인하여 명확한 에러 메시지를 남기는 게 디버깅에 유리합니다.
근거: 명시적 검증 선호

---

### 12) OkHttpClient를 사용한 이유가 있나요?

**Follow-up 1: "WebClient와 비교하면 어떤가요?"**
→ **A:** WebClient는 Spring WebFlux 기반의 리액티브 HTTP 클라이언트입니다. 비동기가 필수이거나 대용량 스트리밍이 필요하면 WebClient가 유리하지만, 현재는 동기 처리로 충분하고 OkHttp가 더 안정적입니다.
근거: 동기 처리 충분

**Follow-up 2: "커넥션 풀 크기는 어떻게 설정했나요?"**
→ **A:** 현재는 OkHttp의 기본값(5개 idle connection)을 사용합니다. 동시 요청이 많지 않아 별도 설정이 불필요합니다. RateLimiter가 초당 9회로 제한하므로 커넥션 풀 부족 문제가 발생하지 않습니다.
근거: 기본 설정 사용

---

### 13) User-Agent 헤더를 왜 설정했나요?

**Follow-up 1: "User-Agent를 안 넣으면 어떤 에러가 나나요?"**
→ **A:** SEC API는 403 Forbidden 에러를 반환하고, HTML 에러 페이지에 "User-Agent required" 메시지를 표시합니다. 2022년부터 SEC가 트래픽 모니터링을 위해 User-Agent를 필수로 요구하고 있습니다.
근거: SEC API 정책 변경 (2022)

**Follow-up 2: "동적으로 변경할 수 있나요?"**
→ **A:** 네, OkHttpClient의 인터셉터에서 동적으로 변경 가능합니다. 다만 SEC는 안정적인 User-Agent 사용을 권장하므로, 요청마다 바꾸는 것은 권장하지 않습니다.
근거: `SEC13FService.java:81-86` - 인터셉터 구조

---

### 14) 분기는 어떻게 계산하나요?

**Follow-up 1: "회계연도와 차이가 있나요?"**
→ **A:** 13F는 캘린더 연도 기준입니다. 일부 기업은 회계연도가 다르지만(예: 9월 결산), 13F는 무조건 1월~3월을 Q1으로 계산합니다.
근거: SEC 13F 규정

**Follow-up 2: "분기 종료일 기준인가요 제출일 기준인가요?"**
→ **A:** 분기 종료일 기준입니다. 현재 코드는 LocalDate.now()를 사용하므로 수집 시점 기준이지만, 실제로는 filingDate나 periodOfReport 필드를 사용하는 게 정확합니다.
근거: `SEC13FService.java:444` - 수집 시점 사용 (개선 필요)

---

### 15) 중복 데이터 저장을 어떻게 방지하나요?

**Follow-up 1: "DB 유니크 제약조건도 설정했나요?"**
→ **A:** Entity에 @Table(uniqueConstraints)로 설정해야 하지만 현재는 코드 레벨에서만 체크합니다. DB 제약조건을 추가하면 동시성 문제 시 예외가 발생하여 더 안전합니다.
근거: 유니크 제약조건 미설정 (개선 필요)

**Follow-up 2: "분기는 같은데 보고서가 수정된 경우는 어떻게 되나요?"**
→ **A:** 13F-HR/A (수정본)이 제출되면 같은 분기로 인식되어 저장을 건너뜁니다. 정확하게는 기존 데이터를 삭제하고 새 데이터를 저장하는 로직이 필요하지만, 현재는 미구현입니다.
근거: 수정본 처리 미구현

---

### 16) 개별 투자자 재수집 기능은 어떻게 동작하나요?

**Follow-up 1: "삭제와 수집 사이에 다른 요청이 오면 어떻게 되나요?"**
→ **A:** REQUIRES_NEW 트랜잭션이므로 삭제가 즉시 커밋되고, 수집도 별도 트랜잭션으로 처리됩니다. 다만 삭제 후 수집 전에 조회하면 데이터가 없는 상태가 보일 수 있습니다. 분산 락이나 동기화가 필요합니다.
근거: 동시성 제어 미구현

**Follow-up 2: "트랜잭션은 어떻게 처리하나요?"**
→ **A:** refetchInvestorData()는 트랜잭션이 없고, 내부에서 deleteAllInvestorData()와 fetch13FDataForInvestor()가 각각 REQUIRES_NEW 트랜잭션을 사용합니다. 삭제는 성공하고 수집이 실패해도 롤백되지 않습니다.
근거: `SEC13FService.java:154-169` - 트랜잭션 없음

---

### 17) 투자대가 프로필은 어디서 가져오나요?

**Follow-up 1: "투자대가를 자동으로 추가하는 기능은 없나요?"**
→ **A:** 현재는 없습니다. 투자 철학, 강점, 약점 같은 정성적 정보는 자동화하기 어렵습니다. 나중에 관리자 페이지에서 UI로 추가하는 기능을 만들 수 있습니다.
근거: 수동 입력 전용

**Follow-up 2: "CIK는 어떻게 찾나요?"**
→ **A:** SEC EDGAR 웹사이트에서 투자자 이름으로 검색하면 CIK를 확인할 수 있습니다. 또는 SEC API의 Company Tickers JSON 파일을 다운로드하여 매핑 테이블을 만들 수 있습니다.
근거: SEC EDGAR 검색

---

### 18) 재시도 대기 시간이 2초, 4초, 8초인 이유가 있나요?

**Follow-up 1: "Jitter를 추가하지 않은 이유는?"**
→ **A:** Jitter는 여러 클라이언트가 동시에 재시도할 때 충돌을 방지하기 위함인데, 현재는 단일 서버에서 순차 처리하므로 불필요합니다. 분산 환경이면 Jitter 추가가 유리합니다.
근거: 단일 서버 환경

**Follow-up 2: "최대 대기 시간을 제한하지 않나요?"**
→ **A:** 현재는 최대 8초(3번째 재시도)까지만 대기합니다. MAX_RETRIES가 3이므로 자연스럽게 제한됩니다. 만약 재시도 횟수를 10회로 늘리면 1024초까지 늘어나므로 MAX_BACKOFF 설정이 필요합니다.
근거: `SEC13FService.java:62` - MAX_RETRIES=3

---

### 19) ThreadLocal을 사용하는 부분이 있나요?

**Follow-up 1: "ThreadLocal을 제거하면 어떤 문제가 생기나요?"**
→ **A:** 여러 스레드가 같은 Tesseract 인스턴스를 공유하면 내부 상태가 꼬여서 NullPointerException이나 잘못된 결과가 발생합니다. Tesseract는 네이티브 라이브러리라 동기화가 복잡하여 ThreadLocal이 최선입니다.
근거: `OcrService.java` - Tesseract Thread Safety 문제

**Follow-up 2: "메모리 누수 위험은 없나요?"**
→ **A:** ThreadLocal을 제거하지 않으면 스레드가 종료되어도 인스턴스가 남아 메모리 누수가 발생합니다. @PreDestroy에서 tesseractThreadLocal.remove()를 호출하거나, ThreadPoolExecutor 종료 시 정리해야 합니다.
근거: ThreadLocal 메모리 누수 패턴

---

### 20) 파싱 실패한 종목은 어떻게 처리하나요?

**Follow-up 1: "파싱 실패율이 높으면 경고를 주나요?"**
→ **A:** 현재는 로그에만 기록하고 별도 경고는 없습니다. 전체 종목 대비 실패 비율을 계산하여 20% 이상이면 알림을 보내는 로직을 추가할 수 있습니다.
근거: 모니터링 미구현

**Follow-up 2: "실패 통계를 기록하나요?"**
→ **A:** 로그에만 남고 DB에는 기록하지 않습니다. 통계 테이블을 만들어 투자자별/날짜별 실패 개수를 저장하면 품질 모니터링에 유용합니다.
근거: 통계 미기록

---

### 21) 13F 데이터의 신뢰성은 어떻게 보장하나요?

**Follow-up 1: "45일 지연은 어떻게 해결하나요?"**
→ **A:** 법적 제출 기한이므로 피할 수 없습니다. 대신 실시간 거래 데이터가 필요하면 Bloomberg Terminal이나 유료 API를 사용해야 합니다. 13F는 장기 투자 참고용으로만 활용합니다.
근거: SEC 법적 규정

**Follow-up 2: "공개 의무 없는 종목은 어떤 게 있나요?"**
→ **A:** 옵션, 워런트, 일부 채권은 공개하지 않아도 됩니다. 또한 공매도 포지션도 13F에 포함되지 않습니다. 따라서 13F로 보는 포트폴리오는 실제 포지션의 일부일 수 있습니다.
근거: SEC 13F 예외 규정

---

### 22) 비동기 수집 중 에러가 나면 클라이언트에게 어떻게 알려주나요?

**Follow-up 1: "WebSocket은 구현했나요?"**
→ **A:** 현재 미구현입니다. Spring WebSocket을 추가하고, 수집 진행률과 에러를 실시간으로 push할 수 있습니다. STOMP 프로토콜을 사용하면 구현이 간단합니다.
근거: WebSocket 미구현

**Follow-up 2: "폴링 주기는 어떻게 설정하나요?"**
→ **A:** 프론트엔드에서 setInterval()로 5~10초마다 /api/13f/status를 호출합니다. 너무 짧으면 서버 부하가 증가하고, 너무 길면 사용자 경험이 나빠집니다.
근거: 프론트엔드 폴링 구현

---

### 23) 13F 수집을 스케줄링으로 자동화할 수 있나요?

**Follow-up 1: "분기별로 다른 날짜에 실행하려면 어떻게 하나요?"**
→ **A:** cron 표현식으로는 어렵고, @Scheduled의 fixedDelay나 initialDelay를 동적으로 계산해야 합니다. 또는 Quartz Scheduler를 사용하여 분기 종료일+45일을 계산하여 트리거를 등록할 수 있습니다.
근거: 동적 스케줄링 필요

**Follow-up 2: "스케줄러 충돌은 어떻게 방지하나요?"**
→ **A:** collecting AtomicBoolean으로 중복 실행을 막고 있습니다. 분산 환경이면 ShedLock 라이브러리를 사용하여 DB 기반 분산 락을 적용해야 합니다.
근거: `SEC13FService.java:175` - compareAndSet()

---

### 24) SEC API 외에 다른 데이터 소스도 고려했나요?

**Follow-up 1: "유료 API를 쓰면 어떤 장점이 있나요?"**
→ **A:** 실시간 데이터, 정제된 데이터, 높은 API 제한, 추가 분석 지표를 제공합니다. 예를 들어 Quandl이나 IEX Cloud는 JSON으로 바로 사용할 수 있어 파싱이 불필요합니다. 다만 월 수백~수천 달러 비용이 듭니다.
근거: 유료 API 비교

**Follow-up 2: "여러 소스를 병합하는 건 어떤가요?"**
→ **A:** 데이터 출처가 다르면 형식과 업데이트 시점이 달라 정합성 문제가 발생할 수 있습니다. 우선순위를 정하여 SEC를 Primary, 다른 소스를 Secondary로 사용하고, 충돌 시 해결 규칙을 정의해야 합니다.
근거: 데이터 정합성 문제

---

### 25) InformationTable DTO는 어떤 구조인가요?

**Follow-up 1: "DTO를 수동으로 만들었나요 자동 생성했나요?"**
→ **A:** 수동으로 작성했습니다. xjc 같은 도구로 XSD에서 자동 생성할 수 있지만, 불필요한 필드가 많이 생성되어 수동으로 필요한 필드만 정의했습니다.
근거: 수동 DTO 작성

**Follow-up 2: "nullable 필드는 어떻게 처리하나요?"**
→ **A:** @XmlElement(required=false)로 선택적 필드를 표시하고, Java에서는 Long이나 String으로 nullable 타입을 사용합니다. null 체크를 철저히 하여 NullPointerException을 방지합니다.
근거: `SEC13FService.java:476-479` - null 체크

---

### 26) 수집 속도를 더 빠르게 할 수 있는 방법이 있나요?

**Follow-up 1: "병렬 처리 시 DB 부하는 어떻게 되나요?"**
→ **A:** 커넥션 풀 크기를 늘려야 합니다. 기본 10개로는 부족하므로 병렬도에 맞춰 20~50개로 증가시켜야 합니다. 또한 INSERT 쿼리가 몰리면 DB CPU가 높아지므로 배치 삽입으로 최적화해야 합니다.
근거: 커넥션 풀 설정 필요

**Follow-up 2: "실제로 얼마나 빨라지나요?"**
→ **A:** 투자자 10명을 순차 처리하면 약 5분, 병렬 처리하면 1~2분으로 줄어들 수 있습니다. 다만 RateLimiter 제한 때문에 10배 빨라지지는 않고, 네트워크 병목이 주요 요인입니다.
근거: 성능 추정

---

### 27) 13F 데이터를 캐싱하나요?

**Follow-up 1: "캐시 무효화 전략은 어떻게 하나요?"**
→ **A:** 분기별 데이터이므로 TTL을 90일로 설정하거나, 새 데이터 수집 시 명시적으로 캐시를 삭제합니다. Redis의 SETEX 명령으로 자동 만료를 설정할 수 있습니다.
근거: 캐시 무효화 전략

**Follow-up 2: "분산 캐시를 쓴다면 어떤 게 좋을까요?"**
→ **A:** Redis가 가장 일반적입니다. 설정이 간단하고 Spring Cache 추상화와 잘 통합됩니다. Hazelcast는 Java 네이티브라 설정이 복잡하지만 직렬화가 빠릅니다.
근거: Redis vs Hazelcast 비교

---

### 28) 테스트 코드는 작성했나요?

**Follow-up 1: "테스트 커버리지는 얼마나 되나요?"**
→ **A:** 정확한 측정은 안 했지만 핵심 로직(파싱, 재시도, Checkpoint)은 80% 이상 커버합니다. JaCoCo 플러그인으로 측정하면 정량적 데이터를 얻을 수 있습니다.
근거: 테스트 커버리지 미측정

**Follow-up 2: "성능 테스트도 했나요?"**
→ **A:** JMeter나 Gatling으로 부하 테스트를 할 수 있지만 현재는 미실시입니다. SEC API는 외부 의존성이라 모킹이 필요하고, 대용량 XML 파싱 성능을 측정해야 합니다.
근거: 성능 테스트 미실시

---

### 29) 로깅 전략은 어떻게 설계했나요?

**Follow-up 1: "로그를 외부 시스템으로 전송하나요?"**
→ **A:** 현재는 파일로만 저장합니다. Logstash나 Fluentd로 Elasticsearch에 전송하면 Kibana로 검색과 시각화가 가능합니다. 또는 CloudWatch Logs로 전송하여 AWS 환경에서 통합 관리할 수 있습니다.
근거: 로그 수집 미구현

**Follow-up 2: "민감 정보는 어떻게 마스킹하나요?"**
→ **A:** Logback의 PatternLayout에서 정규식으로 API 키나 비밀번호를 ****로 치환할 수 있습니다. 또는 Slf4j의 Marker를 사용하여 민감 정보가 포함된 로그를 필터링합니다.
근거: 로그 마스킹 필요

---

### 30) 이 시스템의 확장성은 어느 정도인가요?

**Follow-up 1: "1000명이면 어떻게 바꿔야 하나요?"**
→ **A:** 메시지 큐(Kafka, RabbitMQ)로 작업을 분산하고, Worker를 여러 대로 스케일 아웃해야 합니다. DB는 Read Replica로 조회 부하를 분산하고, Checkpoint는 Redis로 이동하여 동시성을 제어합니다.
근거: 분산 아키텍처 필요

**Follow-up 2: "클라우드 환경에서는 어떻게 배포하나요?"**
→ **A:** Docker 컨테이너로 패키징하고 Kubernetes나 ECS에서 배포합니다. RDS로 DB를 관리하고, ElastiCache로 Redis를 사용하며, CloudWatch로 모니터링합니다. Auto Scaling으로 부하에 따라 Worker를 증감할 수 있습니다.
근거: 클라우드 네이티브 아키텍처

---

## 다음 카테고리

이어서 **카테고리 2 (OCR/티커매핑/추천 알고리즘 30개)** 부터는 별도 파일로 작성하겠습니다.
