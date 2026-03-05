# Portfolio Controllers

포트폴리오 비교 및 투자자 분석 API를 제공하는 컨트롤러 패키지입니다.

## 📁 파일 목록

### PortfolioApiController.java
유명 투자자들의 투자 철학 비교 및 AI 기반 포트폴리오 추천을 제공하는 RESTful API 컨트롤러입니다.

#### 주요 엔드포인트

##### 1. 투자자 비교
- `POST /api/portfolio/compare`
  - **설명**: 선택한 투자자들의 투자 철학을 비교 분석
  - **요청 바디**: `InvestorComparisonRequest` (투자자 ID 목록)
  - **응답**: `List<InvestorComparisonDTO>` (각 투자자의 철학 분석 결과)

##### 2. 투자자 검색
- `POST /api/portfolio/search`
  - **설명**: GPT API를 사용하여 새로운 투자자 정보 검색
  - **요청 바디**: `InvestorSearchRequest` (투자자 이름)
  - **응답**: `InvestorSearchResponse` (투자자 정보)

##### 3. AI 포트폴리오 추천 ⭐ NEW
- `POST /api/portfolio/recommend`
  - **설명**: 4명의 투자자 철학을 각 25%씩 반영한 AI 추천 포트폴리오 생성
  - **요청 바디**: `InvestorComparisonRequest` (4명의 투자자 ID)
  - **응답**: `PortfolioRecommendationResponse` (추천 포트폴리오)

#### 지원하는 투자자 목록
1. **캐시 우드 (Cathie Wood)** - 파괴적 혁신 투자
2. **조지 소로스 (George Soros)** - 매크로 투자
3. **피터 틸 (Peter Thiel)** - 벤처 캐피털
4. **래리 핑크 (Larry Fink)** - ESG 투자
5. **워렌 버핏 (Warren Buffett)** - 가치투자
6. **피터 린치 (Peter Lynch)** - 성장주 투자
7. **레이 달리오 (Ray Dalio)** - 리스크 패리티
8. **벤저민 그레이엄 (Benjamin Graham)** - 가치투자 원조
9. **짐 사이먼스 (Jim Simons)** - 퀀트 투자

#### 특징
- GPT-4 API를 활용한 투자자 철학 분석
- 4명의 투자자를 선택하여 비교 분석 가능
- AI 기반 포트폴리오 추천 (각 투자자의 철학을 25%씩 반영)
- 커스텀 투자자 검색 및 추가 지원

## 📌 사용 예시

### 1. 투자자 비교 분석
```javascript
fetch('/api/portfolio/compare', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        investors: ['buffett', 'lynch', 'wood', 'soros']
    })
})
    .then(response => response.json())
    .then(data => {
        // 각 투자자의 철학 분석 결과
        data.forEach(investor => {
            console.log(investor.investorId);
            console.log(investor.philosophy); // 투자 철학 카테고리별 비중
            console.log(investor.insights);   // GPT가 생성한 인사이트
        });
    });
```

### 2. AI 포트폴리오 추천
```javascript
fetch('/api/portfolio/recommend', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        investors: ['buffett', 'lynch', 'wood', 'soros']
    })
})
    .then(response => response.json())
    .then(data => {
        console.log(data.selectedInvestors);   // 선택된 투자자 목록
        console.log(data.combinedPhilosophy);  // 통합 투자 철학
        console.log(data.recommendations);     // 추천 종목 리스트
        console.log(data.rationale);           // 추천 근거
        console.log(data.riskProfile);         // 리스크 프로필
    });
```

### 3. 투자자 검색
```javascript
fetch('/api/portfolio/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        name: 'Charlie Munger'
    })
})
    .then(response => response.json())
    .then(data => {
        console.log(data.name);        // 투자자 이름
        console.log(data.style);       // 투자 스타일
        console.log(data.description); // 투자 철학 설명
    });
```

## 🔗 연관 패키지
- `dto/portfolio/` - Portfolio 관련 DTO
  - `InvestorComparisonRequest.java`
  - `InvestorComparisonDTO.java`
  - `InvestorSearchRequest.java`
  - `InvestorSearchResponse.java`
  - `PortfolioRecommendationResponse.java`
- `service/portfolio/` - Portfolio 비즈니스 로직
  - `InvestorComparisonService.java`
- `service/common/GPTService.java` - GPT API 통합

## 🚀 주요 기능

### 1. 투자자 철학 비교
- 최대 4명의 투자자 선택 가능
- 각 투자자의 투자 철학을 카테고리별로 분석
- GPT를 활용한 심층 인사이트 제공

### 2. AI 포트폴리오 추천
- 4명의 투자자 철학을 각 25%씩 균등 반영
- GPT-4가 생성한 맞춤형 포트폴리오
- 종목별 비중, 선정 이유, 리스크 프로필 제공

### 3. 커스텀 투자자 추가
- GPT API를 통한 실시간 투자자 정보 검색
- 검색한 투자자를 비교 분석에 추가 가능

## ⚙️ 설정 요구사항
- `application.properties`에 OpenAI API 키 설정 필요
  ```properties
  openai.api.key=your-api-key-here
  ```