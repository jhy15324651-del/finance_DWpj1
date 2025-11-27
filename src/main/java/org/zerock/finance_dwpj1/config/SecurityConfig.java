package org.zerock.finance_dwpj1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * <p>
 * 개발 모드: 모든 접근 허용 (인증 비활성화)
 * 프로덕션 배포 시 주석 처리된 부분을 활성화하세요.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                        .anyRequest().permitAll()  // 모든 요청 허용
                );

        // ========== 프로덕션 배포 시 아래 주석 해제 ==========
        /*
        .authorizeHttpRequests(auth -> auth
                // 관리자 전용 API
                .requestMatchers("/api/news/admin/**").hasRole("ADMIN")

                // 나머지는 모두 허용
                .anyRequest().permitAll()
        )

        // 폼 로그인 설정
        .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
        )

        // 로그아웃 설정
        .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
        );
        */

        return http.build();
    }
}

