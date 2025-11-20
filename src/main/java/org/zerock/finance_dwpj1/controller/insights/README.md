# Insights Controllers

뉴스 인사이트 및 소셜 미디어 인사이트 API를 제공하는 컨트롤러 패키지입니다.

## 📁 파일 목록

### NewsApiController.java
금융 뉴스 데이터를 제공하는 RESTful API 컨트롤러입니다.

#### 주요 엔드포인트
- `GET /api/news` - 최신 금융 뉴스 목록 조회
- `GET /api/news/{id}` - 특정 뉴스 상세 정보 조회
- `GET /api/news/search` - 뉴스 검색
- `POST /api/news/analyze` - GPT를 활용한 뉴스 분석

#### 특징
- Finnhub API 또는 기타 뉴스 소스 통합
- GPT를 활용한 뉴스 분석 및 인사이트 제공
- 실시간 뉴스 데이터 제공

---

### TwitterApiController.java
트위터(X) 데이터를 분석하여 시장 심리를 파악하는 API 컨트롤러입니다.

#### 주요 엔드포인트
- `GET /api/twitter/sentiment` - 특정 주식/키워드에 대한 트위터 감성 분석
- `GET /api/twitter/trends` - 금융 관련 트위터 트렌드 조회
- `POST /api/twitter/analyze` - GPT를 활용한 트위터 데이터 분석

#### 특징
- 트위터 API 통합
- 감성 분석을 통한 시장 심리 파악
- GPT를 활용한 소셜 미디어 인사이트 생성

## 📌 사용 예시

### 뉴스 API 사용
```javascript
// 최신 뉴스 조회
fetch('/api/news')
    .then(response => response.json())
    .then(data => console.log(data));

// 뉴스 분석 요청
fetch('/api/news/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        newsId: 123,
        analysisType: 'sentiment'
    })
})
    .then(response => response.json())
    .then(data => console.log(data));
```

### 트위터 감성 분석 사용
```javascript
// 특정 주식에 대한 트위터 감성 분석
fetch('/api/twitter/sentiment?symbol=AAPL')
    .then(response => response.json())
    .then(data => console.log(data));
```

## 🔗 연관 패키지
- `dto/insights/` - Insights 관련 DTO
- `service/insights/` - Insights 비즈니스 로직
- `service/common/GPTService.java` - GPT API 통합

## 🚀 주요 기능
1. **뉴스 인사이트**
   - 실시간 금융 뉴스 수집
   - AI 기반 뉴스 분석
   - 투자 인사이트 도출

2. **소셜 미디어 분석**
   - 트위터 감성 분석
   - 시장 심리 측정
   - 투자자 의견 집계