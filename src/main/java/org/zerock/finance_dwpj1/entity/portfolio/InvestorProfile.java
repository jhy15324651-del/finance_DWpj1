package org.zerock.finance_dwpj1.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;

/**
 * 투자대가 프로필 엔티티
 * 투자 철학, 장점, 단점 등 정보 저장
 */
@Entity
@Table(name = "investor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorProfile {

    @Id
    @Column(length = 50)
    private String investorId; // buffett, wood, dalio 등

    @Column(nullable = false, length = 100)
    private String name; // 워렌 버핏, 캐시 우드

    @Column(nullable = false, length = 100)
    private String nameEn; // Warren Buffett, Cathie Wood

    @Column(nullable = false, length = 50)
    private String organization; // Berkshire Hathaway, ARK Invest

    @Column(nullable = false, length = 20)
    private String cik; // SEC CIK 번호 (예: 0001067983)

    @Column(columnDefinition = "TEXT")
    private String philosophy; // 투자 철학

    @Column(columnDefinition = "TEXT")
    private String strengths; // 장점 (줄바꿈으로 구분)

    @Column(columnDefinition = "TEXT")
    private String weaknesses; // 단점 (줄바꿈으로 구분)

    @Column(length = 50)
    private String investmentStyle; // 가치투자, 성장주, 혁신기술 등

    @Column(length = 500)
    private String profileImageUrl; // 프로필 이미지 URL

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true; // 활성화 여부
}