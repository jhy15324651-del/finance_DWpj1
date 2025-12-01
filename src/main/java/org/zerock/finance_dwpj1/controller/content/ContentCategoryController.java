package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.content.ContentReviewService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentCategoryController {

    private final ContentReviewService contentReviewService;

    /**
     * 🔥 카테고리 + 검색 + 다중 해시태그 + 페이징
     */
    @GetMapping("/category")
    public String categoryPage(
            @RequestParam(defaultValue = "hashtag") String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            @AuthenticationPrincipal CustomUserDetails loginUser  // 🔥 로그인 사용자 정보 추가
    ) {

        log.debug("카테고리 요청: type={}, keyword={}, page={}", searchType, keyword, page);

        int pageSize = 15;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdDate").descending());

        Page<ContentReview> posts;

        // 🔥 검색어 null → "" 처리
        if (keyword == null) keyword = "";

        // 🔥 다중 태그 추출 (#테슬라 #엔비디아 ...)
        Set<String> tagSet = Arrays.stream(keyword.split(" "))
                .map(String::trim)
                .filter(t -> t.length() > 0)   // 빈 문자열 제거
                .collect(Collectors.toSet());

        // -----------------------------------------------------------
        // 🔥 검색 로직
        // -----------------------------------------------------------
        switch (searchType) {

            case "title":
                posts = contentReviewService.searchByTitle(keyword, pageable);
                break;

            case "content":
                posts = contentReviewService.searchByContent(keyword, pageable);
                break;

            default:
                // 🔥 다중 해시태그 AND 검색 (핵심 기능)
                posts = contentReviewService.searchByMultipleTags(tagSet, pageable);
                break;
        }

        // -----------------------------------------------------------
        // 🔥 블록 페이징
        // -----------------------------------------------------------
        int totalPages = posts.getTotalPages();
        int currentPage = posts.getNumber();

        int blockSize = 10;
        int blockStart = (currentPage / blockSize) * blockSize;
        int blockEnd = Math.min(blockStart + blockSize - 1, totalPages - 1);

        // -----------------------------------------------------------
        // 🔥 로그인 사용자 닉네임 전달
        // -----------------------------------------------------------
        if (loginUser != null) {
            model.addAttribute("nickname", loginUser.getNickname());
        } else {
            model.addAttribute("nickname", null);
        }

        // -----------------------------------------------------------
        // 🔥 모델 전달
        // -----------------------------------------------------------
        model.addAttribute("posts", posts);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("blockStart", blockStart);
        model.addAttribute("blockEnd", blockEnd);
        model.addAttribute("prevBlock", blockStart - 1);
        model.addAttribute("nextBlock", blockEnd + 1);

        return "content/category";
    }
}
