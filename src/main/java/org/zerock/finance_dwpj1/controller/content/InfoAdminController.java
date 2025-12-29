package org.zerock.finance_dwpj1.controller.content;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.zerock.finance_dwpj1.service.admin.AdminContentDeletionService;
import org.zerock.finance_dwpj1.service.content.InfoPostService;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Info 게시글 관리자 컨트롤러 (관리자 전용)
 *
 * 권한: ROLE_ADMIN만 접근 가능
 *
 * 기능:
 * - 게시글 소프트 삭제 (연결된 모든 섹션도 함께 숨겨짐)
 * - 게시글 복구
 *
 * 참고: 구조 변경으로 섹션이 아닌 게시글 단위로 관리합니다.
 */
@RestController
@RequestMapping("/api/admin/info-sections")  // URL 호환성 유지
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class InfoAdminController {

    private final InfoPostService postService;
    private final AdminContentDeletionService deletionService;

    /**
     * 게시글 소프트 삭제 (관리자 전용)
     * DELETE /api/admin/info-sections/{id}
     *
     * News/ContentReview 패턴과 동일하게 감사 로그 기록
     *
     * @param id 삭제할 게시글 ID
     * @param requestBody 삭제 사유 (deleteReason 필수)
     * @param request HTTP 요청 (IP, UserAgent 추출용)
     * @return 삭제 결과
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> softDeleteSection(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        log.info("🗑️ 약력 게시글 삭제 요청 - ID: {}", id);

        String deleteReason = requestBody.get("deleteReason");

        // 삭제 사유 필수 체크 (News/ContentReview 패턴)
        if (deleteReason == null || deleteReason.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 사유를 입력해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // AdminContentDeletionService 사용 (감사 로그 자동 기록)
            deletionService.softDeleteInfoPost(id, deleteReason, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "약력 게시글이 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("약력 게시글을 찾을 수 없음: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (IllegalStateException e) {
            log.error("이미 삭제된 약력 게시글: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("약력 게시글 삭제 중 오류 발생: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 게시글 복구 (관리자 전용)
     * POST /api/admin/info-sections/{id}/restore
     *
     * 동작:
     * - isDeleted=false로 변경
     * - deletedDate, deletedBy 초기화
     * - 일반 사용자가 다시 볼 수 있음
     * - 연결된 모든 섹션도 함께 복구됨
     *
     * @param id 복구할 게시글 ID
     * @return 복구 결과
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreSection(@PathVariable Long id) {
        log.info("♻️ 게시글 복구 요청: ID={}", id);

        boolean success = postService.restorePost(id);

        if (success) {
            return ResponseEntity.ok(createResponse(true, "게시글이 복구되었습니다"));
        } else {
            return ResponseEntity.badRequest()
                    .body(createResponse(false, "게시글 복구에 실패했습니다"));
        }
    }

    /**
     * @deprecated 구조 변경으로 더 이상 지원하지 않습니다.
     * 이제 /info/write 페이지에서 직접 게시글을 작성하세요.
     *
     * POST /api/admin/info-sections/initialize
     */
    @Deprecated
    @PostMapping("/initialize")
    public ResponseEntity<?> initializeDefaultSections() {
        log.warn("⚠️ Deprecated: 초기화 기능은 더 이상 지원하지 않습니다. /info/write 페이지를 사용하세요.");
        return ResponseEntity.badRequest()
                .body(createResponse(false, "이 기능은 더 이상 지원하지 않습니다. /info/write 페이지에서 직접 작성해주세요."));
    }

    /**
     * 응답 객체 생성 헬퍼 메서드
     */
    private Map<String, Object> createResponse(boolean success, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }
}