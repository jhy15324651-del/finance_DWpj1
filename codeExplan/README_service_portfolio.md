# Portfolio Services

포트폴리오 비교 및 투자자 분석 비즈니스 로직을 담당하는 서비스 패키지입니다.

## 📁 파일 목록

### InvestorComparisonService.java
유명 투자자들의 투자 철학을 비교하고 AI 기반 포트폴리오를 추천하는 서비스입니다.

#### 주요 메서드

##### 1. compareInvestors(List<String> investorIds)
선택한 투자자들의 투자 철학을 비교 분석합니다.

```java
List<String> investors = Arrays.asList("buffett", "lynch", "wood", "soros");
List<InvestorComparisonDTO> comparison =
    investorComparisonService.compareInvestors(investors);

// 각 투자자의 철학 분석 결과
comparison.forEach(investor -> {
    System.out.println("투자자: " + investor.getInvestorId());

    // 투자 철학 카테고리별 비중
    investor.getPhilosophy().forEach(item -> {
        System.out.println(item.getCategory() + ": " + item.getPercentage() + "%");
    });

    // GPT 인사이트
    System.out.println("인사이트: " + investor.getInsights());
});
```

**반환 데이터 구조**:
- 투자자별로 다음 카테고리의 비중 제공:
  - 가치투자 (Value Investing)
  - 성장투자 (Growth Investing)
  - 기술주 투자 (Tech Investing)
  - ESG 투자 (ESG Investing)
  - 매크로 투자 (Macro Investing)
  - 퀀트 투자 (Quantitative)
- GPT가 생성한 각 투자자의 핵심 인사이트

---

##### 2. searchInvestor(String investorName)
GPT API를 사용하여 새로운 투자자 정보를 검색합니다.

```java
InvestorSearchResponse response =
    investorComparisonService.searchInvestor("Charlie Munger");

System.out.println("이름: " + response.getName());
System.out.println("스타일: " + response.getStyle());
System.out.println("설명: " + response.getDescription());
```

**특징**:
- GPT-4를 활용한 실시간 정보 검색
- 투자자의 투자 스타일 자동 분류
- 투자 철학 요약 생성

---

##### 3. generatePortfolioRecommendation(List<String> investorIds) ⭐ NEW
4명의 투자자 철학을 각 25%씩 반영한 AI 추천 포트폴리오를 생성합니다.

```java
List<String> investors = Arrays.asList("buffett", "lynch", "wood", "soros");
PortfolioRecommendationResponse portfolio =
    investorComparisonService.generatePortfolioRecommendation(investors);

System.out.println("선택된 투자자: " + portfolio.getSelectedInvestors());
System.out.println("통합 철학: " + portfolio.getCombinedPhilosophy());
System.out.println("추천 포트폴리오: " + portfolio.getRationale());
System.out.println("리스크 프로필: " + portfolio.getRiskProfile());

// 추천 종목 (현재는 GPT 텍스트 응답으로 제공)
System.out.println("추천 내용: " + portfolio.getRationale());
```

**GPT 프롬프트 구조**:
```
1. 통합 투자 철학:
   [4명의 투자자 철학을 25%씩 혼합한 전체적인 투자 접근법]

2. 추천 종목 (5-7개):
   각 종목별로:
   - 종목명 (티커)
   - 섹터
   - 비중 (%)
   - 선정 이유 (어떤 투자자의 철학이 반영되었는지 포함)

3. 포트폴리오 설명:
   [전체 포트폴리오의 특징과 기대 효과]

4. 리스크 프로필:
   [이 포트폴리오의 리스크 수준과 주의사항]
```

---

#### 지원하는 투자자 목록

서비스에 미리 정의된 9명의 유명 투자자:

| ID | 이름 | 투자 스타일 |
|---|---|---|
| `buffett` | 워렌 버핏 (Warren Buffett) | 가치투자 |
| `lynch` | 피터 린치 (Peter Lynch) | 성장주 투자 |
| `wood` | 캐시 우드 (Cathie Wood) | 파괴적 혁신 투자 |
| `soros` | 조지 소로스 (George Soros) | 매크로 투자 |
| `thiel` | 피터 틸 (Peter Thiel) | 벤처 캐피털 |
| `fink` | 래리 핑크 (Larry Fink) | ESG 투자 |
| `dalio` | 레이 달리오 (Ray Dalio) | 리스크 패리티 |
| `graham` | 벤저민 그레이엄 (Benjamin Graham) | 가치투자 원조 |
| `simons` | 짐 사이먼스 (Jim Simons) | 퀀트 투자 |

추가로 `searchInvestor()` 메서드를 통해 커스텀 투자자 추가 가능

---

#### 비즈니스 로직

##### 1. 투자자 철학 분석 로직
```java
// 각 투자자에 대해 GPT API 호출
for (String investorId : investorIds) {
    String analysis = gptService.analyzeInvestor(investorId);

    // GPT 응답 파싱
    InvestorComparisonDTO dto = parseAnalysis(analysis);

    // 철학 카테고리화 (가치투자, 성장투자 등)
    List<PhilosophyItem> philosophy = categorizePhilosophy(analysis);

    results.add(dto);
}
```

##### 2. 포트폴리오 추천 로직
```java
// 4명의 투자자 ID를 GPT에 전달
String prompt = buildPortfolioPrompt(investorIds);

// GPT-4 호출하여 포트폴리오 생성
String portfolio = gptService.generatePortfolioRecommendation(investorIds);

// 응답 포맷팅
PortfolioRecommendationResponse response = formatResponse(portfolio);
```

## 📌 사용 예시

