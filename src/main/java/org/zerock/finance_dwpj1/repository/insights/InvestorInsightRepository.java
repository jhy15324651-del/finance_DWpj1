package org.zerock.finance_dwpj1.repository.insights;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.insights.InvestorInsight;

import java.util.List;
import java.util.Optional;

/**
 * 투자자 인사이트 Repository
 */
public interface InvestorInsightRepository extends JpaRepository<InvestorInsight, Long> {

    /**
     * investorId로 투자자 인사이트 조회
     */
    Optional<InvestorInsight> findByInvestorId(String investorId);

    /**
     * investorId 리스트로 투자자 인사이트 조회
     */
    @Query("SELECT i FROM InvestorInsight i WHERE i.investorId IN :investorIds")
    List<InvestorInsight> findByInvestorIdIn(@Param("investorIds") List<String> investorIds);

    /**
     * investorId 존재 여부 확인
     */
    boolean existsByInvestorId(String investorId);

    /**
     * 전체 투자자 인사이트 조회 (생성일 역순)
     */
    @Query("SELECT i FROM InvestorInsight i ORDER BY i.createdAt DESC")
    List<InvestorInsight> findAllOrderByCreatedAtDesc();

    /**
     * 투자자 이름으로 검색
     */
    @Query("SELECT i FROM InvestorInsight i WHERE i.name LIKE %:keyword% ORDER BY i.createdAt DESC")
    List<InvestorInsight> searchByName(@Param("keyword") String keyword);
}