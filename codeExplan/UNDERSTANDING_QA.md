# Finance DWpj1 - 이해 점검용 Q&A

> **목적**: 학습 일지를 읽고 나서 스스로 이해했는지 검증
> **답변 방식**: 각 질문 아래에 접힌 형식으로 정답 제공 (생각해본 후 펼쳐보기)

---

## 📌 섹션 1: 전체 아키텍처 이해

### Q1. 왜 이 프로젝트에서는 Entity를 바로 View로 넘기지 않았을까?

<details>
<summary>💡 정답 보기</summary>

**이유**:
1. **보안 문제**: Entity는 DB 컬럼 그대로 → 비밀번호 해시, 내부 ID 노출 위험
2. **강한 결합**: Entity 변경 시 View도 깨짐
3. **최소 권한 원칙**: View는 필요한 필드만 받아야 함

**예시**:
- Entity: `User { id, email, password, phoneNumber, ... }`
- DTO: `UserProfileDTO { nickname, email }` (비밀번호 제외!)

**참고 코드**: `UserSignupDTO`, `ContentReviewDTO`
</details>

---

### Q2. Spring MVC에서 Controller → Service → Repository 순서로 호출하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**계층 분리의 이유**:
1. **단일 책임 원칙**: 각 계층은 하나의 역할만
   - Controller: 요청/응답 처리
   - Service: 비즈니스 로직
   - Repository: 데이터 접근

2. **재사용성**: Service 로직을 다른 Controller에서도 사용 가능

3. **테스트 용이성**: Service만 단위 테스트 가능

**잘못된 설계**:
```java
// ❌ Controller에 비즈니스 로직
@PostMapping("/analyze")
public String analyze() {
    // OCR 처리
    // 유사도 계산
    // AI 호출
    // → Controller가 너무 무거움!
}
```

**올바른 설계**:
```java
// ⭕ Service로 분리
@PostMapping("/analyze")
public String analyze(MultipartFile file) {
    AnalysisResult result = portfolioService.analyzePortfolio(file);
    return "result";
}
```
</details>

---

### Q3. @Transactional이 없으면 어떤 문제가 생길까?

<details>
<summary>💡 정답 보기</summary>

**문제 상황**:
```java
public void softDeleteContent(Long id, String reason) {
    // 1. 콘텐츠 삭제
    content.softDelete();
    contentRepository.save(content);  // ✅ 성공

    // 2. 감사 로그 저장
    auditLogRepository.save(log);  // ❌ 오류 발생!

    // → 콘텐츠는 삭제됐는데 로그는 안 남음 (데이터 불일치!)
}
```

**@Transactional 사용 시**:
```java
@Transactional
public void softDeleteContent(Long id, String reason) {
    content.softDelete();
    contentRepository.save(content);
    auditLogRepository.save(log);  // 오류 발생 시 content도 롤백!
}
```

**추가 문제**: LAZY 로딩
```java
// @Transactional 없음
StockBoard board = boardRepository.findById(1L);
return board.getComments();  // LazyInitializationException!

// @Transactional 있음
@Transactional
public List<Comment> getComments(Long boardId) {
    StockBoard board = boardRepository.findById(boardId);
    return board.getComments();  // ✅ 정상 작동
}
```
</details>

---

## 📌 섹션 2: OCR 시스템 이해

### Q4. Tesseract OCR에서 ThreadLocal을 사용하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제**:
- Tesseract는 Thread-Safe하지 않음
- 10명이 동시에 이미지 업로드 → 같은 Tesseract 인스턴스 사용 → 오류 발생

**해결책 비교**:

❌ **방법 1**: synchronized (느림)
```java
private Tesseract tesseract = new Tesseract();

public synchronized String doOcr(BufferedImage image) {
    return tesseract.doOCR(image);
    // → 한 번에 1명만 처리 가능 (병목 현상)
}
```

⭕ **방법 2**: ThreadLocal (빠름)
```java
private ThreadLocal<Tesseract> tesseractThreadLocal = ThreadLocal.withInitial(() -> new Tesseract());

public String doOcr(BufferedImage image) {
    return tesseractThreadLocal.get().doOCR(image);
    // → 각 스레드마다 독립적인 Tesseract 인스턴스
    // → 10명 동시 처리 가능!
}
```

