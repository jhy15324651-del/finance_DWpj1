package org.zerock.finance_dwpj1.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;
import org.zerock.finance_dwpj1.service.content.InfoPostService;

@RequiredArgsConstructor
@Controller
public class UserInfoDetailController {

    private final InfoPostService infoPostService;
    private final ContentReviewService contentReviewService;

    @GetMapping("/user/{writer}")
    public String userDetail(
            @PathVariable String writer,
            Model model
    ) {
        model.addAttribute("writer", writer);

        // 프로필 카드
        infoPostService.getLatestProfileByWriter(writer)
                .ifPresent(profile ->
                        model.addAttribute("profile", profile));

        // 작성한 콘텐츠 목록
        model.addAttribute(
                "reviews",
                contentReviewService.getReviewsByWriter(writer)
        );

        return "user/user-detail";
    }
}


