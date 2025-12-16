package org.zerock.finance_dwpj1.controller.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.finance_dwpj1.entity.content.InfoPost;
import org.zerock.finance_dwpj1.service.content.InfoPostService;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.*;

/**
 * Info 페이지 컨트롤러 (일반 사용자용)
 *
 * 구조:
 * - 글 1개 = InfoPost (제목, 작성자, 작성일, 대표 썸네일)
 * - 글 안에 여러 섹션 = ContentInfoSection (PROFILE, CHANNEL, ACTIVITY, WHY 등)
 *
 * 경로:
 * - GET /info -> 게시글 목록 (카드는 글 개수만큼만 표시)
 * - GET /info/info-write -> 약력 작성 폼 (로그인 필요)
 * - POST /info/api/save-sections -> 글 1개 + 여러 섹션 저장 (로그인 필요)
 * - GET /info/{id} -> 글 상세 (모든 섹션 표시)
 */
@Controller
@RequestMapping("/info")
@RequiredArgsConstructor
@Slf4j
public class InfoController {

    private final InfoPostService postService;

    /**
     * 약력 게시글 목록 페이지
     * GET /info
     *
     * ⭐ 핵심: 섹션 개수가 아니라 글 개수만큼 카드 표시
     */
    @GetMapping
    public String infoList(Model model) {
        log.info("📋 Info 목록 페이지 요청");

        List<InfoPost> posts = postService.getActivePosts();
        model.addAttribute("posts", posts);

        log.info("✅ 활성 게시글 {}개 조회 완료", posts.size());

        return "content/info";
    }

    /**
     * 약력 작성 폼 페이지 (로그인 필요)
     * GET /info/info-write
     */
    @GetMapping("/info-write")
    @PreAuthorize("isAuthenticated()")
    public String writePage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("✏️ 약력 작성 페이지 요청");

        // 작성자 닉네임 전달
        String writer = userDetails != null ? userDetails.getNickname() : "알 수 없음";
        model.addAttribute("writer", writer);

        log.info("📝 작성자: {}", writer);

        return "content/info_form";
    }

    /**
     * 약력 게시글 저장 (여러 섹션 포함)
     * POST /info/api/save-sections
     *
     * ⭐ 핵심: 섹션 개수만큼 글을 만드는 게 아니라
     *         글 1개 안에 여러 섹션을 넣는다!
     *
     * @param allParams 모든 폼 파라미터
     * @param files 업로드된 파일들
     * @param userDetails 로그인한 사용자 정보
     * @return 저장 결과
     */
    @PostMapping("/api/save-sections")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public ResponseEntity<?> saveSections(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(required = false) Map<String, MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String writer = userDetails.getNickname();
        log.info("💾 약력 게시글 저장 요청: 작성자={}", writer);

        try {
            // 1. 섹션 데이터 파싱
            List<InfoPostService.SectionData> sectionsData = new ArrayList<>();
            int index = 0;

            while (true) {
                String sectionType = allParams.getFirst("sections[" + index + "].sectionType");
                if (sectionType == null) break;

                String title = allParams.getFirst("sections[" + index + "].title");
                String content = allParams.getFirst("sections[" + index + "].content");
                MultipartFile imageFile = files != null ? files.get("sections[" + index + "].imageFile") : null;

                sectionsData.add(new InfoPostService.SectionData(sectionType, title, content, imageFile));
                index++;
            }

            if (sectionsData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createResponse(false, "최소 1개 이상의 섹션이 필요합니다"));
            }

            // 2. 글 1개 + 여러 섹션 저장
            InfoPost savedPost = postService.savePost(writer, sectionsData);

            log.info("🎉 약력 게시글 저장 완료: ID={}, 섹션 수={}", savedPost.getId(), sectionsData.size());

            return ResponseEntity.ok(createResponse(true,
                    "약력이 성공적으로 저장되었습니다 (섹션 " + sectionsData.size() + "개)"));

        } catch (Exception e) {
            log.error("약력 저장 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createResponse(false, "저장 실패: " + e.getMessage()));
        }
    }

    /**
     * 약력 상세 페이지
     * GET /info/{id}
     *
     * ⭐ 핵심: 게시글 1개와 연결된 모든 섹션을 보여준다
     *
     * @param id 게시글 ID
     */
    @GetMapping("/{id}")
    public String infoDetail(@PathVariable Long id, Model model) {
        log.info("📄 Info 상세 페이지 요청: ID={}", id);

        Optional<InfoPost> postOpt = postService.getActivePostWithSections(id);

        if (postOpt.isEmpty()) {
            log.warn("⚠️ 게시글을 찾을 수 없음: ID={}", id);
            return "redirect:/info";
        }

        InfoPost post = postOpt.get();
        model.addAttribute("post", post);

        log.info("✅ 게시글 조회 완료: {} (섹션 {}개)", post.getTitle(), post.getSections().size());

        return "content/info_detail";
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