**결론**: ThreadLocal = 스레드당 별도 객체 → Thread-Safe + 고성능
</details>

---

### Q5. 이미지 전처리를 증권사별로 다르게 하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제**:
- 토스: 파란 배경 + 작은 글씨 + 그림자
- 미래에셋: 흰 배경 + 큰 글씨 + 단순 레이아웃

→ 하나의 전처리로 모든 증권사 대응 불가

**해결책**: 전략 패턴 (Strategy Pattern)
```java
// 인터페이스 정의
interface OcrPreprocessor {
    BufferedImage preprocess(BufferedImage image);
    BrokerType getSupportedBroker();
}

// 토스 전용
class TossPreprocessor implements OcrPreprocessor {
    public BufferedImage preprocess(BufferedImage image) {
        // 1. 파란 배경 제거
        // 2. 대비 증가
        // 3. 노이즈 제거
    }
}

// 기본
class DefaultPreprocessor implements OcrPreprocessor {
    public BufferedImage preprocess(BufferedImage image) {
        // 그레이스케일 변환만
    }
}
```

**장점**:
1. 새 증권사 추가 시 → 새 Preprocessor 클래스만 추가
2. 기존 코드 수정 불필요
3. 각 증권사별 최적화 가능

**참고 코드**: `TossPreprocessor`, `DefaultPreprocessor`, `OcrService:208-214`
</details>

---

### Q6. 한글 종목명을 영문 티커로 변환하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제 상황**:
- 사용자 포트폴리오: "삼성전자 30%, 테슬라 25%"
- SEC 13F 데이터: "AAPL 40%, TSLA 30%"
- → 비교 불가능!

**해결**:
```java
// TickerMappingService.java
Map<String, String> koreanToTickerMap = Map.of(
    "삼성전자", "SSNLF",  // 미국 ADR 티커
    "테슬라", "TSLA",
    "애플", "AAPL"
);

String mappedText = tickerMappingService.applyMappingToText(ocrText);
// "삼성전자 30%" → "SSNLF 30%"
```

**추가 이유**:
- SEC 13F 데이터는 미국 상장 종목만 포함
- 한국 상장 삼성전자는 미국에서 SSNLF(ADR)로 거래
- 통일된 티커로 변환해야 유사도 계산 가능

**참고 코드**: `TickerMappingService:72-85`, `OcrService:222-224`
</details>

---

## 📌 섹션 3: 포트폴리오 매칭 알고리즘

### Q7. 코사인 유사도에서 비중(가중치)을 고려하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**단순 겹침 계산의 문제**:
```
포트폴리오 A: AAPL 1%, TSLA 1%, MSFT 98%
포트폴리오 B: AAPL 50%, TSLA 50%

겹치는 종목: 2개 (AAPL, TSLA)
→ 유사도 100%? (잘못됨!)
```

**코사인 유사도의 장점**:
```
A = [1, 1, 98]
B = [50, 50, 0]

cos(θ) = (1*50 + 1*50 + 98*0) / (√10000 × √5000) ≈ 0.14
→ 유사도 14% (정확함!)
```

**핵심**: 종목의 "비중"을 벡터로 표현 → 포트폴리오 "방향성" 측정

**비유**:
- 단순 겹침: "너랑 나 둘 다 사과 먹네?" (양은 무시)
- 코사인 유사도: "너는 사과 1개, 나는 사과 100개" (양도 고려)

**참고 코드**: `PortfolioMatchingService:218-252`
</details>

---

### Q8. 최종 매칭 점수에서 유사도·겹침·상위종목을 혼합하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**각 지표의 한계**:

1. **코사인 유사도만**: 비중은 비슷하지만 종목이 안 겹칠 수 있음
   ```
   A: AAPL 50%, GOOGL 50%
   B: MSFT 50%, AMZN 50%
   → 비중 패턴은 비슷하지만 종목은 0개 겹침
   ```

2. **종목 겹침만**: 비중이 전혀 다를 수 있음
   ```
   A: AAPL 90%, TSLA 10%
   B: AAPL 10%, TSLA 90%
   → 종목은 100% 겹치지만 투자 철학은 정반대
   ```

