package org.zerock.finance_dwpj1.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.service.admin.AdminContentDeletionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 콘텐츠 리뷰 관리 컨트롤러
 * - 콘텐츠 리뷰 목록 조회 (삭제 포함)
 * - 콘텐츠 리뷰 소프트 삭제
 */
@RestController
@RequestMapping("/api/admin/content-reviews")
@RequiredArgsConstructor
@Slf4j
public class AdminContentReviewController {

    private final AdminContentDeletionService deletionService;

    /**
     * 관리자용 콘텐츠 리뷰 전체 목록 조회 (삭제 포함)
     */
    @GetMapping
    public ResponseEntity<List<ContentReview>> getAllContentReviews() {
        log.info("[관리자] 콘텐츠 리뷰 전체 목록 조회");
        List<ContentReview> reviews = deletionService.getAllContentReviewsForAdmin();
        return ResponseEntity.ok(reviews);
    }

    /**
     * 콘텐츠 리뷰 소프트 삭제
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> softDeleteContentReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        log.info("[관리자] 콘텐츠 리뷰 소프트 삭제 요청 - ID: {}", reviewId);

        String deleteReason = requestBody.get("deleteReason");

        // 삭제 사유 필수 체크
        if (deleteReason == null || deleteReason.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 사유를 입력해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            deletionService.softDeleteContentReview(reviewId, deleteReason, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "콘텐츠 리뷰가 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("콘텐츠 리뷰를 찾을 수 없음: {}", reviewId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (IllegalStateException e) {
            log.error("이미 삭제된 콘텐츠 리뷰: {}", reviewId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("콘텐츠 리뷰 삭제 중 오류 발생: {}", reviewId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}