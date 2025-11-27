package org.zerock.finance_dwpj1.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.entity.user.User;
import org.zerock.finance_dwpj1.repository.user.UserRepository;

import java.util.Collections;

/**
 * Spring Security 로그인 처리
 * UserDetailsService를 구현하여 로그인 시 사용자 정보를 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("로그인 시도: {}", email);

        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 비활성화된 계정 체크
        if (!user.getIsActive()) {
            log.warn("비활성화된 계정 로그인 시도: {}", email);
            throw new RuntimeException("비활성화된 계정입니다");
        }

        log.info("로그인 성공: {} ({})", user.getEmail(), user.getNickname());

        // Spring Security User 객체 반환
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .build();
    }
}