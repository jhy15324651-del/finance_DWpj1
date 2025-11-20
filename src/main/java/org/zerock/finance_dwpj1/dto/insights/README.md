# Insights DTOs

뉴스 및 소셜 미디어 인사이트 데이터를 전달하기 위한 DTO(Data Transfer Object) 패키지입니다.

## 📁 파일 목록

### NewsDTO.java
뉴스 데이터를 전달하는 DTO입니다.

#### 주요 필드
```java
public class NewsDTO {
    private Long id;                // 뉴스 ID
    private String title;           // 제목
    private String summary;         // 요약
    private String url;             // 뉴스 원본 URL
    private String source;          // 뉴스 소스 (예: Bloomberg, Reuters)
    private LocalDateTime publishedAt; // 발행 시각
    private String category;        // 카테고리 (예: 시장, 경제, 기업)
    private List<String> relatedSymbols; // 관련 주식 심볼
    private String sentiment;       // 감성 분석 결과 (positive/negative/neutral)
    private Double sentimentScore;  // 감성 점수 (-1.0 ~ 1.0)
    private String gptAnalysis;     // GPT가 생성한 분석 내용
}
```

#### 사용 사례
- 뉴스 목록 조회 응답
- 뉴스 검색 결과
- 뉴스 상세 정보 전달
- GPT 분석 결과 포함

---

### TwitterDTO.java
트위터(X) 데이터를 전달하는 DTO입니다.

#### 주요 필드
```java
public class TwitterDTO {
    private Long id;                // 트윗 ID
    private String text;            // 트윗 내용
    private String username;        // 작성자 사용자명
    private String displayName;     // 작성자 표시 이름
    private LocalDateTime createdAt; // 작성 시각
    private Integer likeCount;      // 좋아요 수
    private Integer retweetCount;   // 리트윗 수
    private Integer replyCount;     // 답글 수
    private String sentiment;       // 감성 분석 결과
    private Double sentimentScore;  // 감성 점수
    private List<String> hashtags;  // 해시태그 목록
    private List<String> mentions;  // 멘션된 계정
    private String symbol;          // 관련 주식 심볼
}
```

#### 사용 사례
- 트위터 감성 분석 응답
- 트렌드 분석 데이터
- 소셜 미디어 인사이트
- 시장 심리 측정

## 📌 사용 예시

### NewsDTO 사용
```java
NewsDTO news = NewsDTO.builder()
    .title("Apple Announces Q4 Earnings")
    .summary("Apple reports strong Q4 earnings...")
    .source("Bloomberg")
    .publishedAt(LocalDateTime.now())
    .relatedSymbols(Arrays.asList("AAPL"))
    .sentiment("positive")
    .sentimentScore(0.85)
    .gptAnalysis("GPT analysis of the news...")
    .build();
```

### TwitterDTO 사용
```java
TwitterDTO tweet = TwitterDTO.builder()
    .text("$AAPL looking strong today!")
    .username("investor123")
    .createdAt(LocalDateTime.now())
    .sentiment("positive")
    .sentimentScore(0.75)
    .hashtags(Arrays.asList("AAPL", "stocks"))
    .symbol("AAPL")
    .build();
```

## 🔗 연관 패키지
- `controller/insights/` - Insights API 컨트롤러
- `service/insights/` - Insights 비즈니스 로직

## 💡 설계 원칙
- **불변성**: Lombok의 `@Builder`와 `@Data` 사용
- **유효성 검증**: 필요시 JSR-303 어노테이션 활용
- **직렬화**: JSON 직렬화 지원
- **가독성**: 명확한 필드명 사용
