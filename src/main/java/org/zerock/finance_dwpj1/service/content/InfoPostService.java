package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.ContentInfoSection;
import org.zerock.finance_dwpj1.entity.content.InfoPost;
import org.zerock.finance_dwpj1.repository.content.InfoPostRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * InfoPost Service
 *
 * 약력 게시글(글 1개 안에 여러 섹션) 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InfoPostService {

    private final InfoPostRepository repository;

    public Optional<InfoPost> getLatestProfileByWriter(String writer) {
        return repository
                .findTopByWriterAndIsDeletedFalseOrderByCreatedDateDesc(writer);
    }

    // application.properties에서 정보 탭 전용 업로드 경로 주입
    @Value("${file.info-upload-path}")
    private String infoUploadPath;

    /**
     * 활성 게시글 목록 조회 (일반 사용자용)
     * /info 목록 페이지에 표시
     *
     * @return 삭제되지 않은 게시글 목록
     */
    public List<InfoPost> getActivePosts() {
        return repository.findActivePostsWithSections();
    }

    /**
     * ID로 활성 게시글 조회 (섹션 포함)
     * /info/{id} 상세 페이지용
     *
     * @param id 게시글 ID
     * @return 게시글 (섹션 포함)
     */
    public Optional<InfoPost> getActivePostWithSections(Long id) {
        return repository.findActivePostWithSections(id);
    }

    /**
     * 모든 게시글 조회 (관리자용)
     *
     * @return 전체 게시글 목록 (삭제된 것 포함)
     */
    public List<InfoPost> getAllPostsForAdmin() {
        return repository.findAllPostsWithSections();
    }

    /**
     * 게시글 저장 (여러 섹션 포함)
     *
     * @param writer       작성자 닉네임
     * @param sectionsData 섹션 데이터 리스트 (타입, 제목, 본문, 이미지파일)
     * @return 저장된 게시글
     */
    @Transactional
    public InfoPost savePost(String writer, List<SectionData> sectionsData) {
        log.info("💾 약력 게시글 저장 시작: 작성자={}, 섹션 수={}", writer, sectionsData.size());

        // 1. PROFILE 섹션에서 대표 정보 추출
        SectionData profileData = sectionsData.stream()
                .filter(s -> "PROFILE".equals(s.getSectionType()))
                .findFirst()
                .orElse(sectionsData.get(0)); // PROFILE 없으면 첫 번째 섹션 사용

        // 2. PROFILE 이미지 업로드 (있는 경우)
        String thumbnailUrl = null;
        if (profileData.getImageFile() != null && !profileData.getImageFile().isEmpty()) {
            thumbnailUrl = uploadImage(profileData.getImageFile());
        }

        // 3. InfoPost 생성
        InfoPost post = InfoPost.builder()
                .title(profileData.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .writer(writer)
                .isDeleted(false)
                .build();

        // 4. 섹션 추가
        int order = 1;
        for (SectionData sectionData : sectionsData) {
            // 섹션별 이미지 업로드
            String sectionImageUrl = null;
            if (sectionData.getImageFile() != null && !sectionData.getImageFile().isEmpty()) {
                sectionImageUrl = uploadImage(sectionData.getImageFile());
            }

            ContentInfoSection section = ContentInfoSection.builder()
                    .sectionType(sectionData.getSectionType())
                    .title(sectionData.getTitle())
                    .content(sectionData.getContent())
                    .thumbnailUrl(sectionImageUrl)
                    .displayOrder(order++)
                    .build();

            post.addSection(section);
            log.info("  ✅ 섹션 추가: {}", sectionData.getSectionType());
        }

        // 5. 저장
        InfoPost savedPost = repository.save(post);
        log.info("🎉 약력 게시글 저장 완료: ID={}, 섹션 수={}", savedPost.getId(), savedPost.getSections().size());

        return savedPost;
    }

    /**
     * 게시글 소프트 삭제 (관리자용)
     *
     * @param id      삭제할 게시글 ID
     * @param adminId 삭제를 요청한 관리자 ID
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean softDeletePost(Long id, String adminId) {
        Optional<InfoPost> postOpt = repository.findById(id);

        if (postOpt.isEmpty()) {
            log.warn("⚠️ 게시글을 찾을 수 없음: ID={}", id);
            return false;
        }

        InfoPost post = postOpt.get();

        if (post.getIsDeleted()) {
            log.warn("⚠️ 이미 삭제된 게시글: ID={}", id);
            return false;
        }

        post.softDelete(adminId);
        repository.save(post);

        log.info("🗑️ 게시글 삭제됨: ID={} by {}", id, adminId);
        return true;
    }

    /**
     * 게시글 복구 (관리자용)
     *
     * @param id 복구할 게시글 ID
     * @return 복구 성공 여부
     */
    @Transactional
    public boolean restorePost(Long id) {
        Optional<InfoPost> postOpt = repository.findById(id);

        if (postOpt.isEmpty()) {
            log.warn("⚠️ 게시글을 찾을 수 없음: ID={}", id);
            return false;
        }

        InfoPost post = postOpt.get();

        if (!post.getIsDeleted()) {
            log.warn("⚠️ 삭제되지 않은 게시글: ID={}", id);
            return false;
        }

        post.restore();
        repository.save(post);

        log.info("♻️ 게시글 복구됨: ID={}", id);
        return true;
    }

    /**
     * 이미지 파일 업로드 (콘텐츠리뷰 패턴 적용)
     * <p>
     * ⭐ 저장 방식:
     * - 실제 저장: C:/info_uploads/2025-12-16/UUID.jpg
     * - DB 저장: /info_uploads/2025-12-16/UUID.jpg (상대 URL)
     * - 날짜별 폴더 자동 생성
     * - UUID 파일명으로 중복 방지
     * <p>
     * ⭐ transferTo() 대신 getBytes() 사용 이유:
     * - MultipartFile의 임시 파일은 한 번만 transferTo() 가능
     * - 여러 섹션에서 동시에 파일 업로드 시 임시 파일 삭제 문제 방지
     * - getBytes()는 즉시 메모리에 읽어서 임시 파일 의존성 제거
     *
     * @param file 업로드할 이미지 파일
     * @return 웹 접근 URL (/info_uploads/날짜/파일명)
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 없습니다.");
        }

        try {
            // 1️⃣ 날짜별 폴더 생성 (콘텐츠리뷰와 동일 패턴)
            String dateFolder = LocalDate.now().toString(); // 예: 2025-12-16
            Path uploadDirPath = Paths.get(infoUploadPath, dateFolder);

            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
                log.info("📁 Info 업로드 디렉토리 생성: {}", uploadDirPath.toAbsolutePath());
            }

            // 2️⃣ 확장자 추출
            String originalName = file.getOriginalFilename();
            String ext = "";

            if (originalName != null && originalName.lastIndexOf(".") != -1) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            // 3️⃣ UUID 파일명 생성 (중복 방지)
            String savedFileName = UUID.randomUUID().toString() + ext;

            // 4️⃣ 파일을 바이트 배열로 읽기 (임시 파일 의존성 제거)
            byte[] fileBytes = file.getBytes();

            // 5️⃣ 실제 디스크에 저장 (Files.write 사용)
            Path filePath = uploadDirPath.resolve(savedFileName);
            Files.write(filePath, fileBytes);

            // 6️⃣ 웹 접근 URL 반환 (DB 저장용)
            String imageUrl = "/info_uploads/" + dateFolder + "/" + savedFileName;

            log.info("📸 Info 이미지 업로드 완료: {} -> {}", originalName, imageUrl);
            log.info("   실제 저장 위치: {}", filePath.toAbsolutePath());
            log.info("   파일 크기: {} bytes", fileBytes.length);

            return imageUrl;

        } catch (IOException e) {
            log.error("❌ Info 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }


    /**
     * 섹션 데이터 DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SectionData {
        private String sectionType;
        private String title;
        private String content;
        private MultipartFile imageFile;
    }
}