3. **상위 종목 겹침**: 소액 보유 종목은 중요도 낮음
   ```
   A의 TOP 5: AAPL, MSFT, GOOGL, AMZN, TSLA (전체의 80%)
   B의 TOP 5: AAPL, MSFT, GOOGL, AMZN, TSLA (전체의 75%)
   → 핵심 포트폴리오 유사!
   ```

**혼합 공식**:
```java
matchScore = similarity * 0.4          // 코사인 유사도
           + overlapPercentage * 0.4   // 종목 겹침 비율
           + topOverlapScore * 0.2;    // 상위 5개 종목 겹침
```

**이유**: "사람이 느끼는 유사도"에 가장 근접

**참고 코드**: `PortfolioMatchingService:66-79`
</details>

---

### Q9. 레버리지 ETF를 기초자산으로 환산하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제**:
```
사용자: TSLL 10% 보유 (테슬라 2배 레버리지 ETF)
워렌 버핏: TSLA 20% 보유 (테슬라 현물)

환산 없이 비교 → 겹치는 종목 0개 (잘못됨!)
```

**환산 후**:
```
사용자: TSLL 10% → TSLA 20% 노출
워렌 버핏: TSLA 20%

→ 동일한 노출! 유사도 높게 측정 (정확함!)
```

**환산 규칙**:
- TSLL (2배 레버리지): 10% → TSLA 20%
- SQQQ (-3배 인버스): 10% → QQQ -30%

**코드 참고**: `PortfolioExposureNormalizer`, `PortfolioMatchingService:115-145`
</details>

---

## 📌 섹션 4: SEC 13F 데이터 수집

### Q10. RateLimiter를 초당 9회로 설정한 이유는?

<details>
<summary>💡 정답 보기</summary>

**SEC API 제한**: 초당 10회

**왜 9회?**
1. **안전 마진**: 네트워크 지연, 타이밍 오차 고려
2. **여유 확보**: 10회 정확히 맞추면 오차로 초과 가능
3. **IP 차단 방지**: 한 번이라도 초과하면 차단 위험

**코드**:
```java
private final RateLimiter rateLimiter = RateLimiter.create(9.0);

// API 호출 전
rateLimiter.acquire();  // 필요시 자동 대기
httpClient.newCall(request).execute();
```

**동작**:
- 0.000초: API 호출 1
- 0.111초: API 호출 2 (111ms 대기)
- 0.222초: API 호출 3 (111ms 대기)
- ...

**참고 코드**: `SEC13FService:53`, `SEC13FService:121-122`
</details>

---

### Q11. CUSIP를 Ticker로 변환하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제**:
- SEC 13F XML: CUSIP 코드 사용 (`037833100`)
- 일반 투자자: Ticker로 알고 있음 (`AAPL`)
- → 사용자가 CUSIP를 모르면 이해 불가

**CUSIP란?**
- Committee on Uniform Securities Identification Procedures
- 미국 증권 고유 식별 번호 (9자리)
- 예: `037833100` = Apple Inc.

**변환 과정**:
```java
// OpenFIGI API 호출
String ticker = cusipToTickerService.convertCusipToTicker("037833100");
// → "AAPL"

Investor13FHolding holding = Investor13FHolding.builder()
    .ticker(ticker)  // "AAPL" (사용자가 아는 형태)
    .companyName("Apple Inc.")
    .shares(1000000)
    .build();
```

**API**: OpenFIGI (일일 5,000회 무료)

**참고 코드**: `SEC13FService:470-474`, `CusipToTickerService`
</details>

---

### Q12. Checkpoint를 저장하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제 상황**:
```
1. 워렌 버핏 수집 완료 ✅
2. 캐시 우드 수집 완료 ✅
3. 레이 달리오 수집 중...
4. [서버 재시작] → 모든 진행 상황 유실!
5. 다시 1번부터 시작... (비효율)
```

**Checkpoint 사용 시**:
```
1. 워렌 버핏 수집 완료 → Checkpoint: SUCCESS ✅
2. 캐시 우드 수집 완료 → Checkpoint: SUCCESS ✅
3. 레이 달리오 수집 중 → Checkpoint: IN_PROGRESS
4. [서버 재시작]
5. Resume: 1, 2번은 건너뛰고 3번부터 재시도!
```

