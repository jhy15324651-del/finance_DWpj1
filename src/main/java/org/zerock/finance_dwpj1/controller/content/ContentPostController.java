package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;
import org.zerock.finance_dwpj1.service.content.ContentCommentService;
import org.zerock.finance_dwpj1.dto.content.ContentCommentWriteDTO;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentPostController {

    private final ContentReviewService contentReviewService;
    private final ContentCommentService contentCommentService;

    @GetMapping("/post/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hashtag") String searchType,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        // 🔥 삭제 여부 무시하고 조회
        ContentReview post = contentReviewService.getContentById(id);

        // 🔥 삭제된 글이면 post-detail 접속 불가 → 리포스트 화면으로 보냄
        if (post.getIsDeleted()) {
            return "redirect:/content/restore-page/" + id;
        }

        // 🔥 조회수 증가 포함된 상세조회는 삭제된 글만 피하고 호출
        post = contentReviewService.getContentDetail(id);  // isDeleted = false인 경우만 정상 호출됨
        model.addAttribute("post", post);

        model.addAttribute("comments", contentCommentService.getComments(id));

        double avgRating = contentCommentService.getAverageRating(id);
        int ratingCount = contentCommentService.getRatingCount(id);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("ratingCount", ratingCount);

        if (userDetails != null) {
            model.addAttribute("nickname", userDetails.getNickname());
            model.addAttribute("userId", userDetails.getId());
        }

        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchType", searchType);

        return "content/post-detail";
    }


    @PostMapping("/comment")
    @ResponseBody
    public String writeComment(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ContentCommentWriteDTO dto
    ) {

        if (user == null) {
            return "NOT_LOGIN";
        }

        try {
            contentCommentService.write(user.getId(), user.getNickname(), dto);
            return "SUCCESS";
        } catch (IllegalArgumentException e) {
            return "NO_RATING";  // 평점 없음
        }
    }
}
