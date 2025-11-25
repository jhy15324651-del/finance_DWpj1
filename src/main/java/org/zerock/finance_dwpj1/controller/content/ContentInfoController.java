package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 콘텐츠 정보 컨트롤러
 * 정보 페이지 처리
 */
@Slf4j
@Controller
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentInfoController {

    /**
     * 정보 페이지
     */
    @GetMapping("/info")
    public String info(Model model) {
        log.debug("정보 페이지 요청");
        return "content/info";
    }
}
