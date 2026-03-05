# Common Services

애플리케이션 전반에서 공통으로 사용되는 서비스를 제공하는 패키지입니다.

## 📁 파일 목록

### GPTService.java
OpenAI GPT API를 통합하여 AI 기능을 제공하는 핵심 서비스입니다.

#### 주요 메서드

##### 1. analyzeStock(String symbol, String companyName)
주식 분석을 수행합니다.
```java
String analysis = gptService.analyzeStock("AAPL", "Apple Inc.");
// 반환: GPT가 생성한 주식 분석 리포트
```

##### 2. analyzeInvestor(String investorId)
투자자의 투자 철학을 분석합니다.
```java
String philosophy = gptService.analyzeInvestor("buffett");
// 반환: 워렌 버핏의 투자 철학 분석
```

##### 3. searchInvestorInfo(String investorName)
투자자 정보를 검색합니다.
```java
String info = gptService.searchInvestorInfo("Charlie Munger");
// 반환: 투자자 정보 및 투자 스타일
```

##### 4. generatePortfolioRecommendation(List<String> investorIds) ⭐ NEW
4명의 투자자 철학을 각 25%씩 반영한 포트폴리오를 생성합니다.
```java
List<String> investors = Arrays.asList("buffett", "lynch", "wood", "soros");
String portfolio = gptService.generatePortfolioRecommendation(investors);
// 반환: 통합 투자 철학, 추천 종목, 리스크 프로필 등
```

##### 5. analyzeNews(String newsContent)
뉴스 내용을 분석하여 인사이트를 제공합니다.
```java
String analysis = gptService.analyzeNews("Apple announces Q4 earnings...");
// 반환: 뉴스 분석 및 투자 인사이트
```

#### 주요 기능

1. **주식 분석**
   - 기업 정보 분석
   - 재무 지표 해석
   - 투자 의견 제공

2. **투자자 철학 분석**
   - 유명 투자자의 투자 스타일 분석
   - 투자 철학 카테고리화
   - 실전 적용 방법 제시

3. **포트폴리오 추천**
   - 복수 투자자의 철학 통합
   - 맞춤형 종목 추천
   - 리스크 프로필 분석

4. **뉴스 분석**
   - 금융 뉴스 요약
   - 시장 영향 분석
   - 투자 기회 도출

#### 설정

`application.properties`에 OpenAI API 키를 설정해야 합니다:

```properties
openai.api.key=your-openai-api-key
```

#### 기술 스택
- **OpenAI Java Client**: `com.theokanning.openai-gpt3-java`
- **모델**: GPT-4 (고급 분석), GPT-3.5-turbo (일반 분석)
- **타임아웃**: 90초 (복잡한 분석을 위한 충분한 시간)

#### 프롬프트 엔지니어링

각 메서드는 최적화된 프롬프트를 사용합니다:

1. **System Message**: AI의 역할 정의
   ```
   "당신은 세계적인 투자 전문가입니다..."
   ```

2. **User Message**: 구체적인 요청 및 형식 지정
   ```
   "다음 4명의 투자자 철학을 각각 25%씩 반영한 추천 포트폴리오를 만들어주세요..."
   ```

3. **Temperature**: 창의성 조절
   - 분석 작업: 0.7 (균형잡힌 응답)
   - 추천 작업: 0.8 (창의적 응답)

#### 에러 처리
- API 호출 실패 시 로그 기록
- 타임아웃 처리
- 사용자에게 친화적인 에러 메시지 반환

## 📌 사용 예시

### Spring Bean 주입
```java
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final GPTService gptService;

    public String analyzePortfolio(String symbol) {
        return gptService.analyzeStock(symbol, "Company Name");
    }
}
```

### 직접 사용
```java
@Autowired
private GPTService gptService;

public void analyze() {
    // 주식 분석
    String stockAnalysis = gptService.analyzeStock("TSLA", "Tesla");

    // 투자자 분석
    String investorAnalysis = gptService.analyzeInvestor("wood");

    // 포트폴리오 추천
    List<String> investors = Arrays.asList("buffett", "lynch", "wood", "soros");
    String portfolio = gptService.generatePortfolioRecommendation(investors);
}
```

## 🔗 사용하는 패키지
- `controller/portfolio/` - 포트폴리오 분석
- `controller/insights/` - 뉴스 분석
- `controller/stock/` - 주식 분석
- `service/portfolio/` - 투자자 비교 서비스
- `service/insights/` - 뉴스 스크래핑 서비스

## ⚠️ 주의사항
1. **API 키 보안**: API 키를 코드에 직접 노출하지 말고 환경 변수나 설정 파일 사용
2. **비용 관리**: GPT-4는 비용이 높으므로 필요한 경우에만 사용
3. **Rate Limiting**: OpenAI API의 요청 제한 고려
4. **캐싱**: 동일한 요청에 대한 응답 캐싱 고려

## 🚀 향후 개선사항
- [ ] 응답 캐싱 구현
- [ ] 다양한 GPT 모델 선택 옵션
- [ ] 스트리밍 응답 지원
- [ ] 토큰 사용량 모니터링
