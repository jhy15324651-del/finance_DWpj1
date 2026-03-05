# Entity Package

데이터베이스 테이블과 매핑되는 JPA 엔티티 클래스를 담당하는 패키지입니다.

## 📁 패키지 구조

```
entity/
└── insights/
    ├── News.java          # 뉴스 기사 엔티티
    ├── Comment.java       # 댓글 엔티티
    └── AdminUser.java     # 관리자 계정 엔티티
```

## 📊 데이터베이스 스키마

### News (뉴스 기사)
Yahoo Finance에서 크롤링한 금융 뉴스를 저장합니다.

**테이블명**: `news`

**필드**:
- `id`: Primary Key (자동 증가)
- `title`: 뉴스 제목 (최대 500자)
- `content`: 뉴스 원문 (TEXT)
- `summary`: GPT 요약 내용 (TEXT)
- `url`: 원본 URL (UNIQUE, 중복 방지)
- `source`: 출처 (예: Yahoo Finance)
- `published_at`: 기사 발행 시간
- `created_at`: 크롤링된 시간 (자동 생성)
- `view_count`: 조회수 (기본값 0)
- `status`: 뉴스 상태 (DAILY/ARCHIVE)
- `is_deleted`: 삭제 여부 (소프트 삭제)

**인덱스**:
- `idx_status_created_at`: 상태와 생성 시간으로 정렬
- `idx_view_count`: 조회수 정렬 (금주의 뉴스)
- `idx_url`: URL 중복 방지 (UNIQUE)

**주요 메서드**:
- `incrementViewCount()`: 조회수 1 증가
- `isOver24Hours()`: 24시간 경과 여부 확인
- `archiveNews()`: 상태를 ARCHIVE로 변경
- `softDelete()`: 소프트 삭제

---

### Comment (댓글)
사용자가 뉴스에 작성한 댓글을 저장합니다.

**테이블명**: `comment`

**필드**:
- `id`: Primary Key (자동 증가)
- `news_id`: 뉴스 ID (Foreign Key → News)
- `user_name`: 댓글 작성자 이름
- `content`: 댓글 내용 (TEXT)
- `created_at`: 작성 시간 (자동 생성)
- `is_deleted`: 삭제 여부 (소프트 삭제)

**인덱스**:
- `idx_news_id_created_at`: 뉴스별 댓글 조회 최적화

**관계**:
- `@ManyToOne` → News (지연 로딩)

**주요 메서드**:
- `softDelete()`: 소프트 삭제

---

### AdminUser (관리자)
뉴스 관리 권한을 가진 관리자 계정입니다.

**테이블명**: `admin_user`

**필드**:
- `id`: Primary Key (자동 증가)
- `username`: 사용자명 (UNIQUE)
- `password`: 암호화된 비밀번호 (BCrypt)
- `role`: 권한 (ADMIN/USER)

**권한**:
- `ADMIN`: 뉴스 수정/삭제 가능
- `USER`: 조회, 댓글만 가능

## 🔗 연관 패키지

- `repository/insights/` - JPA Repository
- `dto/insights/` - 데이터 전송 객체
- `service/insights/` - 비즈니스 로직

## 💡 설계 원칙

### 1. 소프트 삭제 (Soft Delete)
물리적 삭제 대신 `isDeleted` 플래그를 사용하여 데이터를 보존합니다.

```java
news.softDelete();  // isDeleted = true
```

### 2. 낙관적 락 (Optimistic Lock)
필요시 `@Version` 어노테이션을 추가하여 동시성 제어 가능

### 3. 인덱스 최적화
자주 사용되는 쿼리 패턴에 맞춰 인덱스 설계:
- 상태별 최신 뉴스 조회
- 조회수 순 정렬 (금주의 뉴스)
- URL 중복 체크

### 4. 지연 로딩 (Lazy Loading)
연관 관계는 기본적으로 LAZY 로딩 사용

## 📌 사용 예시

### News 엔티티
```java
News news = News.builder()
    .title("Apple Reports Q4 Earnings")
    .content("Full article content...")
    .summary("GPT-generated summary...")
    .url("https://finance.yahoo.com/news/...")
    .source("Yahoo Finance")
    .publishedAt(LocalDateTime.now())
    .build();

// 조회수 증가
news.incrementViewCount();

// 24시간 경과 확인
if (news.isOver24Hours()) {
    news.archiveNews();
}
```

### Comment 엔티티
```java
Comment comment = Comment.builder()
    .news(newsEntity)
    .userName("홍길동")
    .content("좋은 뉴스네요!")
    .build();
```

### AdminUser 엔티티
```java
AdminUser admin = AdminUser.builder()
    .username("admin")
    .password(passwordEncoder.encode("password"))
    .role(AdminUser.Role.ADMIN)
    .build();
```

## ⚠️ 주의사항

1. **URL 중복 방지**: `url` 필드는 UNIQUE 제약이 있어 같은 뉴스는 한 번만 저장됩니다.
2. **소프트 삭제**: 삭제된 데이터는 `isDeleted = true`로 표시되며, 쿼리 시 제외해야 합니다.
3. **지연 로딩**: 연관 엔티티 사용 시 LazyInitializationException 주의
4. **비밀번호 암호화**: AdminUser의 password는 반드시 BCrypt로 암호화 후 저장
