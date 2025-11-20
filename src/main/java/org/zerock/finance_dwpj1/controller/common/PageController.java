package org.zerock.finance_dwpj1.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    @GetMapping("/news")
    public String newsInsights() {
        return "news-insights";
    }

    @GetMapping("/portfolio")
    public String portfolioComparison() {
        return "portfolio-comparison";
    }
}