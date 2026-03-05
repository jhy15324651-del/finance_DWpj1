# 약력 소개 섹션 (Info Section) 구현 완료 보고서

## 📋 개요
사용자가 섹션별로 약력을 작성하고 관리할 수 있는 기능을 구현했습니다.
관리자는 오직 삭제 권한만 가지며, 일반 사용자가 컨텐츠를 작성합니다.

## ✅ 완료된 작업 목록

### 1. 프론트엔드 수정

#### `info_form.html` - 약력 작성 폼
**변경 사항:**
- ✅ **작성자 input 제거**: 사용자가 직접 입력할 수 없음 → 서버에서 로그인한 사용자의 닉네임 자동 주입
- ✅ **작성일 input 제거**: 사용자가 직접 입력할 수 없음 → 서버에서 자동 생성 (yyyy.MM.dd 형식)
- ✅ **썸네일 URL input 제거 → 사진 업로드 file input으로 변경**: 각 섹션마다 파일 업로드 가능
- ✅ **폼 인코딩 변경**: `method="post" enctype="multipart/form-data"` 추가
- ✅ **JavaScript 전면 수정**:
  - JSON 전송 방식 → FormData 방식으로 변경
  - 파일 업로드 지원
  - 삭제된 필드(writer, createdDate, thumbnailUrl) 참조 제거

**주요 코드:**
```html
<!-- 폼 태그 -->
<form id="sectionsForm" method="post" enctype="multipart/form-data">

<!-- 각 섹션의 입력 필드 (PROFILE, CHANNEL, ACTIVITY, WHY, SIGNATURE, SOURCES) -->
<div class="form-group">
    <label>사진 업로드</label>
    <input type="file" class="form-control" id="image_profile" accept="image/*">
</div>
```

```javascript
// FormData를 사용한 파일 업로드 처리
const formData = new FormData();
formData.append('sections[' + index + '].sectionType', sectionType.toUpperCase());
formData.append('sections[' + index + '].title', title);
formData.append('sections[' + index + '].content', content);
if (imageFile) {
    formData.append('sections[' + index + '].imageFile', imageFile);
}
```

#### `info.html` - 약력 목록 페이지
**변경 사항:**
- ✅ **관리자 삭제 버튼 제거**: 일반 사용자 페이지에서 삭제 기능 완전 제거
- ✅ **관련 JavaScript 제거**: `deleteSection()` 함수 제거

#### `info_detail.html` - 약력 상세 페이지
**변경 사항:**
- ✅ **관리자 삭제 버튼 제거**: 상세 페이지에서도 삭제 기능 완전 제거
- ✅ **관련 JavaScript 제거**: `deleteSectionFromDetail()` 함수 제거

### 2. 백엔드 수정

#### `InfoController.java`
**주요 변경 사항:**
```java
// 변경 전: JSON 방식
@PostMapping("/api/save-sections")
@ResponseBody
public ResponseEntity<?> saveSections(@RequestBody SaveSectionsRequest request, Principal principal) {
    String writer = principal.getName(); // 이메일 반환
    // ...
}

// 변경 후: Multipart 방식 + 닉네임 자동 주입
@PostMapping("/api/save-sections")
@ResponseBody
public ResponseEntity<?> saveSections(
        @RequestParam MultiValueMap<String, String> allParams,
        @RequestParam(required = false) Map<String, MultipartFile> files,
        @AuthenticationPrincipal CustomUserDetails userDetails) {

    String writer = userDetails.getNickname(); // 닉네임 자동 주입

    // FormData 파싱: sections[0].sectionType, sections[0].title, ...
    while (true) {
        String sectionType = allParams.getFirst("sections[" + index + "].sectionType");
        if (sectionType == null) break;

        // 파일 업로드 처리
        MultipartFile imageFile = files.get("sections[" + index + "].imageFile");
        String thumbnailUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            thumbnailUrl = sectionService.uploadImage(imageFile);
        }

        // 섹션 저장 (작성자는 닉네임으로 자동 설정)
        ContentInfoSection section = ContentInfoSection.builder()
                .sectionType(sectionType)
                .title(title)
                .content(content)
                .thumbnailUrl(thumbnailUrl)
                .writer(writer) // 닉네임 자동 설정
                .displayOrder(999)
                .isDeleted(false)
                .build();

        sectionService.saveSection(section);
        index++;
    }
}
```

