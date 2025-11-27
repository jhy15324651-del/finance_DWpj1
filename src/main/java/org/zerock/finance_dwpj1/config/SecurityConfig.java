package org.zerock.finance_dwpj1.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.zerock.finance_dwpj1.service.user.CustomUserDetailsService;

/**
 * Spring Security 설정
 *
 * 개발 모드: 모든 접근 허용 (인증 없이도 모든 페이지 접근 가능)
 * 프로덕션 배포 시 주석 처리된 부분을 활성화하세요.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API용)
                .csrf(csrf -> csrf.disable())

                // ========== 개발 모드: 모든 접근 허용 ==========
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // 모든 요청 허용 (인증 불필요)
                )

                // 로그인/로그아웃 기능은 유지 (선택적으로 사용 가능)
                .formLogin(form -> form
                        .loginPage("/user/login")
                        .loginProcessingUrl("/user/login")
                        .defaultSuccessUrl("/")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/user/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )

                .userDetailsService(customUserDetailsService);

        // ========== 프로덕션 배포 시 아래 주석 해제하고 위 부분 주석 처리 ==========
        /*
        .authorizeHttpRequests(auth -> auth
                // 누구나 접근 가능
                .requestMatchers("/", "/user/signup", "/user/login", "/user/check-**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/upload/**").permitAll()

                // 관리자만 접근 가능
                .requestMatchers("/api/news/admin/**", "/admin/**").hasRole("ADMIN")

                // 로그인한 사용자만 접근 가능
                .requestMatchers("/user/mypage/**").authenticated()

                // 나머지는 모두 허용
                .anyRequest().permitAll()
        )
        */

        return http.build();
    }
}
