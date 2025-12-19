package org.zerock.finance_dwpj1.dto.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zerock.finance_dwpj1.entity.insights.InvestorInsight;

import java.time.LocalDateTime;

/**
 * 투자자 인사이트 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorInsightDTO {

    private Long id;
    private String investorId;
    private String name;
    private String philosophyKo;
    private String investmentStyle;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String modifiedBy;

    /**
     * Entity -> DTO 변환
     */
    public static InvestorInsightDTO fromEntity(InvestorInsight entity) {
        return InvestorInsightDTO.builder()
                .id(entity.getId())
                .investorId(entity.getInvestorId())
                .name(entity.getName())
                .philosophyKo(entity.getPhilosophyKo())
                .investmentStyle(entity.getInvestmentStyle())
                .profileImageUrl(entity.getProfileImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .modifiedBy(entity.getModifiedBy())
                .build();
    }

    /**
     * DTO -> Entity 변환
     */
    public InvestorInsight toEntity() {
        return InvestorInsight.builder()
                .id(this.id)
                .investorId(this.investorId)
                .name(this.name)
                .philosophyKo(this.philosophyKo)
                .investmentStyle(this.investmentStyle)
                .profileImageUrl(this.profileImageUrl)
                .modifiedBy(this.modifiedBy)
                .build();
    }
}