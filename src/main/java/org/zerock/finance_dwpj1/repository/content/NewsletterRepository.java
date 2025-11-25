package org.zerock.finance_dwpj1.repository.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.finance_dwpj1.entity.content.Newsletter;

import java.util.List;

/**
 * Newsletter Repository
 * 뉴스레터 데이터 접근 인터페이스
 */
@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {

    /**
     * 최신 뉴스레터 목록 조회 (삭제되지 않은 것만)
     */
    List<Newsletter> findByIsDeletedFalseOrderByCreatedDateDesc();

    /**
     * 전체 뉴스레터 수 조회 (삭제되지 않은 것만)
     */
    long countByIsDeletedFalse();
}
