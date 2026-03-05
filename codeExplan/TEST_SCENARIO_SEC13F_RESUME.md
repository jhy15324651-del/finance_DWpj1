# SEC 13F Resume 기능 테스트 시나리오

## 📋 테스트 목표
1. 1~3번 투자자 성공 후 4번째에서 강제 실패
2. 재실행 시 1~3번은 건너뛰고 4번부터 재개되는지 확인
3. ApplicationContext 종료 시 수집 작업이 안전하게 중단되는지 확인

---

## 🧪 테스트 1: 정상 Resume 시나리오

### 1단계: 초기 실행 (1~3번 성공, 4번 실패)

```bash
# 1. 애플리케이션 시작
./gradlew bootRun

# 2. SEC 13F 수집 시작 (API 호출)
curl -X POST http://localhost:8080/api/admin/sec13f/collect

# 3. 로그 확인 (예상 출력)
# [1/10] 투자자: Warren Buffett (buffett) ✅ SUCCESS
# [2/10] 투자자: Bill Ackman (ackman) ✅ SUCCESS
# [3/10] 투자자: Ray Dalio (dalio) ✅ SUCCESS
# [4/10] 투자자: Carl Icahn (icahn) ❌ FAILED (SEC Rate Limit 또는 강제 실패)
```

### 2단계: Checkpoint 확인

```sql
-- sec_collector_checkpoint 테이블 확인
SELECT
    investor_id,
    filing_quarter,
    status,
    holdings_count,
    retry_count,
    fail_reason,
    completed_at
FROM sec_collector_checkpoint
ORDER BY id;

-- 예상 결과:
-- buffett  | 2025Q4 | SUCCESS  | 50  | 0 | NULL                       | 2025-01-10 14:23:10
-- ackman   | 2025Q4 | SUCCESS  | 30  | 0 | NULL                       | 2025-01-10 14:25:30
-- dalio    | 2025Q4 | SUCCESS  | 45  | 0 | NULL                       | 2025-01-10 14:27:50
-- icahn    | 2025Q4 | FAILED   | 0   | 1 | RuntimeException: SEC...   | 2025-01-10 14:30:10
```

### 3단계: 재실행 (Resume)

```bash
# 다시 수집 시작
curl -X POST http://localhost:8080/api/admin/sec13f/collect

# 예상 로그:
# [1/10] 투자자: Warren Buffett (buffett)
# ✓ 이미 성공 (2025Q4 at 2025-01-10T14:23:10) ⏭️ SKIPPED
#
# [2/10] 투자자: Bill Ackman (ackman)
# ✓ 이미 성공 (2025Q4 at 2025-01-10T14:25:30) ⏭️ SKIPPED
#
# [3/10] 투자자: Ray Dalio (dalio)
# ✓ 이미 성공 (2025Q4 at 2025-01-10T14:27:50) ⏭️ SKIPPED
#
# [4/10] 투자자: Carl Icahn (icahn)
# ⚠️ 재시도 1/3 - 2초 후 재시도 (원인: SEC Rate Limit)
# ⚠️ 재시도 2/3 - 4초 후 재시도 (원인: SEC Rate Limit)
# ✅ SUCCESS - 38건 저장
```

---

## 🧪 테스트 2: DevTools 재시작 감지

### 시나리오: 코드 수정 시 ApplicationContext 재시작

```bash
# 1. 수집 실행
curl -X POST http://localhost:8080/api/admin/sec13f/collect

# 2. 수집 중에 아무 Java 파일 수정 (예: SEC13FService.java 주석 추가)
#    → DevTools가 자동 재시작 트리거

# 3. 예상 로그:
# 🔴 ApplicationContext 닫힘 감지! 수집 작업 실행 중: true
# ⚠️ 강제 중단: 진행 중인 수집 작업 종료
# 🛑 13F 수집 중단 요청 플래그 ON
# ⏸️ 중단 요청 감지 - 현재까지 2/10명 처리 완료
#
# ... (재시작) ...
#
# ✅ ApplicationContext 준비 완료 - SEC 13F 수집 대기 상태
```

**검증 포인트:**
- ApplicationContext 종료 시 `onContextClosed()` 이벤트 리스너가 실행되는가?
- `stopRequested` 플래그가 `true`로 설정되는가?
- for loop가 중간에 `break`되는가?
- 앱이 crash 없이 재시작되는가?

---

## 🧪 테스트 3: Rate Limit 재시도

### 시나리오: SEC API 429 응답 시 Exponential Backoff

