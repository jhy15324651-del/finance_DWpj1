# 약력(Info) 기능 구조 변경 완료 보고서

## 🎯 변경 목적
**잘못된 구조**: 섹션마다 글 1개씩 생성 → /info 목록에 섹션 개수만큼 카드 표시
**올바른 구조**: 글 1개 안에 여러 섹션 포함 → /info 목록에 글 1개당 카드 1개

## ✅ 핵심 개념 모델

```
InfoPost (글 1개)
 ├─ title: "홍길동의 약력"
 ├─ writer: "홍길동"
 ├─ createdDate: 2025-12-16
 ├─ thumbnailUrl: "/uploads/info/profile.jpg" (PROFILE 섹션 이미지)
 └─ sections: [
      ├─ ContentInfoSection(PROFILE)
      │    ├─ title: "프로필"
      │    ├─ content: "..."
      │    └─ thumbnailUrl: "/uploads/info/profile.jpg"
      ├─ ContentInfoSection(CHANNEL)
      │    ├─ title: "채널 소개"
      │    └─ content: "..."
      ├─ ContentInfoSection(ACTIVITY)
      └─ ContentInfoSection(WHY)
    ]
```

## 📋 변경 사항 상세

### 1. Entity 구조 변경

#### ✅ 새로 생성: `InfoPost` Entity
**파일**: `entity/content/InfoPost.java`

```java
@Entity
@Table(name = "info_posts")
public class InfoPost {
    private Long id;
    private String title;           // PROFILE 섹션 제목 사용
    private String thumbnailUrl;    // PROFILE 섹션 이미지 사용
    private String writer;          // 작성자 닉네임
    private Boolean isDeleted;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime deletedDate;
    private String deletedBy;

    @OneToMany(mappedBy = "infoPost", cascade = ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ContentInfoSection> sections; // 글 안에 포함된 섹션들
}
```

**핵심 메서드**:
- `addSection(ContentInfoSection section)`: 섹션 추가 헬퍼
- `getProfileSection()`: PROFILE 섹션 추출 (대표 정보 용도)
- `softDelete(String deletedBy)`: 소프트 삭제

#### ✅ 수정: `ContentInfoSection` Entity
**변경 전**:
```java
@Entity
public class ContentInfoSection {
    private Long id;
    private String sectionType;
    private String title;
    private String content;
    private String thumbnailUrl;
    private String writer;          // ❌ 제거
    private Integer displayOrder;
    private Boolean isDeleted;      // ❌ 제거
    private LocalDateTime createdDate; // ❌ 제거
    // ...
}
```

**변경 후**:
```java
@Entity
public class ContentInfoSection {
    private Long id;

    @ManyToOne(fetch = LAZY)        // ✅ 추가
    @JoinColumn(name = "info_post_id", nullable = false)
    private InfoPost infoPost;      // ✅ 추가

    private String sectionType;     // PROFILE, CHANNEL, etc.
    private String title;           // 섹션별 제목
    private String content;         // 섹션별 본문
    private String thumbnailUrl;    // 섹션별 이미지 (선택)
    private Integer displayOrder;   // 섹션 표시 순서

    // writer, createdDate, isDeleted 등은 InfoPost에만 존재
}
```

### 2. Repository 생성

#### ✅ `InfoPostRepository`
**파일**: `repository/content/InfoPostRepository.java`

```java
public interface InfoPostRepository extends JpaRepository<InfoPost, Long> {

    // 활성 게시글 목록 (섹션 포함)
    @Query("SELECT DISTINCT p FROM InfoPost p " +
           "LEFT JOIN FETCH p.sections s " +
           "WHERE p.isDeleted = false " +
           "ORDER BY p.createdDate DESC")
    List<InfoPost> findActivePostsWithSections();

    // ID로 활성 게시글 조회 (섹션 포함)
    @Query("SELECT p FROM InfoPost p " +
           "LEFT JOIN FETCH p.sections s " +
           "WHERE p.id = :id AND p.isDeleted = false")
    Optional<InfoPost> findActivePostWithSections(Long id);
}
```

### 3. Service 생성

#### ✅ `InfoPostService`
**파일**: `service/content/InfoPostService.java`

**핵심 메서드**:
```java
// 활성 게시글 목록 조회 (일반 사용자용)
public List<InfoPost> getActivePosts()

// ID로 활성 게시글 조회 (섹션 포함)
public Optional<InfoPost> getActivePostWithSections(Long id)

// 게시글 저장 (여러 섹션 포함)
public InfoPost savePost(String writer, List<SectionData> sectionsData) {
    // 1. PROFILE 섹션에서 대표 정보 추출
    SectionData profileData = sectionsData.stream()
            .filter(s -> "PROFILE".equals(s.getSectionType()))
            .findFirst()
            .orElse(sectionsData.get(0));

    // 2. PROFILE 이미지 업로드
    String thumbnailUrl = uploadImage(profileData.getImageFile());

    // 3. InfoPost 생성 (제목, 썸네일은 PROFILE 섹션 것 사용)
    InfoPost post = InfoPost.builder()
            .title(profileData.getTitle())
            .thumbnailUrl(thumbnailUrl)
            .writer(writer)
            .build();

    // 4. 각 섹션 추가
    for (SectionData data : sectionsData) {
        ContentInfoSection section = ContentInfoSection.builder()
                .sectionType(data.getSectionType())
                .title(data.getTitle())
                .content(data.getContent())
                .thumbnailUrl(uploadImage(data.getImageFile()))
                .displayOrder(order++)
                .build();

        post.addSection(section); // 양방향 관계 설정
    }

    // 5. 저장 (cascade로 섹션들도 자동 저장)
    return repository.save(post);
}
```

