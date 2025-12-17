package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

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

    /* ============================================================
       1) 작성 폼
    ============================================================ */
    @GetMapping("/write")
    public String writeForm() {
        return "content/write";
    }

    /* ============================================================
       2) 게시글 저장
    ============================================================ */
    @PostMapping("/write")
    public String writeContent(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails loginUser,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        if (loginUser == null) {
            return "redirect:/user/login";
        }

        ContentReview post = ContentReview.builder()
                .title(title)
                .content(content)
                .hashtags(hashtags)
                .userId(loginUser.getId())
                .writer(loginUser.getNickname())
                .viewCount(0)
                .type("review")
                .isDeleted(false)
                .build();

        if (image != null && !image.isEmpty()) {
            String uploadDir = "src/main/resources/static/upload/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, image.getBytes());
            post.setImgUrl("/upload/" + fileName);
        }

        try {
            contentReviewService.saveContent(post);
        } catch (IllegalArgumentException e) {
            // ⭐ alert로 띄울 메시지 전달
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // ⭐ 다시 글쓰기 페이지로
            return "redirect:/content/write";
        }

        return "redirect:/content/category";
    }


    /* ============================================================
       3) 수정 폼  (삭제 여부 상관없이 읽기)
    ============================================================ */
    @GetMapping("/edit/{id}")
    public String editForm(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            Model model) {

        ContentReview post = contentReviewService.getContentById(id);

        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        model.addAttribute("post", post);
        return "content/edit";
    }

    /* ============================================================
       4) 수정 저장
    ============================================================ */
    @PostMapping("/edit/{id}")
    public String editContent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {

        ContentReview post = contentReviewService.getContentById(id);

        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        contentReviewService.updateContent(id, title, content, hashtags, image);
        return "redirect:/content/post/" + id;
    }

    /* ============================================================
       5) 게시글 삭제 (소프트 삭제)
    ============================================================ */
    @GetMapping("/delete/{id}")
    public String deleteContent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        ContentReview post = contentReviewService.getContentDetail(id);

        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        contentReviewService.deleteContent(id);
        return "redirect:/content/category";
    }

    /* ============================================================
       6) 리포스트 전용 페이지 (삭제된 글 클릭 시)
    ============================================================ */
    @GetMapping("/restore-page/{id}")
    public String restorePage(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            Model model
    ) {
        ContentReview post = contentReviewService.getContentById(id);

        // 내 글 아니면 접근 불가
        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        model.addAttribute("post", post);
        // ==> templates/content/restore-page.html
        return "content/restore-page";
    }

    /* ============================================================
       7) 실제 복구 동작 (POST)
    ============================================================ */
    @PostMapping("/restore/{id}")
    public String restorePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        ContentReview post = contentReviewService.getContentById(id);

        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        contentReviewService.restoreContent(id);

        // 요구사항: 복구 후 "내가 작성한 게시글" 화면으로
        return "redirect:/user/mypage/posts";
    }
}
