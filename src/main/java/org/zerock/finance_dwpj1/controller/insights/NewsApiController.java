package org.zerock.finance_dwpj1.controller.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.insights.InsightsNewsDTO;
import org.zerock.finance_dwpj1.service.insights.NewsScrapingService;
import org.zerock.finance_dwpj1.service.insights.NewsSchedulerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Log4j2
public class NewsApiController {

    private final NewsScrapingService newsScrapingService;
    private final NewsSchedulerService newsSchedulerService;

    @GetMapping("/{category}")
    public List<InsightsNewsDTO> getNews(@PathVariable String category) {
        log.info("뉴스 요청: " + category);
        return newsScrapingService.scrapeYahooFinanceNews(category);
    }

    // ========== 관리자용 크롤링 스케줄러 제어 API ==========

    /**
     * 크롤링 스케줄러 시작
     * GET /api/news/admin/scheduler/start
     */
    @GetMapping("/admin/scheduler/start")
    public ResponseEntity<Map<String, Object>> startScheduler() {
        log.info("크롤링 스케줄러 시작 요청");

        Map<String, Object> response = new HashMap<>();

        if (newsSchedulerService.isSchedulerEnabled()) {
            response.put("success", false);
            response.put("message", "스케줄러가 이미 실행 중입니다");
            return ResponseEntity.ok(response);
        }

        newsSchedulerService.startScheduler();

        response.put("success", true);
        response.put("message", "크롤링 스케줄러가 시작되었습니다");
        response.put("status", newsSchedulerService.getSchedulerStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * 크롤링 스케줄러 정지
     * GET /api/news/admin/scheduler/stop
     */
    @GetMapping("/admin/scheduler/stop")
    public ResponseEntity<Map<String, Object>> stopScheduler() {
        log.info("크롤링 스케줄러 정지 요청");

        Map<String, Object> response = new HashMap<>();

        if (!newsSchedulerService.isSchedulerEnabled()) {
            response.put("success", false);
            response.put("message", "스케줄러가 이미 정지되어 있습니다");
            return ResponseEntity.ok(response);
        }

        newsSchedulerService.stopScheduler();

        response.put("success", true);
        response.put("message", "크롤링 스케줄러가 정지되었습니다");
        response.put("status", newsSchedulerService.getSchedulerStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * 크롤링 스케줄러 상태 조회
     * GET /api/news/admin/scheduler/status
     */
    @GetMapping("/admin/scheduler/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        log.info("크롤링 스케줄러 상태 조회 요청");

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", newsSchedulerService.isSchedulerEnabled());
        response.put("status", newsSchedulerService.getSchedulerStatus());
        response.put("schedule", "매일 오전 10시 (크롤링), 매일 오전 10시 10분 (아카이브)");

        return ResponseEntity.ok(response);
    }

    /**
     * 수동 크롤링 실행
     * POST /api/news/admin/manual-crawl
     */
    @PostMapping("/admin/manual-crawl")
    public ResponseEntity<Map<String, Object>> manualCrawl() {
        log.info("수동 크롤링 요청");

        Map<String, Object> response = new HashMap<>();

        try {
            newsSchedulerService.manualCrawl();
            response.put("success", true);
            response.put("message", "수동 크롤링이 완료되었습니다");
        } catch (Exception e) {
            log.error("수동 크롤링 실패", e);
            response.put("success", false);
            response.put("message", "크롤링 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 수동 아카이브 처리 실행
     * POST /api/news/admin/manual-archive
     */
    @PostMapping("/admin/manual-archive")
    public ResponseEntity<Map<String, Object>> manualArchive() {
        log.info("수동 아카이브 처리 요청");

        Map<String, Object> response = new HashMap<>();

        try {
            newsSchedulerService.manualArchive();
            response.put("success", true);
            response.put("message", "수동 아카이브 처리가 완료되었습니다");
        } catch (Exception e) {
            log.error("수동 아카이브 처리 실패", e);
            response.put("success", false);
            response.put("message", "아카이브 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}