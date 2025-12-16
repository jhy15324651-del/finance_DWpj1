package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 게시글 소프트 삭제 (관리자 전용)
     * DELETE /api/admin/info-sections/{id}
     *
     * 동작:
     * - 실제 DB 삭제가 아닌 isDeleted=true로 변경
     * - deletedDate, deletedBy 기록
     * - 일반 사용자는 목록에서 볼 수 없음
     * - 연결된 모든 섹션도 함께 숨겨짐
     *
     * @param id 삭제할 게시글 ID
     * @param principal 현재 로그인한 관리자 정보
     * @return 삭제 결과
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDeleteSection(@PathVariable Long id, Principal principal) {
        String adminId = principal.getName();
        log.info("🗑️ 게시글 삭제 요청: ID={}, 관리자={}", id, adminId);

        boolean success = postService.softDeletePost(id, adminId);

        if (success) {
            return ResponseEntity.ok(createResponse(true, "게시글이 삭제되었습니다"));
        } else {
            return ResponseEntity.badRequest()
                    .body(createResponse(false, "게시글 삭제에 실패했습니다"));
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