### 4. Controller 수정

#### ✅ `InfoController`

**변경 전**:
```java
// ❌ 잘못된 방식: 섹션마다 개별 저장
@PostMapping("/api/save-sections")
public ResponseEntity<?> saveSections(...) {
    for (각 섹션) {
        ContentInfoSection section = ContentInfoSection.builder()
                .sectionType(...)
                .title(...)
                .writer(writer) // 섹션마다 writer 저장
                .build();

        sectionService.saveSection(section); // 섹션 개별 저장
    }
}
```

**변경 후**:
```java
// ✅ 올바른 방식: 글 1개 + 여러 섹션
@PostMapping("/api/save-sections")
public ResponseEntity<?> saveSections(...) {
    // 1. 섹션 데이터 파싱
    List<InfoPostService.SectionData> sectionsData = new ArrayList<>();
    while (true) {
        String sectionType = allParams.getFirst("sections[" + index + "].sectionType");
        if (sectionType == null) break;

        sectionsData.add(new SectionData(
            sectionType, title, content, imageFile
        ));
        index++;
    }

    // 2. 글 1개 + 여러 섹션 저장
    InfoPost savedPost = postService.savePost(writer, sectionsData);

    return ResponseEntity.ok(
        "약력이 성공적으로 저장되었습니다 (섹션 " + sectionsData.size() + "개)"
    );
}
```

**목록 조회**:
```java
// 변경 전: sections 조회
@GetMapping
public String infoList(Model model) {
    List<ContentInfoSection> sections = sectionService.getActiveSections();
    model.addAttribute("sections", sections); // ❌ 섹션 개수만큼 카드
    return "content/info";
}

// 변경 후: posts 조회
@GetMapping
public String infoList(Model model) {
    List<InfoPost> posts = postService.getActivePosts();
    model.addAttribute("posts", posts); // ✅ 글 개수만큼 카드
    return "content/info";
}
```

**상세 페이지**:
```java
// 변경 전: 단일 섹션만 표시
@GetMapping("/{id}")
public String infoDetail(@PathVariable Long id, Model model) {
    Optional<ContentInfoSection> sectionOpt = sectionService.getActiveSectionById(id);
    model.addAttribute("section", sectionOpt.get()); // ❌ 섹션 1개만
    return "content/info_detail";
}

// 변경 후: 글 1개 + 모든 섹션 표시
@GetMapping("/{id}")
public String infoDetail(@PathVariable Long id, Model model) {
    Optional<InfoPost> postOpt = postService.getActivePostWithSections(id);
    model.addAttribute("post", postOpt.get()); // ✅ 글 + 모든 섹션
    return "content/info_detail";
}
```

### 5. HTML 템플릿 수정

#### ✅ `info.html` - 목록 페이지

**변경 전**:
```html
<!-- ❌ sections 순회 → 섹션 개수만큼 카드 생성 -->
<div class="post-card" th:each="section : ${sections}">
    <div class="post-title" th:text="${section.title}"></div>
    <img th:src="${section.thumbnailUrl}">
    <span th:text="${section.writer}"></span>
    <span th:text="${section.createdDate}"></span>
</div>
```

**변경 후**:
```html
<!-- ✅ posts 순회 → 글 개수만큼만 카드 생성 -->
<div class="post-card" th:each="post : ${posts}">
    <div class="post-title" th:text="${post.title}"></div>
    <img th:src="${post.thumbnailUrl}"> <!-- PROFILE 섹션 이미지 -->
    <span th:text="${post.writer}"></span>
    <span th:text="${post.createdDate}"></span>
</div>
```

#### ✅ `info_detail.html` - 상세 페이지

**변경 전**:
```html
<!-- ❌ 단일 섹션만 표시 -->
<h1 th:text="${section.title}"></h1>
<div th:text="${section.sectionType}"></div>
<div th:utext="${section.content}"></div>
```

**변경 후**:
```html
<!-- ✅ 글 정보 + 모든 섹션 순서대로 표시 -->
<h1 th:text="${post.title}">홍길동의 약력</h1>
<div class="detail-meta">
    <span th:text="'작성자: ' + ${post.writer}"></span>
    <span th:text="${post.createdDate}"></span>
</div>

<!-- 모든 섹션을 순서대로 출력 -->
<div th:each="section : ${post.sections}" class="section-block">
    <div class="category-badge" th:text="${section.sectionType}">PROFILE</div>
    <h2 class="section-title" th:text="${section.title}">프로필</h2>
    <img th:if="${section.thumbnailUrl}" th:src="${section.thumbnailUrl}">
    <div th:utext="${section.content}">본문...</div>
</div>
```

