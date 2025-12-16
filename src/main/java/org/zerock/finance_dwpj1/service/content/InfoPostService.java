package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.ContentInfoSection;
import org.zerock.finance_dwpj1.entity.content.InfoPost;
import org.zerock.finance_dwpj1.repository.content.InfoPostRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * @param writer 작성자 닉네임
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
     * @param id 삭제할 게시글 ID
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
     * 이미지 파일 업로드
     *
     * @param file 업로드할 이미지 파일
     * @return 저장된 파일의 웹 접근 경로
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        try {
            String uploadDir = "uploads/info";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("📁 업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String uniqueFilename = UUID.randomUUID().toString() + "_" + timestamp + extension;

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String webPath = "/" + uploadDir + "/" + uniqueFilename;
            log.info("📸 이미지 업로드 완료: {} -> {}", originalFilename, webPath);

            return webPath;

        } catch (IOException e) {
            log.error("❌ 이미지 업로드 실패: {}", e.getMessage(), e);
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