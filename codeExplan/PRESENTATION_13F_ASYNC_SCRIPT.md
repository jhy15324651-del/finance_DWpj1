# 13F 비동기 처리 발표 스크립트 (45초)

## Q: "13F 보고서 다운로드할 때 비동기로 처리했다고 했는데, 어떤 식으로 어떻게 구현했나요?"

---

### 발표 스크립트 (45초)

네, 13F 데이터 수집은 **완전 비동기 아키텍처**로 구현했습니다.

**첫째, 요청 흐름**입니다.
Controller에서 `/api/13f/start` 엔드포인트를 호출하면, SEC13FService의 `startAsyncCollection()` 메서드가 실행됩니다. 이 메서드에는 `@Async` 어노테이션이 붙어있어, Spring의 기본 ThreadPoolTaskExecutor가 별도 스레드를 할당하여 메인 스레드와 분리됩니다.

**둘째, 스레드 분리 메커니즘**입니다.
Application 클래스에 `@EnableAsync`를 선언하여 Spring의 비동기 기능을 활성화했습니다. 이로 인해 `@Async` 메서드는 자동으로 스레드 풀에서 실행되며, 클라이언트는 즉시 응답을 받고 백그라운드에서 수집 작업이 진행됩니다.

**셋째, 상태와 예외 처리**입니다.
`AtomicBoolean collecting` 플래그로 중복 실행을 방지하고, 각 투자자별로 Checkpoint 테이블에 IN_PROGRESS, SUCCESS, FAILED 상태를 기록합니다. 트랜잭션은 SEC13FTransactionalService로 분리하여 `Propagation.REQUIRES_NEW`를 적용, 개별 투자자 실패가 전체 작업을 롤백시키지 않도록 했습니다.

**넷째, 동시성 제한**입니다.
SEC API의 초당 10회 제한을 준수하기 위해 Guava RateLimiter를 초당 9회로 설정했습니다. 또한 `stopRequested` 플래그로 중단 요청을 감지하여 안전하게 작업을 종료할 수 있습니다.

---

## 예상 꼬리질문 5개 & 모범답변

### 1) Q: "왜 @Async를 쓰면 스레드가 분리되는 건가요? 내부 원리가 궁금합니다."

**A:** Spring AOP 프록시 메커니즘 때문입니다. `@EnableAsync`를 선언하면 Spring은 `AsyncAnnotationBeanPostProcessor`를 등록하는데, 이 프로세서가 `@Async` 메서드를 감지하여 프록시 객체를 생성합니다. 실제 메서드 호출 시 프록시가 ThreadPoolTaskExecutor의 submit() 메서드를 통해 별도 스레드에서 실행하도록 위임합니다. 이때 중요한 점은 같은 클래스 내부에서 @Async 메서드를 호출하면 프록시를 거치지 않아 비동기로 동작하지 않는다는 것입니다.

**Follow-up:** "그럼 ThreadPool 크기는 어떻게 설정하셨나요?" / "프록시를 거치지 않는 경우는 어떻게 해결하나요?"

---

### 2) Q: "RateLimiter를 초당 9회로 설정한 이유가 있나요? 왜 10회가 아니라 9회인가요?"

**A:** SEC API의 공식 제한은 초당 10회이지만, 여유를 두기 위해 9회로 설정했습니다. 네트워크 지연이나 요청 시점의 미세한 차이로 인해 10회 제한을 초과할 위험이 있기 때문입니다. Guava RateLimiter는 Token Bucket 알고리즘을 사용하는데, `acquire()` 메서드가 호출되면 토큰이 소진될 때까지 스레드를 블로킹합니다. 만약 429 에러가 발생하면 `Retry-After` 헤더를 파싱하여 해당 시간만큼 대기 후 재시도합니다.

**Follow-up:** "Token Bucket 알고리즘이 뭔가요?" / "429 에러 외에 다른 에러는 어떻게 처리하나요?"

---

### 3) Q: "Propagation.REQUIRES_NEW를 왜 사용했나요? 일반 REQUIRED와 차이가 뭔가요?"

**A:** 개별 투자자의 저장 실패가 전체 트랜잭션에 영향을 주지 않도록 하기 위해서입니다. REQUIRED는 기존 트랜잭션이 있으면 참여하고, REQUIRES_NEW는 항상 새로운 트랜잭션을 시작합니다. 예를 들어 워렌 버핏 데이터 저장에 성공했는데 레이 달리오 데이터 저장에 실패했을 때, REQUIRED를 쓰면 두 작업 모두 롤백되지만, REQUIRES_NEW를 쓰면 버핏 데이터는 커밋되고 달리오만 실패 처리됩니다. Checkpoint 테이블에 상태를 기록하여 재실행 시 성공한 투자자는 건너뛸 수 있습니다.

**Follow-up:** "Checkpoint를 쓰지 않고 재시도하면 어떤 문제가 생기나요?" / "트랜잭션 격리 수준은 어떻게 설정했나요?"

---

### 4) Q: "AtomicBoolean을 왜 사용했나요? 일반 boolean과 차이가 뭔가요?"

**A:** 멀티스레드 환경에서 동시성 문제를 방지하기 위해서입니다. 일반 boolean은 여러 스레드가 동시에 읽고 쓸 때 race condition이 발생할 수 있습니다. AtomicBoolean은 CAS(Compare-And-Swap) 연산을 사용하여 원자적으로 값을 변경합니다. 예를 들어 `collecting.compareAndSet(false, true)`는 현재 값이 false일 때만 true로 변경하고, 변경 성공 여부를 반환합니다. 이를 통해 두 개의 스레드가 동시에 수집 작업을 시작하려 해도 하나만 성공하도록 보장합니다.

**Follow-up:** "CAS 연산이 내부적으로 어떻게 동작하나요?" / "synchronized와 비교하면 어떤 차이가 있나요?"

---

### 5) Q: "재시도 로직은 어떻게 구현했나요? 무한 재시도하지 않나요?"

**A:** Exponential Backoff 전략으로 최대 3회까지 재시도합니다. `fetch13FDataWithRetry()` 메서드에서 구현했는데, 첫 번째 실패 시 2초, 두 번째 4초, 세 번째 8초 대기 후 재시도합니다. 모든 예외를 재시도하는 것이 아니라 `isRetryable()` 메서드로 네트워크 타임아웃, 429/503 에러 등 일시적 오류만 재시도하고, 데이터 파싱 오류나 인증 오류는 즉시 실패 처리합니다. 3회 실패 시 Checkpoint에 FAILED 상태와 예외 메시지를 기록하고 다음 투자자로 넘어갑니다.

**Follow-up:** "왜 Linear가 아니라 Exponential Backoff를 선택했나요?" / "재시도 횟수와 대기 시간은 어떤 기준으로 정했나요?"

---

## 핵심 코드 근거

- **요청 흐름**: `SEC13FController.java :: startCollection()` → `SEC13FService.java :: startAsyncCollection()`
- **스레드 분리**: `FinanceDWpj1Application.java :: @EnableAsync` + `SEC13FService.java :: @Async`
- **상태 처리**: `SEC13FTransactionalService.java :: markInProgress/markSuccess/markFailed` (REQUIRES_NEW)
- **동시성 제한**: `SEC13FService.java :: RateLimiter.create(9.0)` + `AtomicBoolean stopRequested/collecting`
- **재시도 로직**: `SEC13FService.java :: fetch13FDataWithRetry()` + `isRetryable()` + `calculateBackoff()`