**Checkpoint 상태**:
- `PENDING`: 아직 시작 안 함
- `IN_PROGRESS`: 수집 중
- `SUCCESS`: 성공
- `FAILED`: 실패 (재시도 가능)
- `SKIPPED`: 데이터 없음

**참고 코드**: `SEC13FService:208-221`, `SecCollectorCheckpoint.java`
</details>

---

## 📌 섹션 5: AI 통합 설계

### Q13. OpenAI와 Gemini를 둘 다 지원하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**이유**:
1. **비용 절감**: Gemini 무료 할당량 활용
   - OpenAI GPT-4: $0.03/1K tokens (비쌈)
   - Gemini 2.0: 무료 할당량 많음

2. **장애 대응**: OpenAI 다운 시 Gemini로 즉시 전환

3. **성능 비교**: 두 모델의 품질 테스트 가능

**전환 방법**:
```properties
# application.properties
ai.provider=gemini  # 또는 openai

# 환경변수만 변경 → 코드 수정 없음!
```

**Strategy Pattern 적용**:
```java
@Service
@RequiredArgsConstructor
public class GPTService {
    private final AiClient aiClient;  // 인터페이스

    public String translate(String text) {
        AiRequest request = AiRequest.builder()
            .user("번역: " + text)
            .build();

        return aiClient.generate(request).getText();
        // → 실제 구현체는 Spring이 환경변수 기준으로 주입
    }
}
```

**참고 코드**: `GPTService:23`, `AiClient.java`, `OpenAiClient`, `GeminiClient`
</details>

---

### Q14. AI에게 JSON 형식으로 응답받는 이유는?

<details>
<summary>💡 정답 보기</summary>

**일반 텍스트 응답의 문제**:
```
AI: "추천 종목은 Apple, Tesla, Microsoft입니다. 비중은 각각 30%, 25%, 20%입니다."

→ 파싱 방법:
1. 정규식으로 종목명 추출? (복잡)
2. 쉼표로 split? ("Apple, Inc." 같은 경우 오류)
3. 비중은 어떻게 매칭? (순서 보장 안 됨)
```

**JSON 응답의 장점**:
```json
{
  "stocks": [
    { "ticker": "AAPL", "weightPercent": 30.0 },
    { "ticker": "TSLA", "weightPercent": 25.0 },
    { "ticker": "MSFT", "weightPercent": 20.0 }
  ]
}
```

→ Jackson으로 즉시 DTO 변환!
```java
ObjectMapper mapper = new ObjectMapper();
PortfolioRecommendation dto = mapper.readValue(response, PortfolioRecommendation.class);
```

**주의사항**: AI가 ```json ... ``` 마크다운을 붙이는 경우
```java
if (response.startsWith("```json")) {
    response = response.substring(7);  // 제거
}
```

**참고 코드**: `GPTService:240-250`, `GPTService:193-226`
</details>

---

### Q15. temperature를 0.3으로 낮게 설정하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**temperature란?**
- AI 응답의 "랜덤성" 조절 파라미터
- 0.0: 완전히 결정론적 (항상 동일한 답변)
- 1.0: 매우 창의적 (매번 다른 답변)

**값에 따른 차이**:

**temperature = 0.9 (높음)**:
```
프롬프트: "Apple의 투자 가치는?"

응답 1: "Apple은 혁신의 아이콘이며..."
응답 2: "Apple은 브랜드 파워가 강하고..."
응답 3: "Apple은 생태계가 탄탄하며..."
→ 매번 다른 표현 (창의적이지만 일관성 낮음)
```

**temperature = 0.3 (낮음)**:
```
프롬프트: "Apple의 투자 가치는?"

