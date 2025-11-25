package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 콘텐츠 작성 컨트롤러
 * 콘텐츠 작성 폼 및 저장 처리
 */
@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentWriteController {

    private final ContentReviewService contentReviewService;

    /**
     * 콘텐츠 작성 폼
     */
    @GetMapping("/write")
    public String writeForm() {
        log.debug("콘텐츠 작성 폼 요청");
        return "content/write";
    }

    /**
     * 콘텐츠 저장
     */
    @PostMapping("/write")
    public String writeContent(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam String category,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {
        log.debug("콘텐츠 저장 요청: title={}, category={}", title, category);

        ContentReview contentReview = ContentReview.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .category(category)
                .viewCount(0)
                .type("review")
                .isDeleted(false)
                .build();

        // 이미지 저장
        if (image != null && !image.isEmpty()) {
            String uploadDir = "src/main/resources/static/upload/";

            // 디렉토리가 없으면 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, image.getBytes());

            contentReview.setImgUrl("/upload/" + fileName);
            log.debug("이미지 저장 완료: {}", fileName);
        }

        contentReviewService.saveContent(contentReview);
        log.info("콘텐츠 저장 완료: id={}, title={}", contentReview.getId(), contentReview.getTitle());

        return "redirect:/content/category";
    }
}
