package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;

import java.util.List;

/**
 * 콘텐츠 홈 컨트롤러
 * 메인 페이지와 콘텐츠 상세 페이지 처리
 */
@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentHomeController {

    private final ContentReviewService contentReviewService;

    /**
     * 콘텐츠 홈 화면
     */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        log.debug("콘텐츠 홈 화면 요청");

        List<ContentReview> latestContents = contentReviewService.getLatestContents();
        List<ContentReview> popularContents = contentReviewService.getPopularContents();

        model.addAttribute("latestPosts", latestContents);
        model.addAttribute("popularPosts", popularContents);

        return "content/home";
    }
}
