package org.zerock.finance_dwpj1.controller.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.zerock.finance_dwpj1.dto.insights.InsightsDailyNewsDTO;
import org.zerock.finance_dwpj1.service.insights.DailyNewsService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final DailyNewsService dailyNewsService;

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

    @GetMapping("/admin/twitter")
    public String twitterAdmin() {
        return "admin/twitter-admin";
    }

    @GetMapping("/admin")
    public String adminMain() {
        return "admin/admin-main";
    }
}