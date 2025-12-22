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
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            Model model
    ) {

        ContentReview post = ContentReview.builder()
                .title(title)
                .content(content)
                .hashtags(hashtags)
                .build();

        try {
            contentReviewService.saveContent(post, user);
        } catch (IllegalArgumentException e) {

            // 🔥 이 두 줄이 없으면 alert 절대 안 뜸
            model.addAttribute("post", post);
            model.addAttribute("errorMessage", e.getMessage());

            return "content/write"; // ❌ redirect 아님
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
            @RequestParam(required = false) MultipartFile image,
            Model model
    ) {

        ContentReview post = contentReviewService.getContentById(id);

        try {
            contentReviewService.updateContent(
                    id, title, content, hashtags, image, user
            );
        } catch (IllegalArgumentException e) {

            post.setTitle(title);
            post.setContent(content);
            post.setHashtags(hashtags);

            model.addAttribute("post", post);
            model.addAttribute("errorMessage", e.getMessage());

            return "content/edit";

        } catch (IOException e) {
            // 🔥 파일 업로드 중 오류
            model.addAttribute("post", post);
            model.addAttribute("errorMessage", "이미지 처리 중 오류가 발생했습니다.");

            return "content/edit";
        }

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
