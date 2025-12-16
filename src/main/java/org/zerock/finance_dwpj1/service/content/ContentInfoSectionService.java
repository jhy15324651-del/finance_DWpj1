package org.zerock.finance_dwpj1.service.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.ContentInfoSection;
import org.zerock.finance_dwpj1.repository.content.ContentInfoSectionRepository;

import java.io.File;
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
 * ContentInfoSection Service
 *
 * 목적:
 * - info.html 썸네일 목록 페이지용 섹션 관리
 * - 일반 사용자: 활성 섹션만 조회
 * - 관리자: 소프트 삭제 및 복구 가능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContentInfoSectionService {

    private final ContentInfoSectionRepository repository;

    /**
     * @deprecated InfoPostService 사용을 권장합니다.
     * 이 서비스는 구조 변경으로 더 이상 사용되지 않습니다.
     */
    @Deprecated
    public List<ContentInfoSection> getActiveSections() {
        log.warn("⚠️ Deprecated: ContentInfoSectionService.getActiveSections() 대신 InfoPostService 사용을 권장합니다");
        return repository.findAll();
    }

    /**
     * @deprecated InfoPostService 사용을 권장합니다.
     */
    @Deprecated
    public Optional<ContentInfoSection> getActiveSectionById(Long id) {
        log.warn("⚠️ Deprecated: ContentInfoSectionService 대신 InfoPostService 사용을 권장합니다");
        return repository.findById(id);

    }

    /**
     * @deprecated InfoPostService 사용을 권장합니다.
     */
    @Deprecated
    @Transactional
    public ContentInfoSection saveSection(ContentInfoSection section) {
        log.warn("⚠️ Deprecated: ContentInfoSectionService 대신 InfoPostService 사용을 권장합니다");
        return repository.save(section);
    }

    /**
     * 이미지 파일 업로드
     *
     * @param file 업로드할 이미지 파일
     * @return 저장된 파일의 웹 접근 경로 (예: /uploads/info/uuid.jpg)
     * @throws RuntimeException 파일 업로드 실패 시
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        try {
            // 파일 저장 디렉토리 설정 (프로젝트 루트/uploads/info)
            String uploadDir = "uploads/info";
            Path uploadPath = Paths.get(uploadDir);

            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("📁 업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
            }

            // 원본 파일명과 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // UUID + 타임스탬프로 고유한 파일명 생성
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String uniqueFilename = UUID.randomUUID().toString() + "_" + timestamp + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 웹 접근 경로 반환 (정적 리소스로 접근 가능한 경로)
            String webPath = "/" + uploadDir + "/" + uniqueFilename;

            log.info("📸 이미지 업로드 완료: {} -> {}", originalFilename, webPath);

            return webPath;

        } catch (IOException e) {
            log.error("❌ 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * @deprecated 구조 변경으로 섹션 단위 삭제는 지원하지 않습니다.
     * InfoPost 삭제 시 cascade로 섹션이 자동 삭제됩니다.
     */
    @Deprecated
    @Transactional
    public boolean softDeleteSection(Long id, String adminId) {
        log.warn("⚠️ Deprecated: 섹션 단위 삭제는 지원하지 않습니다. InfoPostService를 사용하세요.");
        return false;
    }

    /**
     * @deprecated 구조 변경으로 더 이상 사용되지 않습니다.
     */
    @Deprecated
    @Transactional
    public boolean restoreSection(Long id) {
        log.warn("⚠️ Deprecated: InfoPostService를 사용하세요.");
        return false;
    }

    /**
     * @deprecated 구조 변경으로 더 이상 사용되지 않습니다.
     */
    @Deprecated
    public boolean existsActiveSection(String sectionType) {
        log.warn("⚠️ Deprecated: InfoPostService를 사용하세요.");
        return false;
    }

    /**
     * @deprecated 구조 변경으로 더 이상 사용되지 않습니다.
     * InfoPost를 통해 섹션을 생성하세요.
     */
    @Deprecated
    @Transactional
    public void initializeDefaultSections() {
        log.warn("⚠️ Deprecated: 구조가 변경되어 이 메서드는 더 이상 사용되지 않습니다.");
    }
}