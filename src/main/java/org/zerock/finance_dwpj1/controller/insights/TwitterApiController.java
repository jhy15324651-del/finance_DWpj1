package org.zerock.finance_dwpj1.controller.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.insights.InsightsTwitterDTO;
import org.zerock.finance_dwpj1.service.insights.TwitterService;

import java.util.List;

@RestController
@RequestMapping("/api/twitter")
@RequiredArgsConstructor
@Log4j2
public class TwitterApiController {

    private final TwitterService twitterService;

    @GetMapping("/insights")
    public List<InsightsTwitterDTO> getTwitterInsights() {
        log.info("트위터 인사이트 요청");
        return twitterService.getTwitterInsights();
    }
}