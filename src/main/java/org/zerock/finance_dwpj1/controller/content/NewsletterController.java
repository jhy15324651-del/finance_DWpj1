package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zerock.finance_dwpj1.entity.content.Newsletter;
import org.zerock.finance_dwpj1.service.content.NewsletterService;

import java.util.List;

/**
 * 뉴스레터 컨트롤러
 * 뉴스레터 목록 조회
 */
@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    /**
     * 뉴스레터 목록 페이지
     */
    @GetMapping("/newsletter")
    public String newsletter(Model model) {
        log.debug("뉴스레터 목록 요청");

        List<Newsletter> newsletters = newsletterService.getAllNewsletters();
        model.addAttribute("newsletters", newsletters);

        return "content/newsletter";
    }
}