```java
// SEC13FService.java에서 강제로 429 에러 발생시키기 (테스트용)
private String getLatest13FFileUrl(String cik) {
    // 테스트: 특정 투자자에서 강제로 429 반환
    if ("icahn".equals(cik)) {
        throw new RuntimeException("SEC Rate Limit (429) - Retry-After: 5");
    }
    // ...
}
```

**예상 로그:**
```
[4/10] 투자자: Carl Icahn (icahn)
🚨 SEC Rate Limit! Retry-After: 5초
⚠️ 재시도 1/3 - 2초 후 재시도 (원인: SEC Rate Limit (429))
⚠️ 재시도 2/3 - 4초 후 재시도 (원인: SEC Rate Limit (429))
⚠️ 재시도 3/3 - 8초 후 재시도 (원인: SEC Rate Limit (429))
💀 최대 재시도 횟수 초과 (3/3)
❌ FAILED - Carl Icahn
```

**검증 포인트:**
- Retry 딜레이가 2초 → 4초 → 8초로 증가하는가?
- 최대 3회 재시도 후 FAILED 처리되는가?
- 다음 투자자는 정상 진행되는가?

---

## 🧪 테스트 4: 4번째 투자자 원인 분석

### 방법 1: 로그 레벨 DEBUG로 변경

```yaml
# application.yml
logging:
  level:
    org.zerock.finance_dwpj1.service.portfolio.SEC13FService: DEBUG
```

### 방법 2: 4번째 투자자의 CIK 직접 확인

```sql
SELECT investor_id, name, cik, organization
FROM investor_profiles
WHERE active = true
ORDER BY investor_id
LIMIT 4;

-- 4번째 투자자의 CIK를 복사
```

```bash
# SEC API 직접 호출 (브라우저 또는 curl)
curl -H "User-Agent: FinanceDWPJ1/1.0 (test@example.com)" \
     https://data.sec.gov/submissions/CIK0001234567890.json

# 응답 확인:
# - HTML 페이지인가? (차단/에러)
# - JSON인가? (정상)
# - 429 Too Many Requests인가? (Rate Limit)
# - filings.recent.form 배열에 "13F-HR"이 있는가?
```

### 방법 3: 특정 투자자만 테스트

```java
// Controller 또는 직접 호출
@GetMapping("/test/{investorId}")
public ResponseEntity<?> testSingleInvestor(@PathVariable String investorId) {
    try {
        int count = sec13FService.fetch13FDataForInvestor(investorId);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}
```

```bash
# 4번째 투자자만 테스트
curl http://localhost:8080/api/admin/sec13f/test/icahn
```

---

## 🔍 원인 확인 체크리스트

### ✅ 로그에서 확인할 항목

1. **4번째 투자자 정보**
   ```
   [4/10] 투자자: ??? (???)
   CIK: ???, 조직: ???
   ```

2. **SEC API 응답**
   ```
   📡 SEC API 응답 - Status: ???, Content-Type: ???
   ```
   - Status 200: 정상
   - Status 429: Rate Limit
   - Status 403: 차단
   - Content-Type이 text/html: HTML 응답 (에러)

3. **최초 예외 발생 지점**
   ```
   ❌ SEC API 호출 중 오류
   Caused by: ???
   ```

4. **ApplicationContext 재시작 여부**
   ```
   🔴 ApplicationContext 닫힘 감지!
   ```

### ✅ DB에서 확인할 항목

```sql
-- 1. 성공한 투자자 확인
SELECT COUNT(*) FROM sec_collector_checkpoint WHERE status = 'SUCCESS';
-- 예상: 3개

-- 2. 실패한 투자자 확인
SELECT investor_id, fail_reason FROM sec_collector_checkpoint WHERE status = 'FAILED';
-- 예상: icahn | RuntimeException: SEC Rate Limit (429)

-- 3. 실제 저장된 Holdings 확인
SELECT investor_id, COUNT(*) FROM investor_13f_holdings GROUP BY investor_id;
-- 예상: buffett(50), ackman(30), dalio(45)
```

---

## 🎯 성공 기준

- ✅ 1~3번 투자자는 SUCCESS, 4번은 FAILED로 Checkpoint 저장
- ✅ 재실행 시 1~3번은 "이미 성공" 메시지와 함께 SKIP
- ✅ 4번부터 재시도 시작
- ✅ DevTools 재시작 시 수집 작업 안전하게 중단
- ✅ 앱 전체가 crash 없이 계속 실행됨
- ✅ 로그에서 실패 원인 명확히 확인 가능