package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.content.ContentCommentNodeDTO;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentCommentNodeService;
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
    private final ContentCommentNodeService contentCommentNodeService;

    @GetMapping("/post/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hashtag") String searchType,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        ContentReview post = contentReviewService.getContentDetail(id);
        model.addAttribute("post", post);

        // 기존 댓글 목록 제거
        model.addAttribute("comments", contentCommentService.getComments(id));

        // 🔥 트리댓글로 변경
        //model.addAttribute("commentTree", contentCommentNodeService.getCommentTree(id));


        // 평점 모델에 담아 뷰로 전달하기
        double avgRating = contentCommentService.getAverageRating(id);
        int ratingCount = contentCommentService.getRatingCount(id);

        model.addAttribute("avgRating", avgRating);
        model.addAttribute("ratingCount", ratingCount);


        // 로그인 사용자 정보 전달
        if (userDetails != null) {
            model.addAttribute("nickname", userDetails.getNickname());
            model.addAttribute("userId", userDetails.getId());
        }

        // 목록 복귀 정보
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
