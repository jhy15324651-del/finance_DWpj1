package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final PostRepository postRepository;

    /** 카테고리별 게시글 수 추가 */
    private void addCategoryCounts(Model model) {
        model.addAttribute("totalCount", postRepository.count());
        model.addAttribute("noticeCount", postRepository.countByCategory("공지"));
        model.addAttribute("macroCount", postRepository.countByCategory("거시경제"));
        model.addAttribute("oilCount", postRepository.countByCategory("원유"));
        model.addAttribute("nvidiaCount", postRepository.countByCategory("엔비디아"));
        model.addAttribute("teslaCount", postRepository.countByCategory("테슬라"));
    }

    /** 카테고리 + 페이징 */
    @GetMapping("/category")
    public String categoryPage(
            @RequestParam(defaultValue = "전체") String category,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        int pageSize = 15; // ⭐ 한 페이지당 15개
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<Post> posts;

        if (category.equals("전체")) {
            posts = postRepository.findAll(pageable);
        } else {
            posts = postRepository.findByCategory(category, pageable);
        }

        int totalPages = posts.getTotalPages();
        int currentPage = posts.getNumber(); // 0-based index

        // ⭐ 블록 페이징 계산
        int blockSize = 10;  // <<<<< 페이징 버튼 10개 단위
        int blockStart = (currentPage / blockSize) * blockSize;    // 현재 블록 시작 페이지
        int blockEnd = Math.min(blockStart + blockSize - 1, totalPages - 1); // 끝 페이지

        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);

        // ⏮ 처음/⏭ 마지막/◀ 이전/▶ 다음에 필요한 값 추가
        model.addAttribute("blockStart", blockStart);
        model.addAttribute("blockEnd", blockEnd);
        model.addAttribute("prevBlock", blockStart - 1);
        model.addAttribute("nextBlock", blockEnd + 1);

        model.addAttribute("activeCategory", category);

        addCategoryCounts(model);

        return "category";
    }

}
