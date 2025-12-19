package org.zerock.finance_dwpj1.controller.insights;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.insights.InvestorInsightDTO;
import org.zerock.finance_dwpj1.service.insights.InvestorInsightService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 투자자 인사이트 관리 컨트롤러
 * - 투자자 인사이트 CRUD (AI 호출 대신 관리자가 직접 관리)
 */
@RestController
@RequestMapping("/api/admin/investor-insights")
@RequiredArgsConstructor
@Slf4j
public class InvestorInsightAdminController {

    private final InvestorInsightService insightService;

    /**
     * 전체 투자자 인사이트 조회
     */
    @GetMapping
    public ResponseEntity<List<InvestorInsightDTO>> getAllInsights() {
        log.info("[관리자] 투자자 인사이트 전체 목록 조회");
        List<InvestorInsightDTO> insights = insightService.getAllInsights();
        return ResponseEntity.ok(insights);
    }

    /**
     * ID로 투자자 인사이트 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvestorInsightDTO> getInsightById(@PathVariable Long id) {
        log.info("[관리자] 투자자 인사이트 조회 - ID: {}", id);
        InvestorInsightDTO insight = insightService.getInsightById(id);
        return ResponseEntity.ok(insight);
    }

    /**
     * investorId로 투자자 인사이트 조회
     */
    @GetMapping("/by-investor/{investorId}")
    public ResponseEntity<InvestorInsightDTO> getInsightByInvestorId(@PathVariable String investorId) {
        log.info("[관리자] 투자자 인사이트 조회 - investorId: {}", investorId);
        InvestorInsightDTO insight = insightService.getInsightByInvestorId(investorId);
        return ResponseEntity.ok(insight);
    }

    /**
     * 투자자 이름으로 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<InvestorInsightDTO>> searchByName(@RequestParam String keyword) {
        log.info("[관리자] 투자자 인사이트 검색 - 키워드: {}", keyword);
        List<InvestorInsightDTO> insights = insightService.searchByName(keyword);
        return ResponseEntity.ok(insights);
    }

    /**
     * 투자자 인사이트 생성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createInsight(
            @RequestBody InvestorInsightDTO dto,
            Authentication authentication
    ) {
        log.info("[관리자] 투자자 인사이트 생성 요청 - investorId: {}", dto.getInvestorId());

        try {
            String modifiedBy = authentication != null ? authentication.getName() : "system";
            InvestorInsightDTO created = insightService.createInsight(dto, modifiedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "투자자 인사이트가 생성되었습니다.");
            response.put("data", created);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[관리자] 투자자 인사이트 생성 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 투자자 인사이트 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateInsight(
            @PathVariable Long id,
            @RequestBody InvestorInsightDTO dto,
            Authentication authentication
    ) {
        log.info("[관리자] 투자자 인사이트 수정 요청 - ID: {}", id);

        try {
            String modifiedBy = authentication != null ? authentication.getName() : "system";
            InvestorInsightDTO updated = insightService.updateInsight(id, dto, modifiedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "투자자 인사이트가 수정되었습니다.");
            response.put("data", updated);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[관리자] 투자자 인사이트 수정 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 투자자 인사이트 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteInsight(@PathVariable Long id) {
        log.info("[관리자] 투자자 인사이트 삭제 요청 - ID: {}", id);

        try {
            insightService.deleteInsight(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "투자자 인사이트가 삭제되었습니다.");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[관리자] 투자자 인사이트 삭제 실패: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * investorId 존재 여부 확인
     */
    @GetMapping("/exists/{investorId}")
    public ResponseEntity<Map<String, Boolean>> checkExists(@PathVariable String investorId) {
        log.info("[관리자] investorId 존재 여부 확인 - investorId: {}", investorId);
        boolean exists = insightService.existsByInvestorId(investorId);

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);

        return ResponseEntity.ok(response);
    }
}