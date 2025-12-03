package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    /** 작성 폼 */
    @GetMapping("/write")
    public String writeForm() {
        return "content/write";
    }

    /** 게시글 저장 */
    @PostMapping("/write")
    public String writeContent(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) throws IOException {

        // 로그인 체크
        if (loginUser == null) {
            return "redirect:/user/login";
        }

        // 게시글 엔티티 생성
        ContentReview post = ContentReview.builder()
                .title(title)
                .content(content)
                .hashtags(hashtags)
                .userId(loginUser.getId())           // 작성자 ID
                .writer(loginUser.getNickname())     // 작성자 닉네임
                .viewCount(0)
                .type("review")
                .isDeleted(false)
                .build();

        // 이미지 업로드
        if (image != null && !image.isEmpty()) {

            String uploadDir = "src/main/resources/static/upload/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.write(filePath, image.getBytes());
            post.setImgUrl("/upload/" + fileName);
        }

        // DB 저장
        contentReviewService.saveContent(post);

        return "redirect:/content/category";
    }

    /** 수정 폼 */
    @GetMapping("/edit/{id}")
    public String editForm(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            Model model) {

        ContentReview post = contentReviewService.getContentDetail(id);

        // 작성자 확인
        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        model.addAttribute("post", post);
        return "content/edit";
    }

    /** 수정 저장 */
    @PostMapping("/edit/{id}")
    public String editContent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String hashtags,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {

        ContentReview post = contentReviewService.getContentDetail(id);

        // 권한 체크
        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id;
        }

        // 서비스에서 업데이트 처리
        contentReviewService.updateContent(id, title, content, hashtags, image);

        return "redirect:/content/post/" + id;
    }

    /** 게시글 삭제 */
    @GetMapping("/delete/{id}")
    public String deleteContent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        ContentReview post = contentReviewService.getContentDetail(id);

        // 🔐 권한 체크: 작성자만 삭제 가능
        if (user == null || !post.getWriter().equals(user.getNickname())) {
            return "redirect:/content/post/" + id; // 권한 없음 → 상세페이지로
        }

        // 🔥 소프트 삭제 처리 (isDeleted = true)
        contentReviewService.deleteContent(id);

        return "redirect:/content/category";
    }

}
