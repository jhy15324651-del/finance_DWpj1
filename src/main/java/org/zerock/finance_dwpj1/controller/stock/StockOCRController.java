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

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // traineddata 파일 없음 등 설정 오류
            log.error("[Stock OCR Controller] 설정 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "OCR 설정 오류",
                "message", e.getMessage(),
                "hint", "관리자에게 문의하세요. (traineddata 파일 경로 확인 필요)"
            ));

        } catch (IllegalArgumentException e) {
            // 이미지 파일 형식 오류 등
            log.error("[Stock OCR Controller] 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "잘못된 파일",
                "message", e.getMessage(),
                "hint", "PNG, JPG, JPEG 형식의 이미지 파일을 업로드하세요."
            ));

        } catch (Exception e) {
            // 기타 예외
            log.error("[Stock OCR Controller] OCR 처리 중 예외 발생", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "OCR 처리 실패",
                "message", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류"
            ));

        } catch (Throwable t) {
            // 🔥 Error까지 포함 (JNA Invalid memory access, OutOfMemoryError 등)
            // 서버 크래시 방지를 위한 최종 방어선
            log.error("[Stock OCR Controller] 심각한 오류 발생 (Error 레벨): {}", t.getClass().getName(), t);
            return ResponseEntity.status(500).body(Map.of(
                "error", "서버 내부 오류",
                "message", "OCR 처리 중 심각한 오류가 발생했습니다.",
                "detail", t.getMessage() != null ? t.getMessage() : t.getClass().getName(),
                "hint", "관리자에게 문의하세요. (Tesseract 라이브러리 오류 가능성)"
            ));
        }
    }

}

