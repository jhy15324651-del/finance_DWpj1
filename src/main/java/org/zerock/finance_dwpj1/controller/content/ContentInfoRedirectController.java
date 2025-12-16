package org.zerock.finance_dwpj1.controller.content;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy URL 호환성을 위한 Redirect Controller
 *
 * /content/info -> /info로 redirect
 */
@Controller
@Slf4j
public class ContentInfoRedirectController {

    /**
     * Legacy URL 처리
     * GET /content/info -> redirect to /info
     */
    @GetMapping("/content/info")
    public String redirectToInfo() {
        log.info("🔄 Legacy URL 접근: /content/info -> /info로 redirect");
        return "redirect:/info";
    }
}