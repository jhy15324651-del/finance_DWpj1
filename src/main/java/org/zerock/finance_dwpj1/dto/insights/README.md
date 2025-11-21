# Insights DTOs

뉴스 및 소셜 미디어 인사이트 데이터를 전달하기 위한 DTO(Data Transfer Object) 패키지입니다.

## 📁 파일 목록

### DailyNewsDTO.java ⭐ NEW
Yahoo Finance 크롤링 데일리 뉴스 데이터를 전달하는 DTO입니다.

#### 주요 필드
```java
public class DailyNewsDTO {
    private Long id;                // 뉴스 ID
    private String title;           // 제목
    private String content;         // 원문
    private String summary;         // GPT 요약
    private String url;             // 뉴스 원본 URL
    private String source;          // 출처 (Yahoo Finance)
    private String publishedAt;     // 발행 시각 (포맷팅됨)
    private String createdAt;       // 크롤링 시각 (포맷팅됨)
    private Long viewCount;         // 조회수
    private String status;          // DAILY or ARCHIVE
    private Long commentCount;      // 댓글 개수
}
```

#### 주요 메서드
```java
// Entity → DTO 변환
DailyNewsDTO.fromEntity(News news);
DailyNewsDTO.fromEntity(News news, Long commentCount);

// DTO → Entity 변환
news.toEntity();
```

#### 사용 사례
- 데일리 뉴스 목록 조회
- 아카이브 뉴스 조회
- 금주의 뉴스 (조회수 TOP)
- 뉴스 상세 정보

---

### CommentDTO.java ⭐ NEW
뉴스 댓글 데이터를 전달하는 DTO입니다.

#### 주요 필드
```java
public class CommentDTO {
    private Long id;                // 댓글 ID
    private Long newsId;            // 뉴스 ID
    private String userName;        // 작성자 이름
    private String content;         // 댓글 내용
    private String createdAt;       // 작성 시각 (포맷팅됨)
}
```

#### 주요 메서드
```java
// Entity → DTO 변환
CommentDTO.fromEntity(Comment comment);

// DTO → Entity 변환
comment.toEntity(News news);
```

#### 사용 사례
- 뉴스별 댓글 목록 조회
- 댓글 작성/수정/삭제
- 댓글 개수 집계

---

### NewsDTO.java
뉴스 데이터를 전달하는 DTO입니다 (기존 외부 API용).

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

### DailyNewsDTO 사용
```java
// Entity → DTO 변환
News news = newsRepository.findById(1L).orElseThrow();
Long commentCount = commentRepository.countByNewsId(news.getId());
DailyNewsDTO dto = DailyNewsDTO.fromEntity(news, commentCount);

// DTO → Entity 변환 (크롤링 시)
DailyNewsDTO crawledDto = DailyNewsDTO.builder()
    .title("Apple Announces Q4 Earnings")
    .content("Full article content...")
    .summary("GPT-generated summary...")
    .url("https://finance.yahoo.com/news/...")
    .source("Yahoo Finance")
    .publishedAt("2025-11-20 10:00")
    .build();

News newsEntity = crawledDto.toEntity();
newsRepository.save(newsEntity);
```

### CommentDTO 사용
```java
// DTO → Entity 변환 (댓글 작성)
CommentDTO commentDto = CommentDTO.builder()
    .newsId(newsId)
    .userName("홍길동")
    .content("유익한 뉴스입니다!")
    .build();

News news = newsRepository.findById(commentDto.getNewsId()).orElseThrow();
Comment comment = commentDto.toEntity(news);
commentRepository.save(comment);

// Entity → DTO 변환 (댓글 조회)
List<Comment> comments = commentRepository.findByNewsId(newsId);
List<CommentDTO> commentDtos = comments.stream()
    .map(CommentDTO::fromEntity)
    .collect(Collectors.toList());
```

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
- `entity/insights/` - JPA 엔티티
- `repository/insights/` - JPA Repository

## 💡 설계 원칙
- **불변성**: Lombok의 `@Builder`와 `@Data` 사용
- **유효성 검증**: 필요시 JSR-303 어노테이션 활용
- **직렬화**: JSON 직렬화 지원
- **가독성**: 명확한 필드명 사용
- **변환 메서드**: Entity ↔ DTO 변환 메서드 제공
- **날짜 포맷팅**: LocalDateTime을 String으로 변환 (프론트엔드 편의성)

## 🔄 DTO 사용 플로우

### 데일리 뉴스 조회
```
Database (News Entity)
    ↓
Repository.findDailyNews()
    ↓
DailyNewsDTO.fromEntity(news, commentCount)
    ↓
Controller → Frontend (JSON)
```

### 크롤링 & 저장
```
Yahoo Finance 크롤링
    ↓
GPT 요약
    ↓
DailyNewsDTO 생성
    ↓
dto.toEntity()
    ↓
Repository.save(entity)
```

### 댓글 작성
```
Frontend (JSON)
    ↓
Controller receives CommentDTO
    ↓
dto.toEntity(news)
    ↓
Repository.save(comment)
```
