package org.zerock.finance_dwpj1.controller.insights;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 뉴스 페이지 뷰 컨트롤러
 * Thymeleaf 템플릿을 서빙합니다.
 */
@Controller
public class NewsViewController {

    /**
     * 데일리 뉴스 페이지
     */
    @GetMapping("/news-insights")
    public String newsInsights() {
        return "insights/news-insights";
    }
}
