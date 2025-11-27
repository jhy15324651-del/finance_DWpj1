package org.zerock.finance_dwpj1.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.user.UserSessionDTO;
import org.zerock.finance_dwpj1.dto.user.UserSignupDTO;
import org.zerock.finance_dwpj1.entity.user.User;
import org.zerock.finance_dwpj1.repository.user.UserRepository;

/**
 * 사용자 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public UserSessionDTO signup(UserSignupDTO dto) {
        log.info("회원가입 시도: {}", dto.getEmail());

        // 1. 유효성 검증
        validateSignup(dto);

        // 2. User 엔티티 생성
        User user = User.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .isActive(true)
                .build();

        // 3. 저장
        User savedUser = userRepository.save(user);

        log.info("회원가입 완료: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

        // 4. 세션 DTO 반환
        return UserSessionDTO.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * 회원가입 유효성 검증
     */
    private void validateSignup(UserSignupDTO dto) {
        // 비밀번호 확인 일치 여부
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다");
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다");
        }
    }

    /**
     * 이메일 중복 체크 (AJAX용)
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * 닉네임 중복 체크 (AJAX용)
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
}

// 이중 보안 프론트에서 쓰는 코드와 다르게 한번더 백에서 정말 중복및 문제가 없는지 체크