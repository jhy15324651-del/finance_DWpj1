# AI Provider 마이그레이션 가이드

OpenAI → Gemini API 마이그레이션 완료 (Provider 스위칭 지원)

---

## 📋 변경 사항 요약

### 1. 새로 추가된 파일

#### AI Client 인프라
- `src/main/java/org/zerock/finance_dwpj1/ai/client/AiClient.java` - AI 클라이언트 인터페이스
- `src/main/java/org/zerock/finance_dwpj1/ai/client/OpenAiClient.java` - OpenAI 구현체
- `src/main/java/org/zerock/finance_dwpj1/ai/client/GeminiAiClient.java` - Gemini 구현체

#### DTO
- `src/main/java/org/zerock/finance_dwpj1/ai/dto/AiRequest.java` - AI 요청 DTO
- `src/main/java/org/zerock/finance_dwpj1/ai/dto/AiResult.java` - AI 응답 DTO

#### 설정 및 예외
- `src/main/java/org/zerock/finance_dwpj1/ai/config/AiClientConfig.java` - Provider 스위칭 빈 구성
- `src/main/java/org/zerock/finance_dwpj1/ai/exception/AiClientException.java` - 공통 예외 클래스

### 2. 수정된 파일
- `src/main/java/org/zerock/finance_dwpj1/service/common/GPTService.java` - AiClient 기반으로 리팩토링
- `src/main/resources/application.properties` - AI Provider 설정 추가
- `build.gradle` - WebFlux 의존성 추가 (Gemini용)

---

## 🔧 Provider 스위칭 방법

### application.properties에서 설정 변경

```properties
# Gemini 사용 (기본값)
ai.provider=gemini
gemini.api.key=your-gemini-api-key-here

# 또는 OpenAI 사용
ai.provider=openai
openai.api.key=your-openai-api-key-here
```

**중요**: `ai.provider` 값만 변경하면 자동으로 스위칭됩니다.

---

## 🚀 로컬 검증 방법

### 1. Gemini로 실행

```properties
ai.provider=gemini
gemini.api.key=AIza...
```

서버 시작 시 로그 확인:
```
========================================
AI Provider: Google Gemini
========================================
```

### 2. OpenAI로 실행

```properties
ai.provider=openai
openai.api.key=sk-...
```

서버 시작 시 로그 확인:
```
========================================
AI Provider: OpenAI
========================================
```

### 3. 기능 테스트

다음 기능들이 정상 동작하는지 확인:

#### ✅ 뉴스 번역 및 요약
- 경로: `/admin/news` 또는 자동 스케줄러
- 확인 사항: 영어 뉴스가 한국어로 번역 + 요약 생성

#### ✅ 투자자 분석
- 경로: `/portfolio/investors`
- 확인 사항: 투자자 철학 분석 및 인사이트 생성

#### ✅ 포트폴리오 추천
- 경로: `/portfolio/recommendation`
- 확인 사항: 4명 투자자 철학 혼합 포트폴리오 생성

---

## 📊 기능별 JSON 계약 여부

| 기능 | 메서드 | JSON 강제 | 재시도 로직 | 구현 위치 |
|------|--------|-----------|------------|----------|
| 뉴스 번역+요약 | `translateAndSummarizeNews` | ❌ 정형화된 텍스트 | ❌ | GPTService:25 |
| 트윗 번역 | `translateTweet` | ❌ | ❌ | GPTService:50 |
| 투자자 철학 분석 | `analyzeInvestorPhilosophy` | ❌ | ❌ | GPTService:68 |
| 투자자 정보 검색 | `searchInvestorInfo` | ❌ | ❌ | GPTService:88 |
| 포트폴리오 추천 | `generatePortfolioRecommendation` | ❌ 정형화된 텍스트 | ❌ | GPTService:109 |
| 뉴스 요약 | `summarizeNews` | ❌ | ❌ | GPTService:156 |
| 뉴스 번역 | `translateNewsToKorean` | ❌ | ❌ | GPTService:187 |
| 범용 응답 | `generateResponse` | ❌ | ❌ | GPTService:218 |

**참고**: 현재 프로젝트는 모든 기능이 텍스트 생성만 사용하며, JSON 강제 응답을 사용하지 않습니다.

---

## 🏗️ 아키텍처

```
┌─────────────────────────┐
│   GPTService            │ ← 기존 서비스 (호환성 유지)
│   (기존 API 유지)       │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│   AiClient              │ ← 인터페이스
│   (추상화 레이어)       │
└───────────┬─────────────┘
            │
      ┌─────┴─────┐
      ▼           ▼
┌──────────┐  ┌──────────┐
│ OpenAI   │  │ Gemini   │ ← 구현체 (Provider 선택)
│ Client   │  │ Client   │
└──────────┘  └──────────┘
```

---

## 🔒 보안 강화 사항

1. **API 키 로그 출력 금지**: 모든 클라이언트에서 API 키를 로그에 출력하지 않음
2. **공통 예외 처리**: `AiClientException`으로 모든 에러 래핑
3. **타임아웃 설정**: 모든 요청에 타임아웃 적용 (기본 60초)
4. **에러 분류**: 401, 429, 타임아웃 등 에러 타입별 분류

---

## 📝 의존성 추가 사항

### build.gradle

```gradle
// AI Provider Dependencies
implementation 'com.theokanning.openai-gpt3-java:service:0.18.2'  // OpenAI
implementation 'org.springframework.boot:spring-boot-starter-webflux'  // Gemini (WebClient)
implementation 'com.fasterxml.jackson.core:jackson-databind'  // JSON parsing
```

---

## ⚠️ 주의사항

1. **API 키 필수**: 사용하려는 Provider의 API 키를 반드시 설정해야 합니다.
2. **모델 선택**:
   - OpenAI: 포트폴리오 추천은 `gpt-4`, 나머지는 `gpt-3.5-turbo`
   - Gemini: 기본 `gemini-1.5-flash`, 프리미엄 `gemini-1.5-pro`
3. **기존 호환성**: GPTService의 메서드 시그니처는 변경되지 않아 기존 코드 수정 불필요

---

## 🧪 테스트 체크리스트

- [ ] Gemini로 뉴스 번역 테스트
- [ ] Gemini로 투자자 분석 테스트
- [ ] Gemini로 포트폴리오 추천 테스트
- [ ] OpenAI로 뉴스 번역 테스트
- [ ] OpenAI로 투자자 분석 테스트
- [ ] OpenAI로 포트폴리오 추천 테스트
- [ ] Provider 스위칭 후 재시작 확인

---

## 📞 문의

구현 관련 문의사항은 프로젝트 담당자에게 연락하세요.