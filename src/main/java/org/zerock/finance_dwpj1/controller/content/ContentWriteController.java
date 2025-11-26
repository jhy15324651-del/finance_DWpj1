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
 * 콘텐츠 리뷰 작성 및 저장 처리
 */
@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentWriteController {

    private final ContentReviewService contentReviewService;

    /**
     * 작성 폼 페이지
     */
    @GetMapping("/write")
    public String writeForm() {
        log.debug("콘텐츠 작성 폼 요청");
        return "content/write";
    }

    /**
     * 콘텐츠 저장 처리
     */
    @PostMapping("/write")
    public String writeContent(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {

        log.debug("콘텐츠 저장 요청: title={}, hashtags={}", title, hashtags);

        ContentReview post = ContentReview.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .hashtags(hashtags)  // 💡 category 대신 hashtags 입력
                .viewCount(0)
                .type("review")
                .isDeleted(false)
                .build();

        // 🔥 이미지 저장 처리
        if (image != null && !image.isEmpty()) {

            String uploadDir = "src/main/resources/static/upload/";
            Path uploadPath = Paths.get(uploadDir);

            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일명 생성
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // 이미지 저장
            Files.write(filePath, image.getBytes());
            post.setImgUrl("/upload/" + fileName);

            log.debug("이미지 저장 완료: {}", post.getImgUrl());
        }

        // DB 저장
        contentReviewService.saveContent(post);
        log.info("콘텐츠 저장 성공: id={}, title={}", post.getId(), post.getTitle());

        // 저장 후 목록으로 이동
        return "redirect:/content/category";
    }
}