응답 1: "Apple은 강력한 브랜드와 생태계를 보유하고 있으며..."
응답 2: "Apple은 강력한 브랜드와 생태계를 보유하고 있어..."
응답 3: "Apple은 강력한 브랜드와 생태계를 가지고 있고..."
→ 거의 동일한 내용 (일관적)
```

**포트폴리오 추천에서 0.3 선택 이유**:
1. **일관성 중요**: 같은 사용자가 다시 요청 시 비슷한 결과 필요
2. **신뢰성**: 매번 다른 종목 추천 → 사용자 혼란
3. **검증 가능**: 재현 가능한 결과

**참고 코드**: `GPTService:231` (temperature: 0.3), `GPTService:310` (temperature: 0.3)
</details>

---

## 📌 섹션 6: 보안 설계

### Q16. BCrypt로 암호화한 비밀번호는 복호화 가능한가?

<details>
<summary>💡 정답 보기</summary>

**답**: **불가능!** (일방향 해시)

**동작 방식**:
```java
// 회원가입
String plainPassword = "myPassword123";
String encoded = passwordEncoder.encode(plainPassword);
// → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

// 로그인
String inputPassword = "myPassword123";
boolean matches = passwordEncoder.matches(inputPassword, encoded);
// → true (일치)

// 복호화?
String decrypted = passwordEncoder.decode(encoded);
// → 불가능! (메서드 자체가 없음)
```

**왜 복호화 불가?**
- BCrypt는 해시 함수 (단방향)
- 입력 → 출력은 가능하지만 출력 → 입력은 불가능
- 로그인 시: 입력 비밀번호를 해시 → 저장된 해시와 비교

**장점**:
- DB 해킹 당해도 비밀번호 복구 불가
- 관리자도 사용자 비밀번호 볼 수 없음

**참고 코드**: `UserService:54-56` (encode), `CustomUserDetailsService:26-30` (matches)
</details>

---

### Q17. CSRF 토큰이 없으면 어떤 공격이 가능한가?

<details>
<summary>💡 정답 보기</summary>

**공격 시나리오**:

1. 사용자가 은행 사이트에 로그인 (쿠키에 세션 ID 저장)

2. 악성 사이트 방문 → 숨겨진 Form 자동 제출
   ```html
   <!-- 악성 사이트: evil.com -->
   <form action="https://bank.com/transfer" method="POST">
     <input type="hidden" name="to" value="hacker_account">
     <input type="hidden" name="amount" value="1000000">
   </form>
   <script>
     document.forms[0].submit();  // 자동 제출!
   </script>
   ```

3. 브라우저가 자동으로 은행 사이트 쿠키 포함하여 요청 전송

4. 은행 서버: "유효한 세션이네? 송금 승인!" (사용자 의도 아님!)

**CSRF 토큰으로 방어**:
```html
<!-- 은행 사이트 -->
<form action="/transfer" method="POST">
  <input type="hidden" name="_csrf" value="랜덤토큰123">
  <input name="to">
  <input name="amount">
  <button>송금</button>
</form>
```

→ 악성 사이트는 토큰을 모름 → 요청 거부!

**참고 코드**: `SecurityConfig:38-39` (현재 비활성화됨, 프로덕션에서 활성화 필요)
</details>

---

### Q18. SQL Injection 공격은 JPA를 사용하면 완전히 방어되는가?

<details>
<summary>💡 정답 보기</summary>

**답**: **거의 방어되지만 Native Query는 주의!**

**안전한 경우 (Prepared Statement 자동 사용)**:
```java
// ⭕ Spring Data JPA
Optional<User> findByEmail(String email);

// ⭕ @Query with parameter
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// 입력: "admin@example.com"
// 실행: SELECT * FROM users WHERE email = ?
// → 파라미터는 자동 이스케이프 처리
```

**위험한 경우 (문자열 연결)**:
```java
// ❌ Native Query + 문자열 연결
@Query(value = "SELECT * FROM users WHERE email = '" + email + "'", nativeQuery = true)
List<User> findByEmail(String email);