## 🎯 성공 기준 (반드시 확인)

### ✅ 저장 시
- [ ] 섹션 3개를 작성해도 **글 1개만 생성**되어야 함
- [ ] `info_posts` 테이블에 레코드 **1개** 생성
- [ ] `content_info_sections` 테이블에 레코드 **3개** 생성 (info_post_id 동일)

### ✅ 목록 페이지 (/info)
- [ ] 섹션 3개를 작성했어도 **카드 1개만** 표시되어야 함
- [ ] 카드 제목 = PROFILE 섹션의 제목
- [ ] 카드 썸네일 = PROFILE 섹션의 이미지
- [ ] 카드 작성자 = 작성자 닉네임
- [ ] 카드 작성일 = 글 작성일

### ✅ 상세 페이지 (/info/{id})
- [ ] 작성한 **모든 섹션**이 순서대로 표시되어야 함
- [ ] 섹션 표시 순서: PROFILE → CHANNEL → ACTIVITY → WHY → ...
- [ ] 각 섹션마다 타입 배지, 제목, 이미지, 본문이 보여야 함

### ❌ 실패 케이스
- 섹션 3개 작성 시 카드가 3개 생성되면 **실패**
- 상세 페이지에서 섹션 1개만 보이면 **실패**
- PROFILE이 아닌 다른 섹션 정보가 목록 카드에 나오면 **실패**

## 📊 데이터베이스 구조

### Before (잘못됨)
```
content_info_sections
---------------------
id | sectionType | title        | writer | createdDate | ...
1  | PROFILE     | 홍길동 프로필  | 홍길동  | 2025-12-16  | ...
2  | CHANNEL     | 채널 소개     | 홍길동  | 2025-12-16  | ...
3  | ACTIVITY    | 주요 활동     | 홍길동  | 2025-12-16  | ...

→ 결과: /info에서 카드 3개 표시 ❌
```

### After (올바름)
```
info_posts
---------------------
id | title        | thumbnailUrl | writer | createdDate | ...
1  | 홍길동 프로필  | /uploads/... | 홍길동  | 2025-12-16  | ...

content_info_sections
---------------------
id | info_post_id | sectionType | title        | content | displayOrder
1  | 1            | PROFILE     | 홍길동 프로필  | ...     | 1
2  | 1            | CHANNEL     | 채널 소개     | ...     | 2
3  | 1            | ACTIVITY    | 주요 활동     | ...     | 3

→ 결과: /info에서 카드 1개 표시 ✅
→ 상세 페이지에서 섹션 3개 모두 표시 ✅
```

## 🔄 데이터 마이그레이션 (필요 시)

**기존 데이터가 있다면 마이그레이션 필요**:
1. 기존 `content_info_sections` 백업
2. `info_posts` 테이블 생성
3. 작성자별로 섹션들을 그룹핑하여 `info_posts` 생성
4. 각 섹션에 `info_post_id` 설정
5. 기존 섹션의 writer, createdDate, isDeleted 컬럼 제거

**마이그레이션 SQL 예시**:
```sql
-- 1. info_posts 생성
CREATE TABLE info_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    thumbnail_url VARCHAR(255),
    writer VARCHAR(50) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL,
    updated_date TIMESTAMP NOT NULL,
    deleted_date TIMESTAMP,
    deleted_by VARCHAR(50)
);

-- 2. content_info_sections에 info_post_id 컬럼 추가
ALTER TABLE content_info_sections
ADD COLUMN info_post_id BIGINT;

-- 3. 마이그레이션 로직 (작성자별 그룹핑)
-- Spring Boot 코드로 수행 권장
```

## 📌 주의사항

1. **CASCADE 설정**: InfoPost 삭제 시 연결된 모든 섹션도 자동 삭제됨
2. **LAZY Loading**: 섹션 조회 시 `LEFT JOIN FETCH` 사용하여 N+1 문제 방지
3. **대표 정보**: 항상 PROFILE 섹션의 정보를 사용 (없으면 첫 번째 섹션)
4. **정렬 순서**: `displayOrder` 기준 오름차순 정렬

## 🎉 결론

### 변경 요약
- **Entity**: InfoPost 추가, ContentInfoSection 수정
- **Repository**: InfoPostRepository 추가
- **Service**: InfoPostService 추가
- **Controller**: 글 1개 + 여러 섹션 저장 로직으로 변경
- **HTML**: posts 순회 / 모든 섹션 표시

### 성공 기준
✅ 섹션 3개 작성 → 카드 1개만 표시
✅ 상세 페이지에서 모든 섹션 표시
✅ PROFILE 섹션 정보가 대표 정보로 사용

---

**작성일**: 2025-12-16
**작성자**: Claude Sonnet 4.5