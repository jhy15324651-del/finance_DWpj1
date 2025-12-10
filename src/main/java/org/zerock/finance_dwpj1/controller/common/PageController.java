package org.zerock.finance_dwpj1.controller.common;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.zerock.finance_dwpj1.dto.insights.InsightsDailyNewsDTO;
import org.zerock.finance_dwpj1.dto.user.UserCommentDTO;
import org.zerock.finance_dwpj1.dto.user.UserPostDTO;
import org.zerock.finance_dwpj1.entity.content.ContentComment;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.entity.stock.StockBoard;
import org.zerock.finance_dwpj1.entity.stock.StockComment;
import org.zerock.finance_dwpj1.entity.insights.InsightsComment;
import org.zerock.finance_dwpj1.entity.insights.InsightsNews;
import org.zerock.finance_dwpj1.repository.content.ContentCommentRepository;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;
import org.zerock.finance_dwpj1.repository.insights.InsightsCommentRepository;
import org.zerock.finance_dwpj1.repository.insights.InsightsNewsRepository;
import org.zerock.finance_dwpj1.repository.stock.StockBoardRepository;
import org.zerock.finance_dwpj1.repository.stock.StockCommentRepository;
import org.zerock.finance_dwpj1.service.insights.DailyNewsService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final DailyNewsService dailyNewsService;
    private final ContentReviewRepository contentReviewRepository;
    private final ContentCommentRepository contentCommentRepository;
    private final StockBoardRepository stockBoardRepository;
    private final StockCommentRepository stockCommentRepository;
    private final InsightsCommentRepository insightsCommentRepository;
    private final InsightsNewsRepository insightsNewsRepository;


    @GetMapping("/")
    public String index(Model model) {
        // 금주의 인기 뉴스 5개 조회
        List<InsightsDailyNewsDTO> topNews = dailyNewsService.getWeeklyTopNews();

        // 상위 5개만 전달
        List<InsightsDailyNewsDTO> top5News = topNews.size() > 5
            ? topNews.subList(0, 5)
            : topNews;

        model.addAttribute("popularNews", top5News);

        return "index";  // templates/index.html
    }

    @GetMapping("/news")
    public String newsInsights() {
        return "insights/news-insights";
    }

    @GetMapping("/portfolio")
    public String portfolioComparison() {
        return "portfolio/portfolio-comparison";
    }

    @GetMapping("/portfolio/analyzer")
    public String portfolioAnalyzer() {
        return "portfolio/portfolio-analyzer";
    }

    @GetMapping("/user/mypage")
    public String myPage() {
        return "user/mypage";
    }

    //마이페이지 내 게시글
    @GetMapping("/user/mypage/posts")
    public String myPostsPage(Model model,
                              @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) return "redirect:/login";

        String nickname = user.getNickname();

        // 1) 콘텐츠 리뷰 게시글 조회
        List<ContentReview> reviewPosts =
                contentReviewRepository.findByWriter(nickname);

        // 2) 종목 토론방 게시글 조회
        List<StockBoard> stockPosts =
                stockBoardRepository.findByWriter(nickname);

        // 3) 통합 DTO 리스트 생성
        List<UserPostDTO> results = new ArrayList<>();

        // 콘텐츠 리뷰 → DTO
        for (ContentReview r : reviewPosts) {
            results.add(UserPostDTO.builder()
                    .id(r.getId())
                    .title(r.getTitle())
                    .date(r.getCreatedDate())
                    .viewCount(r.getViewCount())
                    .category("콘텐츠 리뷰")
                    .link("/content/post/" + r.getId())
                    .isDeleted(r.getIsDeleted())   // ⭐ 추가!
                    .build());
        }

        // 종목 토론방 → DTO
        for (StockBoard s : stockPosts) {
            results.add(UserPostDTO.builder()
                    .id(s.getId())
                    .title(s.getTitle())
                    .date(s.getRegDate())
                    .viewCount(s.getView())
                    .category("종목 토론방")
                    .link("/stock/board/" + s.getTicker() + "/read/" + s.getId())
                    .isDeleted(false)   // ⭐ 추가!
                    .build());
        }

        // 4) 날짜 기준으로 최신순 정렬
        results.sort(Comparator.comparing(UserPostDTO::getDate).reversed());

        // 5) 모델에 추가
        model.addAttribute("posts", results);

        return "user/mypage/my-posts";
    }


    // 마이페이지 내 댓글
    @GetMapping("/user/mypage/comments")
    public String myCommentsPage(Model model,
                                 @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) return "redirect:/login";

        String nickname = user.getNickname();

        // 최종 결과 DTO 리스트
        List<UserCommentDTO> results = new ArrayList<>();

    /* --------------------------------------------
       1) 콘텐츠 리뷰 댓글 가져오기
    --------------------------------------------- */
        List<ContentComment> reviewComments =
                contentCommentRepository.findByWriter(nickname);

        for (ContentComment c : reviewComments) {

            Long postId = c.getPostId();

            ContentReview review = contentReviewRepository.findById(postId).orElse(null);

            if (review != null && !review.getIsDeleted()) {

                results.add(UserCommentDTO.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .date(c.getCreatedDate())
                        .category("콘텐츠 리뷰")
                        .postTitle(review.getTitle())
                        .postLink("/content/post/" + review.getId())
                        .build());

                continue;
            }

            // 리뷰 게시글이 삭제된 경우
            results.add(UserCommentDTO.builder()
                    .id(c.getId())
                    .content(c.getContent())
                    .date(c.getCreatedDate())
                    .category("[삭제된 게시글]")
                    .postTitle("(삭제된 게시글)")
                    .postLink(null)
                    .build());
        }


    /* --------------------------------------------
       2) 종목 토론방 댓글 가져오기
    --------------------------------------------- */
        List<StockComment> stockComments =
                stockCommentRepository.findByWriter(nickname);

        for (StockComment sc : stockComments) {

            StockBoard board = sc.getBoard();  // ManyToOne 관계라 자동 로딩됨

            if (board != null) {
                results.add(UserCommentDTO.builder()
                        .id(sc.getId())
                        .content(sc.getContent())
                        .date(sc.getRegDate())
                        .category("종목 토론방")
                        .postTitle(board.getTitle())
                        .postLink("/stock/board/" + board.getTicker() + "/read/" + board.getId())
                        .build());

            } else {
                // 게시글 자체가 사라진 경우
                results.add(UserCommentDTO.builder()
                        .id(sc.getId())
                        .content(sc.getContent())
                        .date(sc.getRegDate())
                        .category("[삭제된 게시글]")
                        .postTitle("(삭제된 게시글)")
                        .postLink(null)
                        .build());
            }
        }


    /* --------------------------------------------
       3) 뉴스 인사이트 댓글 가져오기
    --------------------------------------------- */
        List<InsightsComment> newsComments =
                insightsCommentRepository.findByUserName(nickname);

        for (InsightsComment nc : newsComments) {

            InsightsNews news = nc.getNews();  // ManyToOne 관계라 자동 로딩됨

            if (news != null && !news.getIsDeleted()) {
                results.add(UserCommentDTO.builder()
                        .id(nc.getId())
                        .content(nc.getContent())
                        .date(nc.getCreatedAt())
                        .category("뉴스 인사이트")
                        .postTitle(news.getTitle())
                        .postLink("/news")  // 뉴스 인사이트 페이지로 이동
                        .build());

            } else {
                // 뉴스가 삭제된 경우
                results.add(UserCommentDTO.builder()
                        .id(nc.getId())
                        .content(nc.getContent())
                        .date(nc.getCreatedAt())
                        .category("[삭제된 게시글]")
                        .postTitle("(삭제된 게시글)")
                        .postLink(null)
                        .build());
            }
        }


    /* --------------------------------------------
       4) 날짜 기준 최신순 정렬
    --------------------------------------------- */
        results.sort(Comparator.comparing(UserCommentDTO::getDate).reversed());


        model.addAttribute("comments", results);
        return "user/mypage/my-comments";
    }



    @GetMapping("/admin/twitter")
    public String twitterAdmin() {
        return "admin/twitter-admin";
    }

    @GetMapping("/admin")
    public String adminMain() {
        return "admin/admin-main";
    }
}