// 입력: "admin' OR '1'='1"
// 실행: SELECT * FROM users WHERE email = 'admin' OR '1'='1'
// → 모든 사용자 정보 유출!
```

**올바른 Native Query**:
```java
// ⭕ Native Query + 파라미터
@Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
List<User> findByEmail(@Param("email") String email);
```

**이 프로젝트의 방어**:
- 모든 쿼리는 Spring Data JPA 또는 `@Query` + 파라미터 사용
- 직접 SQL 문자열 연결 금지

**참고**: 프로젝트 전체 Repository 확인 → 모두 안전한 패턴 사용
</details>

---

## 📌 섹션 7: 데이터베이스 설계

### Q19. Soft Delete를 사용하면 DB 용량이 계속 증가하는데 괜찮은가?

<details>
<summary>💡 정답 보기</summary>

**문제**: 맞습니다! Soft Delete는 물리적 삭제가 아니므로 데이터 누적

**해결책**: 30일 후 하드 삭제 (GDPR 규정 준수)

```java
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
@Transactional
public void hardDeleteOldRecords() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(30);

    // 30일 경과 + 소프트 삭제된 데이터 조회
    List<ContentReview> oldContents = contentRepository
        .findByIsDeletedTrueAndDeletedAtBefore(threshold);

    for (ContentReview content : oldContents) {
        // 1. 감사 로그 기록 (하드 삭제 사실 남김)
        auditLogRepository.save(AuditDeleteLog.builder()
            .action(DeleteAction.HARD_DELETE)
            .targetId(content.getId())
            .reason("30일 경과 자동 삭제")
            .build());

        // 2. 물리적 삭제
        contentRepository.delete(content);
    }
}
```

**왜 30일?**
- GDPR 규정: 사용자 요청 시 30일 내 삭제 의무
- 법적 분쟁 대비: 일정 기간 데이터 보존 필요
- 실수 복구 기간: 관리자 실수로 삭제 시 복구 가능

**참고**: 감사 로그는 영구 보존 (삭제 금지)
</details>

---

### Q20. LAZY 로딩과 EAGER 로딩 중 무엇을 기본으로 사용해야 하는가?

<details>
<summary>💡 정답 보기</summary>

**정답**: **LAZY 로딩 (99%의 경우)**

**이유**:

**EAGER의 문제 (N+1 Problem)**:
```java
// @ManyToOne(fetch = FetchType.EAGER)
List<StockComment> comments = commentRepository.findAll();

// 실행되는 쿼리:
// 1. SELECT * FROM stock_comment
// 2. SELECT * FROM stock_board WHERE id = 1
// 3. SELECT * FROM stock_board WHERE id = 2
// ...
// 101개 쿼리 실행! (댓글 100개 + 게시글 100개)
```

**LAZY + Fetch Join**:
```java
// @ManyToOne(fetch = FetchType.LAZY)
@Query("SELECT c FROM StockComment c JOIN FETCH c.board")
List<StockComment> findAllWithBoard();

// 실행되는 쿼리:
// 1. SELECT c.*, b.* FROM stock_comment c
//    INNER JOIN stock_board b ON c.board_id = b.id
// → 1개 쿼리로 모두 조회!
```

**EAGER가 필요한 경우**:
- 연관 엔티티를 100% 사용하는 경우
- 예: 주문 → 주문자 정보 (항상 필요)

**기본 원칙**:
1. 일단 모두 LAZY로 설정
2. N+1 문제 발생 시 → Fetch Join으로 해결
3. EAGER는 최후의 수단

**참고 코드**: 프로젝트 내 모든 `@ManyToOne`은 LAZY 사용
</details>

---

## 📌 섹션 8: 성능 최적화

### Q21. 캐시 테이블을 사용하는 대신 Redis를 사용하면 안 되는가?

<details>
<summary>💡 정답 보기</summary>

**비교**:

**캐시 테이블 (현재 방식)**:
- 장점: 추가 인프라 불필요, DB 백업에 포함
- 단점: DB 부하 증가, TTL 관리 직접 구현 필요

**Redis**:
- 장점: 초고속 (메모리 기반), TTL 자동 관리
- 단점: 추가 인프라 비용, 서버 재시작 시 데이터 유실

**언제 Redis?**
1. 초당 수천 건 이상의 캐시 조회
2. 매우 짧은 TTL (1분 이하)
3. 여러 서버에서 공유 캐시 필요

**이 프로젝트에서 캐시 테이블 선택 이유**:
1. 트래픽이 그렇게 높지 않음 (개인 프로젝트)
2. 15분 TTL → DB 조회도 충분히 빠름
3. 인프라 간소화 (비용 절감)

**나중에 Redis 도입 시**:
```java
// 캐시 테이블 코드
StockQuoteCache cache = cacheRepository.findByTicker("AAPL");

