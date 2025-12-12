package org.zerock.finance_dwpj1.controller.stock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.dto.stock.StockOCRResultDTO;
import org.zerock.finance_dwpj1.service.stock.StockGradeCalculatorService;
import org.zerock.finance_dwpj1.service.stock.StockOCRService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;
import org.zerock.finance_dwpj1.service.user.UserService;

import java.util.Map;


@Slf4j
@Controller
@RequestMapping("/user/mypage")
@RequiredArgsConstructor
public class StockOCRController {

    private final StockOCRService stockOCRService;
    private final StockGradeCalculatorService stockGradeCalculatorService;
    private final UserService userService;

    @PostMapping("/grade/update")
    public ResponseEntity<?> updateGrade(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        try {
            // OCR 실행
            StockOCRResultDTO result = stockOCRService.processImage(file);

            // 등급 계산
            String grade = stockGradeCalculatorService.calculate(
                    result.getAmount(),
                    result.getPercent(),
                    result.getType()
            );

            // DB에 저장
            userService.updateGrade(user.getId(), grade);


            String medal = stockGradeCalculatorService.gradeToEmoji(grade);

            // 프론트에 전달할 응답
            Map<String, Object> response = Map.of(
                    "grade", grade,
                    "medal", medal,
                    "amount", result.getAmount(),
                    "percent", result.getPercent(),
                    "type", result.getType()
            );

            return ResponseEntity.ok(response);  // 🔥 수정됨

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("OCR 처리 실패 : " + e.getMessage());
        }
    }

}