**핵심 개선점:**
1. ✅ `@AuthenticationPrincipal CustomUserDetails` 사용으로 닉네임 접근
2. ✅ `MultiValueMap`과 `Map<String, MultipartFile>`로 FormData 파싱
3. ✅ 작성자(writer) 필드 자동 주입 - 사용자가 조작 불가
4. ✅ 작성일(createdDate)은 Entity의 `@CreationTimestamp`로 자동 생성
5. ✅ 파일 업로드 처리 로직 추가

#### `ContentInfoSectionService.java`
**새로 추가된 메서드:**
```java
/**
 * 이미지 파일 업로드
 *
 * @param file 업로드할 이미지 파일
 * @return 저장된 파일의 웹 접근 경로 (예: /uploads/info/uuid.jpg)
 */
public String uploadImage(MultipartFile file) {
    // 파일 저장 디렉토리: uploads/info
    String uploadDir = "uploads/info";
    Path uploadPath = Paths.get(uploadDir);

    // 디렉토리 생성
    if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
    }

    // UUID + 타임스탬프로 고유한 파일명 생성
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String uniqueFilename = UUID.randomUUID().toString() + "_" + timestamp + extension;

    // 파일 저장
    Path filePath = uploadPath.resolve(uniqueFilename);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    // 웹 접근 경로 반환
    return "/" + uploadDir + "/" + uniqueFilename;
}
```

**기능:**
- ✅ 파일을 `uploads/info/` 디렉토리에 저장
- ✅ UUID + 타임스탬프로 중복 방지
- ✅ 웹에서 접근 가능한 경로 반환

## 🔒 보안 강화

### 1. 작성자 조작 불가
- **이전**: 프론트엔드에서 작성자 이름 입력 가능 → 악의적 사용자가 다른 사람 이름으로 작성 가능
- **현재**: 서버에서 `CustomUserDetails.getNickname()` 사용 → Spring Security로 인증된 사용자의 닉네임만 사용

### 2. 작성일 조작 불가
- **이전**: 프론트엔드에서 작성일 입력 가능 → 과거/미래 날짜 조작 가능
- **현재**: Entity의 `@CreationTimestamp`로 DB 레벨에서 자동 생성

### 3. 삭제 권한 분리
- **일반 사용자 페이지 (`/info`, `/info/{id}`)**: 삭제 버튼 완전 제거
- **관리자 페이지**: 별도 관리자 페이지에서만 삭제 가능 (구현 필요)

## 📂 파일 구조

```
finance_DWpj1/
├── src/main/resources/templates/content/
│   ├── info.html                      # 약력 목록 (삭제 버튼 제거됨)
│   ├── info_form.html                 # 약력 작성 폼 (파일 업로드 추가)
│   └── info_detail.html               # 약력 상세 (삭제 버튼 제거됨)
├── src/main/java/org/zerock/finance_dwpj1/
│   ├── controller/content/
│   │   └── InfoController.java        # Multipart 처리, 닉네임 자동 주입
│   └── service/content/
│       └── ContentInfoSectionService.java  # 파일 업로드 처리
└── uploads/info/                      # 업로드된 이미지 저장 위치 (자동 생성)
```

## 🔄 데이터 흐름

### 약력 작성 플로우
```
1. 사용자: /info/write 접속 (로그인 필요)
   ↓
2. info_form.html: 섹션별 입력 폼 표시
   - 제목, 본문, 이미지 업로드만 입력
   - 작성자, 작성일 필드 없음
   ↓
3. JavaScript (FormData): 폼 제출
   - sections[0].sectionType = "PROFILE"
   - sections[0].title = "..."
   - sections[0].content = "..."
   - sections[0].imageFile = File 객체
   ↓
4. InfoController.saveSections():
   - CustomUserDetails에서 닉네임 추출
   - 각 섹션 파싱
   - 이미지가 있으면 uploadImage() 호출
   ↓
5. ContentInfoSectionService.uploadImage():
   - 파일을 uploads/info/UUID_timestamp.jpg로 저장
   - 웹 경로 반환 (/uploads/info/UUID_timestamp.jpg)
   ↓
6. ContentInfoSection 엔티티 생성:
   - writer: 닉네임 자동 설정
   - createdDate: @CreationTimestamp로 자동 생성
   - thumbnailUrl: 업로드된 이미지 경로 (또는 null)
   ↓
7. DB 저장 및 성공 응답
   ↓
8. 리다이렉트: /info (목록 페이지)
```

