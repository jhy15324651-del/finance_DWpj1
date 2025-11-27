package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;

@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentPostController {

    private final ContentReviewService contentReviewService;

    /**
     * 🔥 게시글 상세 페이지
     *  - 목록에서 넘어온 page / keyword / searchType 정보를 그대로 받아서
     *    다시 목록으로 돌아갈 때 사용한다.
     */
    @GetMapping("/post/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hashtag") String searchType,
            Model model
    ) {
        log.debug("상세 페이지 요청: id={}, page={}, keyword={}, searchType={}",
                id, page, keyword, searchType);

        // 🔥 게시글 상세 조회 (조회수 증가까지 포함된 너의 서비스 메서드)
        ContentReview post = contentReviewService.getContentDetail(id);

        model.addAttribute("post", post);

        // 🔥 목록 복귀를 위한 정보 유지
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchType", searchType);

        return "content/post-detail";
    }
}
