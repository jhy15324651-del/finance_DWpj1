package org.zerock.finance_dwpj1.repository.insights;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.insights.InsightsAdminUser;

import java.util.Optional;

/**
 * 관리자 사용자 Repository
 */
public interface InsightsAdminUserRepository extends JpaRepository<InsightsAdminUser, Long> {

    /**
     * 사용자명으로 관리자 조회
     */
    Optional<InsightsAdminUser> findByUsername(String username);

    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);
}
