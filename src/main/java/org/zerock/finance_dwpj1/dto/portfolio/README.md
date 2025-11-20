# Portfolio DTOs

포트폴리오 비교 및 투자자 분석 데이터를 전달하기 위한 DTO(Data Transfer Object) 패키지입니다.

## 📁 파일 목록

### InvestorComparisonRequest.java
투자자 비교 요청을 위한 DTO입니다.

#### 주요 필드
```java
public class InvestorComparisonRequest {
    private List<String> investors; // 비교할 투자자 ID 목록 (최대 4개)
}
```

#### 사용 사례
- 투자자 철학 비교 요청
- AI 포트폴리오 추천 요청

---

### InvestorComparisonDTO.java
투자자 비교 분석 결과를 전달하는 DTO입니다.

#### 주요 필드
```java
public class InvestorComparisonDTO {
    private String investorId;           // 투자자 ID
    private List<PhilosophyItem> philosophy; // 투자 철학 카테고리별 비중
    private String insights;             // GPT가 생성한 인사이트

    @Data
    @Builder
    public static class PhilosophyItem {
        private String category;         // 투자 철학 카테고리
        private Integer percentage;      // 비중 (%)
    }
}
```

#### 사용 사례
- 투자자 철학 비교 응답
- 투자 스타일 분석 결과

---

### InvestorSearchRequest.java
투자자 검색 요청을 위한 DTO입니다.

#### 주요 필드
```java
public class InvestorSearchRequest {
    private String name; // 검색할 투자자 이름
}
```

#### 사용 사례
- GPT를 활용한 투자자 정보 검색

---

### InvestorSearchResponse.java
투자자 검색 결과를 전달하는 DTO입니다.

#### 주요 필드
```java
public class InvestorSearchResponse {
    private String name;        // 투자자 이름
    private String style;       // 투자 스타일
    private String description; // 투자 철학 설명
}
```

#### 사용 사례
- 투자자 검색 응답
- 커스텀 투자자 추가

---

### PortfolioRecommendationResponse.java ⭐ NEW
AI 포트폴리오 추천 결과를 전달하는 DTO입니다.

#### 주요 필드
```java
public class PortfolioRecommendationResponse {
    private List<String> selectedInvestors;      // 선택된 투자자 목록
    private String combinedPhilosophy;           // 통합 투자 철학
    private List<StockRecommendation> recommendations; // 추천 종목 목록
    private String rationale;                    // 추천 근거
    private String riskProfile;                  // 리스크 프로필

    @Data
    @Builder
    public static class StockRecommendation {
        private String symbol;      // 주식 심볼
        private String name;        // 종목명
        private String sector;      // 섹터
        private Double allocation;  // 비중 (%)
        private String reason;      // 선정 이유
    }
}
```

#### 사용 사례
- AI 포트폴리오 추천 응답
- 4명의 투자자 철학을 25%씩 반영한 맞춤형 포트폴리오

## 📌 사용 예시

### 1. 투자자 비교 요청
```java
InvestorComparisonRequest request = InvestorComparisonRequest.builder()
    .investors(Arrays.asList("buffett", "lynch", "wood", "soros"))
    .build();

// API 호출
List<InvestorComparisonDTO> response = portfolioService.compareInvestors(request);
```

### 2. 투자자 검색
```java
InvestorSearchRequest request = InvestorSearchRequest.builder()
    .name("Charlie Munger")
    .build();

InvestorSearchResponse response = portfolioService.searchInvestor(request);
```

### 3. AI 포트폴리오 추천
```java
InvestorComparisonRequest request = InvestorComparisonRequest.builder()
    .investors(Arrays.asList("buffett", "lynch", "wood", "soros"))
    .build();

PortfolioRecommendationResponse response =
    portfolioService.generatePortfolioRecommendation(request);

// 추천 종목 확인
response.getRecommendations().forEach(stock -> {
    System.out.println(stock.getSymbol() + ": " + stock.getAllocation() + "%");
    System.out.println("이유: " + stock.getReason());
});
```

### 4. 투자 철학 분석 결과 사용
```java
InvestorComparisonDTO buffett = response.get(0);

// 투자 철학 카테고리별 비중 확인
buffett.getPhilosophy().forEach(item -> {
    System.out.println(item.getCategory() + ": " + item.getPercentage() + "%");
});

// GPT 인사이트 확인
System.out.println("인사이트: " + buffett.getInsights());
```

## 🔗 연관 패키지
- `controller/portfolio/` - Portfolio API 컨트롤러
- `service/portfolio/` - Portfolio 비즈니스 로직
- `service/common/GPTService.java` - GPT API 통합

## 💡 설계 원칙
- **불변성**: Lombok의 `@Builder`와 `@Data` 사용
- **중첩 DTO**: 복잡한 데이터 구조를 위한 내부 클래스 활용
- **유효성 검증**: `@NotNull`, `@Size` 등의 어노테이션 활용 가능
- **명확성**: 각 필드의 목적과 의미가 명확한 네이밍

## 🚀 주요 기능별 DTO 매핑

| 기능 | 요청 DTO | 응답 DTO |
|------|----------|----------|
| 투자자 비교 | `InvestorComparisonRequest` | `List<InvestorComparisonDTO>` |
| 투자자 검색 | `InvestorSearchRequest` | `InvestorSearchResponse` |
| AI 포트폴리오 추천 | `InvestorComparisonRequest` | `PortfolioRecommendationResponse` |
