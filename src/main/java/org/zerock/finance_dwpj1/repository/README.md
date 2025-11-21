# Repository Package

JPA Repository 인터페이스를 담당하는 패키지입니다. 데이터베이스 접근 로직을 추상화합니다.

## 📁 패키지 구조

```
repository/
└── insights/
    ├── NewsRepository.java         # 뉴스 Repository
    ├── CommentRepository.java      # 댓글 Repository
    └── AdminUserRepository.java    # 관리자 Repository
```

## 📊 Repository 상세

### NewsRepository
뉴스 데이터 접근 및 조회를 담당합니다.

#### 주요 메서드

##### 1. 중복 체크
```java
Optional<News> findByUrl(String url);
boolean existsByUrl(String url);
```

**사용 예시**:
```java
// 같은 URL의 뉴스가 이미 있는지 확인
if (newsRepository.existsByUrl(newsUrl)) {
    log.info("중복 뉴스: {}", newsUrl);
    return;
}
```

##### 2. 데일리 뉴스 조회
```java
List<News> findDailyNews();
Page<News> findDailyNews(Pageable pageable);
```

**사용 예시**:
```java
// 24시간 이내 최신 뉴스
List<News> dailyNews = newsRepository.findDailyNews();

// 페이징
Pageable pageable = PageRequest.of(0, 10);
Page<News> page = newsRepository.findDailyNews(pageable);
```

##### 3. 아카이브 뉴스 조회
```java
Page<News> findArchiveNews(Pageable pageable);
```

##### 4. 금주의 뉴스 (조회수 TOP)
```java
List<News> findTopNewsByViewCount(LocalDateTime weekAgo, Pageable pageable);
```

**사용 예시**:
```java
// 최근 7일간 조회수 TOP 10
LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
Pageable top10 = PageRequest.of(0, 10);
List<News> topNews = newsRepository.findTopNewsByViewCount(weekAgo, top10);
```

##### 5. 아카이브 대상 뉴스 조회
```java
List<News> findNewsToArchive(LocalDateTime twentyFourHoursAgo);
```

**사용 예시**:
```java
// 스케줄러에서 사용 - 24시간 경과한 뉴스 찾기
LocalDateTime threshold = LocalDateTime.now().minusHours(24);
List<News> toArchive = newsRepository.findNewsToArchive(threshold);
```

##### 6. 검색
```java
Page<News> searchByTitle(String keyword, Pageable pageable);
```

---

### CommentRepository
댓글 데이터 접근을 담당합니다.

#### 주요 메서드

##### 1. 뉴스별 댓글 조회
```java
List<Comment> findByNewsId(Long newsId);
Page<Comment> findByNewsId(Long newsId, Pageable pageable);
```

**사용 예시**:
```java
// 특정 뉴스의 모든 댓글
List<Comment> comments = commentRepository.findByNewsId(newsId);

// 페이징
Pageable pageable = PageRequest.of(0, 20);
Page<Comment> page = commentRepository.findByNewsId(newsId, pageable);
```

##### 2. 댓글 개수 조회
```java
Long countByNewsId(Long newsId);
```

**사용 예시**:
```java
Long commentCount = commentRepository.countByNewsId(newsId);
System.out.println("댓글 " + commentCount + "개");
```

---

### AdminUserRepository
관리자 계정 데이터 접근을 담당합니다.

#### 주요 메서드

##### 1. 사용자명으로 조회
```java
Optional<AdminUser> findByUsername(String username);
```

**사용 예시**:
```java
// 로그인 시 사용
Optional<AdminUser> admin = adminUserRepository.findByUsername("admin");
if (admin.isPresent() && passwordEncoder.matches(password, admin.get().getPassword())) {
    // 로그인 성공
}
```

##### 2. 사용자명 존재 여부
```java
boolean existsByUsername(String username);
```

**사용 예시**:
```java
// 회원가입 시 중복 체크
if (adminUserRepository.existsByUsername("admin")) {
    throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
}
```

## 🔗 연관 패키지

- `entity/insights/` - JPA 엔티티
- `service/insights/` - 비즈니스 로직
- `controller/insights/` - API 컨트롤러

## 💡 쿼리 최적화

### 1. JPQL 사용
복잡한 조회 조건은 `@Query` 어노테이션으로 JPQL 작성

```java
@Query("SELECT n FROM News n WHERE n.status = 'DAILY' AND n.isDeleted = false ORDER BY n.createdAt DESC")
List<News> findDailyNews();
```

### 2. 인덱스 활용
Entity에 정의된 인덱스를 활용하여 쿼리 성능 최적화:
- `idx_status_created_at`: 상태별 최신 뉴스 조회
- `idx_view_count`: 조회수 순 정렬
- `idx_url`: URL 중복 체크

### 3. 페이징
대량 데이터 조회 시 `Pageable` 사용

```java
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
Page<News> newsPage = newsRepository.findDailyNews(pageable);
```

## 📌 사용 예시

### Service에서 Repository 사용
```java
@Service
@RequiredArgsConstructor
public class DailyNewsService {
    private final NewsRepository newsRepository;
    private final CommentRepository commentRepository;

    public List<News> getDailyNews() {
        // 24시간 이내 최신 뉴스
        return newsRepository.findDailyNews();
    }

    public List<News> getWeeklyTopNews() {
        // 최근 7일간 조회수 TOP 10
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        Pageable top10 = PageRequest.of(0, 10);
        return newsRepository.findTopNewsByViewCount(weekAgo, top10);
    }

    public void archiveOldNews() {
        // 24시간 경과한 뉴스를 ARCHIVE 상태로 변경
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<News> toArchive = newsRepository.findNewsToArchive(threshold);

        toArchive.forEach(News::archiveNews);
        newsRepository.saveAll(toArchive);
    }
}
```

## ⚠️ 주의사항

1. **N+1 문제**: 연관 엔티티 조회 시 `@EntityGraph` 또는 `fetch join` 사용 고려
2. **페이징 처리**: 대량 데이터는 반드시 페이징 처리
3. **소프트 삭제**: 모든 조회 쿼리에 `isDeleted = false` 조건 포함
4. **트랜잭션**: Service 계층에서 `@Transactional` 처리
