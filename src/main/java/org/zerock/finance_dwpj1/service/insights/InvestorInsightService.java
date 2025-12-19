package org.zerock.finance_dwpj1.service.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.dto.insights.InvestorInsightDTO;
import org.zerock.finance_dwpj1.entity.insights.InvestorInsight;
import org.zerock.finance_dwpj1.repository.insights.InvestorInsightRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 투자자 인사이트 서비스 (정적 콘텐츠 관리)
 * AI 호출 대신 관리자가 직접 작성/수정하는 투자 철학 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestorInsightService {

    private final InvestorInsightRepository insightRepository;

    /**
     * 전체 투자자 인사이트 조회
     */
    @Transactional(readOnly = true)
    public List<InvestorInsightDTO> getAllInsights() {
        return insightRepository.findAllOrderByCreatedAtDesc().stream()
                .map(InvestorInsightDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * ID로 투자자 인사이트 조회
     */
    @Transactional(readOnly = true)
    public InvestorInsightDTO getInsightById(Long id) {
        InvestorInsight insight = insightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("투자자 인사이트를 찾을 수 없습니다: " + id));
        return InvestorInsightDTO.fromEntity(insight);
    }

    /**
     * investorId로 투자자 인사이트 조회
     */
    @Transactional(readOnly = true)
    public InvestorInsightDTO getInsightByInvestorId(String investorId) {
        InvestorInsight insight = insightRepository.findByInvestorId(investorId)
                .orElseThrow(() -> new IllegalArgumentException("투자자를 찾을 수 없습니다: " + investorId));
        return InvestorInsightDTO.fromEntity(insight);
    }

    /**
     * 여러 investorId로 투자자 인사이트 조회 (프론트 화면용)
     */
    @Transactional(readOnly = true)
    public List<InvestorInsightDTO> getInsightsByInvestorIds(List<String> investorIds) {
        return insightRepository.findByInvestorIdIn(investorIds).stream()
                .map(InvestorInsightDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 투자자 이름으로 검색
     */
    @Transactional(readOnly = true)
    public List<InvestorInsightDTO> searchByName(String keyword) {
        return insightRepository.searchByName(keyword).stream()
                .map(InvestorInsightDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 투자자 인사이트 생성
     */
    @Transactional
    public InvestorInsightDTO createInsight(InvestorInsightDTO dto, String modifiedBy) {
        // 중복 체크
        if (insightRepository.existsByInvestorId(dto.getInvestorId())) {
            throw new IllegalArgumentException("이미 존재하는 investorId입니다: " + dto.getInvestorId());
        }

        InvestorInsight insight = dto.toEntity();
        insight.setModifiedBy(modifiedBy);

        InvestorInsight saved = insightRepository.save(insight);
        log.info("투자자 인사이트 생성 완료 - investorId: {}, 관리자: {}", dto.getInvestorId(), modifiedBy);

        return InvestorInsightDTO.fromEntity(saved);
    }

    /**
     * 투자자 인사이트 수정
     */
    @Transactional
    public InvestorInsightDTO updateInsight(Long id, InvestorInsightDTO dto, String modifiedBy) {
        InvestorInsight insight = insightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("투자자 인사이트를 찾을 수 없습니다: " + id));

        // 업데이트
        insight.setName(dto.getName());
        insight.setPhilosophyKo(dto.getPhilosophyKo());
        insight.setInvestmentStyle(dto.getInvestmentStyle());
        insight.setProfileImageUrl(dto.getProfileImageUrl());
        insight.setModifiedBy(modifiedBy);

        insightRepository.save(insight);
        log.info("투자자 인사이트 수정 완료 - ID: {}, investorId: {}, 관리자: {}", id, insight.getInvestorId(), modifiedBy);

        return InvestorInsightDTO.fromEntity(insight);
    }

    /**
     * 투자자 인사이트 삭제
     */
    @Transactional
    public void deleteInsight(Long id) {
        InvestorInsight insight = insightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("투자자 인사이트를 찾을 수 없습니다: " + id));

        insightRepository.delete(insight);
        log.info("투자자 인사이트 삭제 완료 - ID: {}, investorId: {}", id, insight.getInvestorId());
    }

    /**
     * investorId 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsByInvestorId(String investorId) {
        return insightRepository.existsByInvestorId(investorId);
    }
}