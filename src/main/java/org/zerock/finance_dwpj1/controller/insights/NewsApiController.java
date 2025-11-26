package org.zerock.finance_dwpj1.controller.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.insights.InsightsNewsDTO;
import org.zerock.finance_dwpj1.service.insights.NewsScrapingService;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Log4j2
public class NewsApiController {

    private final NewsScrapingService newsScrapingService;

    @GetMapping("/{category}")
    public List<InsightsNewsDTO> getNews(@PathVariable String category) {
        log.info("뉴스 요청: " + category);
        return newsScrapingService.scrapeYahooFinanceNews(category);
    }
}