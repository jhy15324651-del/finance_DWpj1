package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;   // 🔵 추가
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;           // 🔵 추가

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            @RequestParam(required = false) MultipartFile image,

            @AuthenticationPrincipal CustomUserDetails loginUser  // 🔵 로그인 사용자 정보 받기
    ) throws IOException {

        // 🔒 로그인 안된 사용자는 작성 불가
        if (loginUser == null) {
            log.warn("비로그인 사용자의 게시글 작성 시도");
            return "redirect:/user/login";
        }

        log.debug("콘텐츠 저장 요청: title={}, hashtags={}", title, hashtags);

        // 🆕 작성자 정보 포함해서 엔티티 생성
        ContentReview post = ContentReview.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .hashtags(hashtags)
                .userId(loginUser.getId())        // 🔵 추가
                .writer(loginUser.getNickname())  // 🔵 추가
                .viewCount(0)
                .type("review")
                .isDeleted(false)
                .build();

        // 🔥 이미지 저장 처리
        if (image != null && !image.isEmpty()) {

            String uploadDir = "src/main/resources/static/upload/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.write(filePath, image.getBytes());
            post.setImgUrl("/upload/" + fileName);

            log.debug("이미지 저장 완료: {}", post.getImgUrl());
        }

        // DB 저장
        contentReviewService.saveContent(post);
        log.info("콘텐츠 저장 성공: id={}, title={}, userId={}, writer={}",
                post.getId(), post.getTitle(), post.getUserId(), post.getWriter());

        return "redirect:/content/category";
    }
}
