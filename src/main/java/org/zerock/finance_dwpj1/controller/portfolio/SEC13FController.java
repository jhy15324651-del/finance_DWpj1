package org.zerock.finance_dwpj1.controller.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.service.portfolio.SEC13FService;

import java.util.HashMap;
import java.util.Map;

/**
 * SEC 13F 데이터 수집 컨트롤러
 * 시작/중단/상태 확인 API 제공
 */
@RestController
@RequestMapping("/api/13f")
@RequiredArgsConstructor
@Slf4j
public class SEC13FController {

    private final SEC13FService sec13FService;

    /**
     * 전체 투자자 13F 데이터 수집 시작
     */
    @PostMapping("/start")
    public ResponseEntity<?> startCollection() {
        log.info("📊 13F 데이터 수집 시작 요청");

        try {
            sec13FService.startAsyncCollection();
            return ResponseEntity.ok(createResponse(true, "13F 데이터 수집이 시작되었습니다"));
        } catch (Exception e) {
            log.error("13F 데이터 수집 시작 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createResponse(false, "수집 시작 실패: " + e.getMessage()));
        }
    }

    /**
     * 13F 데이터 수집 중단
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopCollection() {
        log.info("🛑 13F 데이터 수집 중단 요청");

        try {
            sec13FService.stopCollection();
            return ResponseEntity.ok(createResponse(true, "13F 데이터 수집 중단 요청이 전송되었습니다"));
        } catch (Exception e) {
            log.error("13F 데이터 수집 중단 실패", e);
            return ResponseEntity.internalServerError()
                    .body(createResponse(false, "중단 실패: " + e.getMessage()));
        }
    }

    /**
     * 현재 실행 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean isRunning = sec13FService.isCollecting();

        Map<String, Object> status = new HashMap<>();
        status.put("success", true);
        status.put("isRunning", isRunning);
        status.put("message", isRunning ? "수집 진행 중" : "대기 중");

        return ResponseEntity.ok(status);
    }

    /**
     * 개별 투자자 13F 데이터 수집
     */
    @PostMapping("/fetch/{investorId}")
    public ResponseEntity<?> fetchInvestorData(@PathVariable String investorId) {
        log.info("📊 {} 투자자 13F 데이터 수집 요청", investorId);

        try {
            int count = sec13FService.fetch13FDataForInvestor(investorId);
            return ResponseEntity.ok(createResponse(true,
                    String.format("%s의 13F 데이터 %d건 수집 완료", investorId, count)));
        } catch (Exception e) {
            log.error("{} 투자자 데이터 수집 실패", investorId, e);
            return ResponseEntity.internalServerError()
                    .body(createResponse(false, "수집 실패: " + e.getMessage()));
        }
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