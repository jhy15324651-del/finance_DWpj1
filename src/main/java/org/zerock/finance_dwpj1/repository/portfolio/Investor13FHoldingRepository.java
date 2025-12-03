package org.zerock.finance_dwpj1.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.finance_dwpj1.entity.portfolio.Investor13FHolding;

import java.util.List;

public interface Investor13FHoldingRepository extends JpaRepository<Investor13FHolding, Long> {

    /**
     * 특정 투자대가의 최신 분기 보유 종목 조회
     */
    @Query("SELECT h FROM Investor13FHolding h WHERE h.investorId = :investorId " +
           "AND h.filingQuarter = (SELECT MAX(h2.filingQuarter) FROM Investor13FHolding h2 WHERE h2.investorId = :investorId) " +
           "ORDER BY h.portfolioWeight DESC")
    List<Investor13FHolding> findLatestHoldingsByInvestor(@Param("investorId") String investorId);

    /**
     * 특정 분기의 보유 종목 조회
     */
    List<Investor13FHolding> findByInvestorIdAndFilingQuarterOrderByPortfolioWeightDesc(
            String investorId, String filingQuarter);

    /**
     * 특정 종목을 보유한 투자대가 조회
     */
    @Query("SELECT DISTINCT h.investorId FROM Investor13FHolding h " +
           "WHERE h.ticker = :ticker " +
           "AND h.filingQuarter = (SELECT MAX(h2.filingQuarter) FROM Investor13FHolding h2 WHERE h2.investorId = h.investorId)")
    List<String> findInvestorsByTicker(@Param("ticker") String ticker);

    /**
     * 모든 투자대가의 최신 분기 데이터 조회
     */
    @Query("SELECT h FROM Investor13FHolding h " +
           "WHERE h.filingQuarter IN " +
           "(SELECT MAX(h2.filingQuarter) FROM Investor13FHolding h2 GROUP BY h2.investorId) " +
           "ORDER BY h.investorId, h.portfolioWeight DESC")
    List<Investor13FHolding> findAllLatestHoldings();

    /**
     * 특정 투자대가의 데이터 존재 여부 확인
     */
    boolean existsByInvestorIdAndFilingQuarter(String investorId, String filingQuarter);
}