## 🎯 테스트 체크리스트

### 기능 테스트
- [ ] 로그인 후 /info/write 페이지 접근 가능
- [ ] 각 섹션(PROFILE, CHANNEL, ACTIVITY, WHY, SIGNATURE, SOURCES) 작성 가능
- [ ] 이미지 파일 업로드 정상 동작
- [ ] 작성 완료 후 /info 목록에서 확인 가능
- [ ] 작성자가 로그인한 사용자의 닉네임으로 표시
- [ ] 작성일이 현재 시간으로 자동 생성
- [ ] 이미지가 정상적으로 표시됨 (/uploads/info/... 경로)

### 보안 테스트
- [ ] 프론트엔드에서 작성자 필드 조작 불가 확인
- [ ] 프론트엔드에서 작성일 필드 조작 불가 확인
- [ ] /info 페이지에서 삭제 버튼이 표시되지 않음 (관리자 포함)
- [ ] /info/{id} 상세 페이지에서 삭제 버튼이 표시되지 않음 (관리자 포함)

### 에러 처리
- [ ] 로그인 없이 /info/write 접근 시 로그인 페이지로 리다이렉트
- [ ] 빈 폼 제출 시 프론트엔드 검증 (제목, 본문 필수)
- [ ] 이미지 업로드 실패 시 에러 메시지 표시
- [ ] 지원하지 않는 파일 형식 업로드 시 처리

## 📌 주의사항

### 1. 정적 리소스 설정 필요
업로드된 파일에 접근하려면 Spring Boot 설정 필요:

**`application.properties` 또는 `application.yml`:**
```properties
# 파일 업로드 설정
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# 정적 리소스 경로 설정
spring.web.resources.static-locations=classpath:/static/,file:uploads/
```

**또는 WebMvcConfigurer 사용:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

### 2. 파일 저장 위치
- 현재: 프로젝트 루트의 `uploads/info/` 디렉토리
- 배포 시: 절대 경로 또는 클라우드 스토리지(S3 등) 사용 권장

### 3. 파일 크기 제한
- 현재 설정: 파일당 10MB, 요청당 50MB
- 필요에 따라 `application.properties`에서 조정

### 4. 관리자 삭제 기능
- 현재: /info와 /info/{id}에서 삭제 버튼 완전 제거됨
- TODO: 별도 관리자 페이지(/admin/info 등)에서 삭제 기능 구현 필요

## 🚀 다음 단계

1. **정적 리소스 설정 추가** (application.properties 또는 WebConfig)
2. **관리자 페이지 구현** (InfoAdminController 활용)
3. **이미지 리사이징** (선택사항: 썸네일 자동 생성)
4. **파일 타입 검증** (이미지 파일만 허용)
5. **클라우드 스토리지 연동** (배포 시 AWS S3 등)

## 📝 요약

### 주요 개선점
1. ✅ **작성자 자동 주입**: 사용자 조작 불가, 보안 강화
2. ✅ **작성일 자동 생성**: 서버 시간 기준, 조작 불가
3. ✅ **파일 업로드**: 썸네일 URL 입력 → 실제 파일 업로드로 변경
4. ✅ **권한 분리**: 일반 사용자 페이지에서 삭제 기능 완전 제거
5. ✅ **Multipart 처리**: JSON → FormData 방식으로 전환

### 기술 스택
- **프론트엔드**: Thymeleaf, vanilla JavaScript (FormData API)
- **백엔드**: Spring Boot, Spring Security, JPA
- **파일 저장**: 로컬 파일 시스템 (java.nio.file)
- **인증**: CustomUserDetails (Spring Security)

---

**작성일**: 2025-12-16
**작성자**: Claude Sonnet 4.5