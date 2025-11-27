package org.zerock.finance_dwpj1.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 비밀번호 유효성 검증 구현체
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        // 8자 이상
        if (password.length() < 8) {
            return false;
        }

        // 숫자 포함 여부
        boolean hasDigit = password.matches(".*\\d.*");

        // 특수문자 포함 여부
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        return hasDigit && hasSpecial;
    }
}