### 1. 완전한 투자자 비교 플로우
```java
@Service
@RequiredArgsConstructor
public class PortfolioController {
    private final InvestorComparisonService comparisonService;

    public void compareAndRecommend() {
        // 1. 투자자 선택
        List<String> investors = Arrays.asList("buffett", "lynch", "wood", "soros");

        // 2. 투자자 철학 비교
        List<InvestorComparisonDTO> comparison =
            comparisonService.compareInvestors(investors);

        // 3. 각 투자자 분석 결과 확인
        comparison.forEach(this::displayInvestorAnalysis);

        // 4. AI 포트폴리오 추천 생성
        PortfolioRecommendationResponse portfolio =
            comparisonService.generatePortfolioRecommendation(investors);

        // 5. 추천 포트폴리오 표시
        displayPortfolio(portfolio);
    }

    private void displayInvestorAnalysis(InvestorComparisonDTO investor) {
        System.out.println("\n=== " + investor.getInvestorId() + " ===");
        investor.getPhilosophy().forEach(p ->
            System.out.println(p.getCategory() + ": " + p.getPercentage() + "%")
        );
        System.out.println("인사이트: " + investor.getInsights());
    }

    private void displayPortfolio(PortfolioRecommendationResponse portfolio) {
        System.out.println("\n=== AI 추천 포트폴리오 ===");
        System.out.println(portfolio.getRationale());
    }
}
```

### 2. 커스텀 투자자 추가
```java
// 새로운 투자자 검색
InvestorSearchResponse charlie =
    comparisonService.searchInvestor("Charlie Munger");

// 기존 투자자와 비교
List<String> investors = Arrays.asList("buffett", "lynch", "wood");
// Charlie Munger는 검색으로 추가된 커스텀 투자자

List<InvestorComparisonDTO> comparison =
    comparisonService.compareInvestors(investors);
```

### 3. Spring Bean 주입
```java
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioApiController {
    private final InvestorComparisonService investorComparisonService;

    @PostMapping("/compare")
    public List<InvestorComparisonDTO> compare(
            @RequestBody InvestorComparisonRequest request) {
        return investorComparisonService.compareInvestors(
            request.getInvestors()
        );
    }

    @PostMapping("/recommend")
    public PortfolioRecommendationResponse recommend(
            @RequestBody InvestorComparisonRequest request) {
        return investorComparisonService.generatePortfolioRecommendation(
            request.getInvestors()
        );
    }
}
```

## 🔗 연관 패키지
- `controller/portfolio/PortfolioApiController.java` - REST API
- `dto/portfolio/` - 요청/응답 DTO
- `service/common/GPTService.java` - GPT API 통합

## 🚀 주요 기능

### 1. 투자자 철학 비교
- 최대 4명까지 동시 비교
- 6가지 카테고리로 철학 분류
- GPT 기반 심층 인사이트

### 2. AI 포트폴리오 추천
- 4명의 철학을 25%씩 균등 반영
- 5-7개 종목 추천
- 섹터별 분산 투자
- 리스크 프로필 제공

### 3. 커스텀 투자자 검색
- 실시간 GPT 검색
- 자동 스타일 분류
- 비교 분석에 즉시 추가

## ⚙️ 설정 요구사항

### application.properties
```properties
# OpenAI API Key (필수)
openai.api.key=your-openai-api-key
```

## 💡 알고리즘 상세

### 투자 철학 카테고리화
각 투자자의 철학을 다음 카테고리로 분류:

1. **가치투자** (Value Investing)
   - 내재 가치 대비 저평가 종목 선호
   - 대표: 워렌 버핏, 벤저민 그레이엄

2. **성장투자** (Growth Investing)
   - 높은 성장 잠재력 기업 투자
   - 대표: 피터 린치

3. **기술주 투자** (Tech Investing)
   - 혁신 기술 및 파괴적 기업
   - 대표: 캐시 우드, 피터 틸

4. **ESG 투자** (ESG Investing)
   - 지속가능성과 장기 가치
   - 대표: 래리 핑크

5. **매크로 투자** (Macro Investing)
   - 거시경제 동향 활용
   - 대표: 조지 소로스, 레이 달리오

6. **퀀트 투자** (Quantitative)
   - 수학과 통계 기반
   - 대표: 짐 사이먼스

### 포트폴리오 생성 알고리즘
```
1. 4명의 투자자 선택
2. 각 투자자의 철학 분석
3. 철학별 가중치 계산 (25% * 4)
4. GPT-4에 통합 프롬프트 전송
5. 종목 추천 생성
6. 섹터 분산 확인
7. 리스크 프로필 평가
8. 최종 포트폴리오 반환
```

## ⚠️ 제약사항
1. **투자자 수**: 정확히 4명 선택 필요 (포트폴리오 추천 시)
2. **API 비용**: GPT-4 사용으로 비용 발생
3. **응답 시간**: GPT 호출로 인해 10-30초 소요
4. **정확성**: AI 추천은 참고용이며 투자 판단은 사용자 책임

## 🔄 데이터 플로우
```
Frontend → PortfolioApiController → InvestorComparisonService
                                           ↓
                                      GPTService
                                           ↓
                                       OpenAI API
                                           ↓
                                      분석 결과 파싱
                                           ↓
                                         DTO 생성
                                           ↓
                                      Frontend 반환
```

## 🎯 향후 개선사항
- [ ] 실시간 주가 데이터 통합
- [ ] 백테스팅 기능 추가
- [ ] 포트폴리오 리밸런싱 알고리즘
- [ ] 사용자 맞춤형 리스크 레벨 설정
- [ ] 과거 추천 포트폴리오 성과 추적
