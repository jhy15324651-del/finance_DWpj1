package org.zerock.finance_dwpj1.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.zerock.finance_dwpj1.entity.portfolio.InvestorProfile;

import java.util.List;
import java.util.Optional;

public interface InvestorProfileRepository extends JpaRepository<InvestorProfile, String> {

    /**
     * 활성화된 투자대가 전체 조회
     */
    List<InvestorProfile> findByActiveTrue();

    /**
     * CIK로 투자대가 조회
     */
    Optional<InvestorProfile> findByCik(String cik);

    /**
     * 투자 스타일로 조회
     */
    List<InvestorProfile> findByInvestmentStyleAndActiveTrue(String investmentStyle);
}