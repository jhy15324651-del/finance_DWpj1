package org.zerock.finance_dwpj1.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
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

    @GetMapping("/user/mypage")
    public String myPage() {
        return "user/mypage";
    }
}