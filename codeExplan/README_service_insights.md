# Insights Services

뉴스 스크래핑 및 소셜 미디어 분석 비즈니스 로직을 담당하는 서비스 패키지입니다.

## 📁 파일 목록

### NewsScrapingService.java
금융 뉴스를 수집하고 분석하는 서비스입니다.

#### 주요 메서드

##### 1. getLatestNews()
최신 금융 뉴스를 조회합니다.
```java
List<NewsDTO> news = newsScrapingService.getLatestNews();
```

##### 2. getNewsBySymbol(String symbol)
특정 주식과 관련된 뉴스를 조회합니다.
```java
List<NewsDTO> appleNews = newsScrapingService.getNewsBySymbol("AAPL");
```

##### 3. searchNews(String keyword)
키워드로 뉴스를 검색합니다.
```java
List<NewsDTO> results = newsScrapingService.searchNews("Tesla earnings");
```

##### 4. analyzeNews(Long newsId)
특정 뉴스를 GPT로 분석합니다.
```java
NewsDTO analyzed = newsScrapingService.analyzeNews(123L);
// GPT 분석 결과가 포함된 뉴스 DTO 반환
```

##### 5. analyzeSentiment(String text)
뉴스의 감성을 분석합니다.
```java
SentimentResult sentiment = newsScrapingService.analyzeSentiment(newsText);
// positive/negative/neutral 반환
```

#### 주요 기능

1. **뉴스 수집**
   - Finnhub API 또는 웹 스크래핑
   - 실시간 뉴스 업데이트
   - 다양한 소스 통합

2. **감성 분석**
   - 뉴스 내용의 긍정/부정/중립 판단
   - 감성 점수 계산 (-1.0 ~ 1.0)
   - 투자 의사결정 지원

3. **GPT 분석**
   - 뉴스 요약
   - 시장 영향 분석
   - 투자 인사이트 도출

4. **필터링 및 검색**
   - 키워드 기반 검색
   - 주식 심볼별 필터링
   - 날짜 범위 조회

#### 데이터 소스
- **Finnhub API**: 실시간 금융 뉴스
- **Bloomberg**: 주요 경제 뉴스
- **Reuters**: 글로벌 뉴스
- **웹 스크래핑**: 추가 소스

---

### TwitterService.java
트위터(X) 데이터를 분석하여 시장 심리를 파악하는 서비스입니다.

#### 주요 메서드

##### 1. getSentimentBySymbol(String symbol)
특정 주식에 대한 트위터 감성을 분석합니다.
```java
TwitterSentiment sentiment = twitterService.getSentimentBySymbol("AAPL");
// 긍정/부정/중립 비율 반환
```

##### 2. getTrends()
금융 관련 트위터 트렌드를 조회합니다.
```java
List<TwitterTrend> trends = twitterService.getTrends();
```

##### 3. getTopMentions(int limit)
가장 많이 언급된 주식을 조회합니다.
```java
List<String> topStocks = twitterService.getTopMentions(10);
// 상위 10개 주식 심볼 반환
```

##### 4. analyzeTweets(List<TwitterDTO> tweets)
트윗 목록을 GPT로 분석합니다.
```java
String analysis = twitterService.analyzeTweets(tweets);
// 전체적인 시장 심리 분석 반환
```

##### 5. getInfluencerOpinions(String symbol)
금융 인플루언서들의 의견을 수집합니다.
```java
List<TwitterDTO> opinions = twitterService.getInfluencerOpinions("TSLA");
```

#### 주요 기능

1. **실시간 감성 분석**
   - 트위터 데이터 수집
   - 감성 점수 계산
   - 시장 심리 측정

2. **트렌드 분석**
   - 인기 해시태그 추적
   - 급등 주식 발견
   - 시장 관심도 측정

3. **인플루언서 추적**
   - 유명 투자자 트윗 모니터링
   - 의견 집계
   - 영향력 분석

4. **GPT 인사이트**
   - 트윗 요약
   - 시장 심리 해석
   - 투자 기회 도출

#### 데이터 수집
- **Twitter API v2**: 공식 API
- **검색 필터**: 금융 관련 키워드
- **인플루언서 리스트**: 주요 투자자 계정

## 📌 사용 예시

### NewsScrapingService 사용
```java
@Service
@RequiredArgsConstructor
public class NewsController {
    private final NewsScrapingService newsService;

    public List<NewsDTO> getNews() {
        // 최신 뉴스 조회
        List<NewsDTO> latest = newsService.getLatestNews();

        // 특정 주식 뉴스
        List<NewsDTO> appleNews = newsService.getNewsBySymbol("AAPL");

        // GPT 분석
        NewsDTO analyzed = newsService.analyzeNews(latest.get(0).getId());

        return latest;
    }
}
```

### TwitterService 사용
```java
@Service
@RequiredArgsConstructor
public class TwitterController {
    private final TwitterService twitterService;

    public void analyzeSentiment() {
        // 트위터 감성 분석
        TwitterSentiment sentiment = twitterService.getSentimentBySymbol("TSLA");

        System.out.println("긍정: " + sentiment.getPositiveRatio() + "%");
        System.out.println("부정: " + sentiment.getNegativeRatio() + "%");

        // 트렌드 조회
        List<TwitterTrend> trends = twitterService.getTrends();
        trends.forEach(trend -> {
            System.out.println(trend.getHashtag() + ": " + trend.getCount());
        });
    }
}
```

## 🔗 연관 패키지
- `controller/insights/` - Insights API 컨트롤러
- `dto/insights/` - NewsDTO, TwitterDTO
- `service/common/GPTService.java` - GPT API 통합

## ⚙️ 설정 요구사항

### application.properties
```properties
# Finnhub API
finnhub.api.key=your-finnhub-api-key

# Twitter API
twitter.api.key=your-twitter-api-key
twitter.api.secret=your-twitter-api-secret
twitter.bearer.token=your-bearer-token

# OpenAI (GPT)
openai.api.key=your-openai-api-key
```

## 🚀 주요 기능

### 1. 뉴스 인사이트
- 실시간 뉴스 수집
- AI 기반 분석
- 감성 분석
- 투자 기회 발견

### 2. 소셜 미디어 분석
- 트위터 감성 측정
- 시장 심리 파악
- 트렌드 추적
- 인플루언서 의견 집계

### 3. 통합 분석
- 뉴스 + 소셜 미디어 통합
- 종합 시장 심리 측정
- AI 인사이트 생성

## ⚠️ 주의사항
1. **API 키 관리**: 모든 API 키는 환경 변수로 관리
2. **Rate Limiting**: 각 API의 요청 제한 고려
3. **데이터 캐싱**: 동일한 요청 캐싱하여 API 호출 최소화
4. **에러 처리**: API 장애 시 대체 소스 사용

## 🔄 데이터 플로우
```
뉴스 소스 → NewsScrapingService → GPT 분석 → NewsDTO → Controller → Frontend
Twitter API → TwitterService → 감성 분석 → TwitterDTO → Controller → Frontend
```