// Redis 코드
@Cacheable(value = "stockQuote", key = "#ticker")
public StockQuote getQuote(String ticker) {
    return yahooFinanceApi.getQuote(ticker);
}
// → 코드 변경 최소화!
```

**참고 코드**: `StockQuoteCache.java`, `YahooFinanceStockService`
</details>

---

### Q22. ThreadLocal 사용 후 remove()를 호출하지 않으면 어떻게 되는가?

<details>
<summary>💡 정답 보기</summary>

**문제**: 메모리 누수 (Memory Leak)

**원리**:
```java
ThreadLocal<Tesseract> tesseractThreadLocal = ThreadLocal.withInitial(() -> new Tesseract());

// 요청 1: Thread-1 사용
Tesseract tess1 = tesseractThreadLocal.get();  // 새 인스턴스 생성

// 요청 1 종료 → Thread-1은 Thread Pool로 반환

// 요청 2: Thread-1 재사용
Tesseract tess2 = tesseractThreadLocal.get();  // 기존 인스턴스 재사용

// → Thread-1에 연결된 Tesseract는 계속 메모리에 남아있음!
```

**해결책**:
```java
try {
    Tesseract tess = tesseractThreadLocal.get();
    return tess.doOCR(image);
} finally {
    tesseractThreadLocal.remove();  // 반드시 제거!
}
```

**이 프로젝트에서는?**
- Tesseract 인스턴스는 재사용해도 문제 없음 (상태가 없음)
- 따라서 remove() 생략 가능
- **단, 사용자 정보 같은 민감 데이터는 반드시 remove() 필요!**

**참고 코드**: `OcrService:84-92` (ThreadLocal 생성), `OcrService:219` (사용)
</details>

---

## 📌 섹션 9: 실전 배포

### Q23. 환경변수를 .env 파일로 관리하는 이유는?

<details>
<summary>💡 정답 보기</summary>

**문제**:
```properties
# application.properties (Git 커밋됨!)
openai.api.key=sk-1234567890abcdef
gemini.api.key=AIzaSyABC123
db.password=admin1234

→ GitHub에 업로드 → API 키 유출 → 과금 폭탄!
```

**해결책**:
```bash
# .env (Git 커밋 금지!)
OPENAI_API_KEY=sk-1234567890abcdef
GEMINI_API_KEY=AIzaSyABC123
DB_PASSWORD=admin1234
```

```properties
# application.properties (Git 커밋됨)
openai.api.key=${OPENAI_API_KEY}
gemini.api.key=${GEMINI_API_KEY}
db.password=${DB_PASSWORD}
```

**.gitignore 설정**:
```
# .gitignore
.env
.env.local
application-local.properties
```

**추가 보안**:
- AWS Secrets Manager 사용
- Kubernetes Secrets 사용
- 환경변수는 서버에서만 설정

**참고**: 이 프로젝트는 현재 application.properties에 직접 저장 → 배포 전 반드시 분리 필요!
</details>

---

### Q24. HTTPS 적용 없이 배포하면 어떤 보안 문제가 발생하는가?

<details>
<summary>💡 정답 보기</summary>

**문제**:

**HTTP (암호화 없음)**:
```
사용자 → [평문 전송] → 서버
    ↓
와이파이 공유기 (패킷 스니핑 가능)
    ↓
로그인 정보: email=user@example.com&password=myPassword123
→ 해커가 그대로 볼 수 있음!
```

**HTTPS (암호화)**:
```
사용자 → [TLS 암호화] → 서버
    ↓
와이파이 공유기
    ↓
