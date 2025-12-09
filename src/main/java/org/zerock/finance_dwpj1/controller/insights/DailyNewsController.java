package org.zerock.finance_dwpj1.controller.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.insights.InsightsCommentDTO;
import org.zerock.finance_dwpj1.dto.insights.InsightsDailyNewsDTO;
import org.zerock.finance_dwpj1.service.insights.DailyNewsService;
import org.zerock.finance_dwpj1.service.insights.NewsSchedulerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데일리 뉴스 API 컨트롤러
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class DailyNewsController {

    private final DailyNewsService dailyNewsService;
    private final NewsSchedulerService schedulerService;

    /**
     * 데일리 뉴스 목록 조회 (24시간 이내)
     */
    @GetMapping("/daily")
    public ResponseEntity<List<InsightsDailyNewsDTO>> getDailyNews() {
        log.info("데일리 뉴스 조회 요청");
        List<InsightsDailyNewsDTO> newsList = dailyNewsService.getDailyNews();
        return ResponseEntity.ok(newsList);
    }

    /**
     * 데일리 뉴스 페이징 조회
     */
    @GetMapping("/daily/page")
    public ResponseEntity<Page<InsightsDailyNewsDTO>> getDailyNewsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("데일리 뉴스 페이징 조회 - page: {}, size: {}", page, size);
        Page<InsightsDailyNewsDTO> newsPage = dailyNewsService.getDailyNews(page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**
     * 아카이브 뉴스 조회 (24시간 이상)
     */
    @GetMapping("/archive")
    public ResponseEntity<Page<InsightsDailyNewsDTO>> getArchiveNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("아카이브 뉴스 조회 - page: {}, size: {}", page, size);
        Page<InsightsDailyNewsDTO> newsPage = dailyNewsService.getArchiveNews(page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**
     * 금주의 뉴스 (조회수 TOP 10)
     */
    @GetMapping("/weekly-top")
    public ResponseEntity<List<InsightsDailyNewsDTO>> getWeeklyTopNews() {
        log.info("금주의 뉴스 조회");
        List<InsightsDailyNewsDTO> topNews = dailyNewsService.getWeeklyTopNews();
        return ResponseEntity.ok(topNews);
    }

    /**
     * 뉴스 상세 조회 (조회수 증가)
     */
    @GetMapping("/detail/{newsId}")
    public ResponseEntity<InsightsDailyNewsDTO> getNewsDetail(@PathVariable Long newsId) {
        log.info("뉴스 상세 조회 - ID: {}", newsId);
        InsightsDailyNewsDTO newsDTO = dailyNewsService.getNewsDetail(newsId);
        return ResponseEntity.ok(newsDTO);
    }

    /**
     * 뉴스 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<InsightsDailyNewsDTO>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("뉴스 검색 - keyword: {}, page: {}, size: {}", keyword, page, size);
        Page<InsightsDailyNewsDTO> newsPage = dailyNewsService.searchNews(keyword, page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/{newsId}/comments")
    public ResponseEntity<List<InsightsCommentDTO>> getComments(@PathVariable Long newsId) {
        log.info("댓글 조회 - 뉴스 ID: {}", newsId);
        List<InsightsCommentDTO> comments = dailyNewsService.getComments(newsId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 작성 (일반 댓글 및 답글)
     */
    @PostMapping("/{newsId}/comments")
    public ResponseEntity<InsightsCommentDTO> addComment(
            @PathVariable Long newsId,
            @RequestBody InsightsCommentDTO commentDTO,
            java.security.Principal principal) {

        // 로그인한 유저의 username 자동 설정 (클라이언트에서 보낸 값 무시)
        String username = principal != null ? principal.getName() : "익명";
        commentDTO.setUserName(username);

        log.info("댓글 작성 - 뉴스 ID: {}, 작성자: {}", newsId, username);
        commentDTO.setNewsId(newsId);
        InsightsCommentDTO savedComment = dailyNewsService.addComment(commentDTO);
        return ResponseEntity.ok(savedComment);
    }

    /**
     * 댓글 좋아요
     */
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<InsightsCommentDTO> likeComment(@PathVariable Long commentId) {
        log.info("댓글 좋아요 - ID: {}", commentId);
        InsightsCommentDTO updatedComment = dailyNewsService.likeComment(commentId);
        return ResponseEntity.ok(updatedComment);
    }

    /**
     * 댓글 싫어요
     */
    @PostMapping("/comments/{commentId}/dislike")
    public ResponseEntity<InsightsCommentDTO> dislikeComment(@PathVariable Long commentId) {
        log.info("댓글 싫어요 - ID: {}", commentId);
        InsightsCommentDTO updatedComment = dailyNewsService.dislikeComment(commentId);
        return ResponseEntity.ok(updatedComment);
    }

    // ========== 관리자 전용 API ==========

    /**
     * 뉴스 수정 (관리자 전용)
     */
    @PutMapping("/admin/{newsId}")
    public ResponseEntity<InsightsDailyNewsDTO> updateNews(
            @PathVariable Long newsId,
            @RequestBody InsightsDailyNewsDTO newsDTO) {
        log.info("뉴스 수정 - ID: {}", newsId);
        InsightsDailyNewsDTO updatedNews = dailyNewsService.updateNews(newsId, newsDTO);
        return ResponseEntity.ok(updatedNews);
    }

    /**
     * 뉴스 삭제 (관리자 전용)
     */
    @DeleteMapping("/admin/{newsId}")
    public ResponseEntity<Map<String, String>> deleteNews(@PathVariable Long newsId) {
        log.info("뉴스 삭제 - ID: {}", newsId);
        dailyNewsService.deleteNews(newsId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "뉴스가 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제 (관리자 전용)
     */
    @DeleteMapping("/admin/comments/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(@PathVariable Long commentId) {
        log.info("댓글 삭제 - ID: {}", commentId);
        dailyNewsService.deleteComment(commentId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "댓글이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 수동 크롤링 실행 (관리자 전용, 테스트용)
     */
    @PostMapping("/admin/crawl")
    public ResponseEntity<Map<String, String>> manualCrawl() {
        log.info("수동 크롤링 실행");
        schedulerService.manualCrawl();

        Map<String, String> response = new HashMap<>();
        response.put("message", "크롤링이 시작되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 수동 아카이브 처리 (관리자 전용, 테스트용)
     */
    @PostMapping("/admin/archive")
    public ResponseEntity<Map<String, String>> manualArchive() {
        log.info("수동 아카이브 처리 실행");
        schedulerService.manualArchive();

        Map<String, String> response = new HashMap<>();
        response.put("message", "아카이브 처리가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 크롤러 테스트 (GPT 없이 크롤링만 테스트)
     */
    @GetMapping("/admin/test-crawler")
    public ResponseEntity<Map<String, Object>> testCrawler() {
        log.info("크롤러 테스트 실행 (GPT 비활성화)");

        Map<String, Object> response = new HashMap<>();
        try {
            List<InsightsDailyNewsDTO> crawledNews = schedulerService.testCrawlerOnly();

            response.put("success", true);
            response.put("count", crawledNews.size());
            response.put("news", crawledNews);
            response.put("message", "크롤링 테스트 성공! " + crawledNews.size() + "개 뉴스 발견");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "크롤링 테스트 실패: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 샘플 뉴스 데이터 생성 (테스트용)
     * GET/POST 모두 지원 (브라우저 접근 가능)
     */
    @GetMapping("/admin/create-sample-news")
    public ResponseEntity<Map<String, Object>> createSampleNews() {
        log.info("샘플 뉴스 데이터 생성");

        Map<String, Object> response = new HashMap<>();
        try {
            int count = dailyNewsService.createSampleNews();

            response.put("success", true);
            response.put("count", count);
            response.put("message", count + "개의 샘플 뉴스가 생성되었습니다.");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "샘플 뉴스 생성 실패: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
