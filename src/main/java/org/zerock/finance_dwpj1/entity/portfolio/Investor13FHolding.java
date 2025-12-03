package org.zerock.finance_dwpj1.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 투자대가 13F 보유 종목 엔티티
 * SEC 13F 분기보고서 데이터 저장
 */
@Entity
@Table(name = "investor_13f_holdings", indexes = {
        @Index(name = "idx_investor_quarter", columnList = "investorId, filingQuarter DESC"),
        @Index(name = "idx_ticker", columnList = "ticker")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investor13FHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String investorId; // 투자대가 ID (buffett, wood 등)

    @Column(nullable = false, length = 10)
    private String ticker; // 종목 티커 (AAPL, TSLA 등)

    @Column(length = 200)
    private String companyName; // 회사명

    @Column(nullable = false)
    private Long shares; // 보유 주식 수

    @Column(nullable = false)
    private Double marketValue; // 시장 가치 (USD)

    @Column(nullable = false)
    private Double portfolioWeight; // 포트폴리오 비중 (%)

    @Column(nullable = false, length = 7)
    private String filingQuarter; // 분기 (예: 2024Q1, 2024Q2)

    @Column(nullable = false)
    private LocalDate filingDate; // 보고서 제출일

    @Column(length = 500)
    private String secFilingUrl; // SEC 원본 파일 URL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investorId", insertable = false, updatable = false)
    private InvestorProfile investorProfile;
}