암호화된 데이터: 6f3a8b2c9d1e... (해독 불가)
```

**추가 문제**:
1. **중간자 공격 (MITM)**: 해커가 가짜 서버로 유도
2. **세션 하이재킹**: 쿠키 탈취 → 계정 탈취
3. **SEO 불이익**: Google은 HTTPS 사이트 우선 노출

**HTTPS 적용 방법**:
1. Let's Encrypt (무료 SSL 인증서)
2. Nginx Reverse Proxy
3. AWS ELB/ALB (자동 SSL)

**참고**: 이 프로젝트는 로컬 개발용 → 배포 시 HTTPS 필수!
</details>

---

### Q25. 프로덕션 배포 전 반드시 확인해야 할 체크리스트는?

<details>
<summary>💡 정답 보기</summary>

**보안**:
- [ ] Spring Security 활성화 (`anyRequest().permitAll()` 제거)
- [ ] CSRF 활성화 (REST API는 JWT로 대체)
- [ ] 환경변수 분리 (.env 파일 또는 Secrets Manager)
- [ ] HTTPS 적용 (Let's Encrypt)
- [ ] 비밀번호 정책 강화 (최소 8자, 특수문자 포함)

**성능**:
- [ ] DB 인덱스 확인 (`EXPLAIN ANALYZE`)
- [ ] 캐시 TTL 조정 (트래픽에 따라)
- [ ] 이미지 압축 (포트폴리오 스크린샷)
- [ ] Gzip 압축 활성화

**모니터링**:
- [ ] 로그 레벨 조정 (`logging.level.root=WARN`)
- [ ] APM 도구 연동 (Sentry, New Relic)
- [ ] 에러 알림 설정 (Slack, Email)
- [ ] 헬스체크 엔드포인트 추가 (`/actuator/health`)

**데이터**:
- [ ] 하드 삭제 스케줄러 활성화 (30일 후)
- [ ] DB 백업 자동화 (일일 백업)
- [ ] API 키 로테이션 정책 수립

**테스트**:
- [ ] 부하 테스트 (JMeter, K6)
- [ ] 보안 스캔 (OWASP ZAP)
- [ ] 브라우저 호환성 테스트

**참고**: `SecurityConfig.java` 주석 참고, 배포 가이드 별도 작성 권장
</details>

---

## 🎯 최종 점검 문제

### Q26. 이 프로젝트를 한 문장으로 설명한다면?

<details>
<summary>💡 정답 보기</summary>

**"증권사 스크린샷 한 장으로 워렌 버핏과 비교하고, AI가 맞춤 투자 전략을 제안하는 금융 플랫폼"**

**핵심 키워드**:
- OCR (Tesseract)
- SEC 13F 데이터
- 코사인 유사도
- AI 추천 (OpenAI/Gemini)
- Spring Boot + JPA + Security
</details>

---

### Q27. 이 프로젝트에서 가장 기술적으로 어려웠던 부분은?

<details>
<summary>💡 정답 보기</summary>

**예상 답변**:

1. **OCR 전처리**: 증권사마다 다른 레이아웃 대응
2. **SEC 13F 수집**: Rate Limit, Checkpoint, 재시도 로직
3. **코사인 유사도 알고리즘**: 레버리지 ETF 환산
4. **AI 프롬프트 엔지니어링**: 일관된 JSON 응답 유도
5. **Soft Delete + 감사 로그**: 트랜잭션 관리

**면접 팁**:
- 문제 상황 → 시도한 방법 → 최종 해결책 순서로 설명
- 수치로 증명 (API 호출 100회 → 1회 감소)
- 트레이드오프 언급 (캐시 테이블 vs Redis)
</details>

---

### Q28. 이 프로젝트를 확장한다면 어떤 기능을 추가하고 싶은가?

<details>
<summary>💡 정답 보기</summary>

**실현 가능한 확장**:

1. **실시간 알림**: 투자대가 포트폴리오 변경 시 알림 (WebSocket)
2. **백테스팅**: AI 추천 포트폴리오의 과거 성과 시뮬레이션
3. **소셜 기능**: 사용자 간 포트폴리오 공유, 팔로우
4. **모바일 앱**: React Native로 포트폴리오 스캔
5. **다국어 지원**: 일본어, 중국어 뉴스 번역

**기술적 챌린지**:

1. **실시간 데이터**: WebSocket + Redis Pub/Sub
2. **백테스팅**: Pandas (Python) + FastAPI 연동
3. **모바일 OCR**: TensorFlow Lite On-Device
4. **다국어**: i18n + 다국어 AI 모델

**참고**: 확장성을 고려한 현재 설계 (도메인 분리, 전략 패턴)
</details>

---

**축하합니다! 🎉**

모든 문제를 풀었다면 프로젝트를 깊이 이해한 것입니다.
이제 발표 준비 또는 포트폴리오 작성에 활용하세요!