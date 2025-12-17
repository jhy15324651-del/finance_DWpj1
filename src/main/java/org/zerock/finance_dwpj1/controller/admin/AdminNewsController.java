package org.zerock.finance_dwpj1.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.entity.insights.InsightsNews;
import org.zerock.finance_dwpj1.service.admin.AdminContentDeletionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 뉴스 관리 컨트롤러
 * - 뉴스 목록 조회 (삭제 포함)
 * - 뉴스 소프트 삭제
 */
@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@Slf4j
public class AdminNewsController {

    private final AdminContentDeletionService deletionService;

    /**
     * 관리자용 뉴스 전체 목록 조회 (삭제 포함)
     */
    @GetMapping
    public ResponseEntity<List<InsightsNews>> getAllNews() {
        log.info("[관리자] 뉴스 전체 목록 조회");
        List<InsightsNews> newsList = deletionService.getAllNewsForAdmin();
        return ResponseEntity.ok(newsList);
    }

    /**
     * 뉴스 소프트 삭제
     */
    @DeleteMapping("/{newsId}")
    public ResponseEntity<Map<String, Object>> softDeleteNews(
            @PathVariable Long newsId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        log.info("[관리자] 뉴스 소프트 삭제 요청 - ID: {}", newsId);

        String deleteReason = requestBody.get("deleteReason");

        // 삭제 사유 필수 체크
        if (deleteReason == null || deleteReason.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 사유를 입력해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            deletionService.softDeleteNews(newsId, deleteReason, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "뉴스가 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("뉴스를 찾을 수 없음: {}", newsId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (IllegalStateException e) {
            log.error("이미 삭제된 뉴스: {}", newsId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("뉴스 삭제 중 오류 발생: {}", newsId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}