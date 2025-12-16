package org.zerock.finance_dwpj1.controller.stock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zerock.finance_dwpj1.service.stock.StockGradeCalculatorService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;
import org.zerock.finance_dwpj1.service.user.UserService;

@Slf4j
@Controller
@RequestMapping("/user/mypage")
@RequiredArgsConstructor
public class StockGradeController {

    private final UserService userService;
    private final StockGradeCalculatorService  stockGradeCalculatorService;

    @GetMapping("/gradeupdate")
    public String showGradePage(
            @AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }

        Long userId = user.getId();

        // DB에서 저장된 등급 가져오기
        String grade = userService.getUserGrade(userId);

        // 등급에 맞는 이모지 생성
        String medal = stockGradeCalculatorService.gradeToEmoji(grade);

        //  템플릿으로 전달
        model.addAttribute("medal", medal);

        return "user/mypage/my-gradeupdate";
    }
}
