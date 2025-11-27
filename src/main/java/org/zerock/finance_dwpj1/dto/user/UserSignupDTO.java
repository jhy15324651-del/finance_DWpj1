package org.zerock.finance_dwpj1.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;
import org.zerock.finance_dwpj1.validator.ValidPassword;

/**
 * 회원가입 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignupDTO {

    /**
     * 이메일 (로그인 ID)
     */
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    /**
     * 닉네임 (화면 표시용)
     */
    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(min = 2, max = 30, message = "닉네임은 2~30자 이내여야 합니다")
    private String nickname;

    /**
     * 비밀번호
     * - 8자 이상
     * - 숫자 1개 이상
     * - 특수문자 1개 이상
     */
    @NotBlank(message = "비밀번호를 입력해주세요")
    @ValidPassword
    private String password;

    /**
     * 비밀번호 확인
     */
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String passwordConfirm;

    /**
     * 전화번호 (선택)
     */
    private String phoneNumber;

    /**
     * 이용약관 동의 (필수)
     */
    @AssertTrue(message = "이용약관에 동의해주세요")
    private Boolean agreeTerms;

    /**
     * 개인정보 처리방침 동의 (필수)
     */
    @AssertTrue(message = "개인정보 처리방침에 동의해주세요")
    private Boolean agreePrivacy;

    /**
     * 마케팅 정보 수신 동의 (선택)
     */
    @Builder.Default
    private Boolean agreeMarketing = false;
}