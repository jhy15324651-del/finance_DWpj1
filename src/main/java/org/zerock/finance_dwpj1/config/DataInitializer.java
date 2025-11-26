package org.zerock.finance_dwpj1.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.zerock.finance_dwpj1.entity.insights.InsightsAdminUser;
import org.zerock.finance_dwpj1.repository.insights.InsightsAdminUserRepository;

/**
 * 애플리케이션 시작 시 초기 데이터 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final InsightsAdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 관리자 계정이 없으면 기본 관리자 생성
        if (adminUserRepository.count() == 0) {
            InsightsAdminUser admin = new InsightsAdminUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setRole(InsightsAdminUser.Role.ADMIN);

            adminUserRepository.save(admin);
            log.info("==============================================");
            log.info("기본 관리자 계정 생성 완료");
            log.info("아이디: admin");
            log.info("비밀번호: admin1234");
            log.info("==============================================");
        }
    }
}
