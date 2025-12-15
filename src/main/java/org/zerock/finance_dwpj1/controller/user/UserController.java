package org.zerock.finance_dwpj1.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.user.UserSessionDTO;
import org.zerock.finance_dwpj1.dto.user.UserSignupDTO;
import org.zerock.finance_dwpj1.service.user.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 컨트롤러
 * 회원가입, 로그인, 로그아웃 처리
 */
@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 페이지
     */
    @GetMapping("/signup")
    public String signupPage() {
        return "user/signup";
    }

    /**
     * 회원가입 처리
     */
    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<?> signup(@Valid @RequestBody UserSignupDTO dto, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        // 유효성 검증 실패
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            response.put("success", false);
            response.put("message", errorMessage);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 회원가입 처리
            UserSessionDTO userSession = userService.signup(dto);

            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다");
            response.put("user", userSession);

            log.info("회원가입 성공: {}", dto.getEmail());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 비즈니스 로직 에러 (이메일 중복 등)
            response.put("success", false);
            response.put("message", e.getMessage());
            log.warn("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            // 예상치 못한 에러
            response.put("success", false);
            response.put("message", "회원가입 중 오류가 발생했습니다");
            log.error("회원가입 오류", e);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 이메일 중복 체크 (AJAX)
     */
    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    /**
     * 닉네임 중복 체크 (AJAX)
     */
    @GetMapping("/check-nickname")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그인 페이지
     */

    @GetMapping("/login")
    public String login(HttpServletRequest request) {

        String referer = request.getHeader("Referer");

        if (referer != null &&
                !referer.contains("/user/login") &&
                !referer.contains("/user/logout")) {

            request.getSession().setAttribute("prevPage", referer);
        }

        return "user/login";
    }

    /**
     * 로그아웃
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}