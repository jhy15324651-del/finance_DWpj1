package org.zerock.finance_dwpj1.controller.insights;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.dto.insights.InsightsTwitterDTO;
import org.zerock.finance_dwpj1.service.insights.TwitterService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 트위터 인사이트 관리자 API 컨트롤러
 */
@RestController
@RequestMapping("/api/twitter/admin")
@RequiredArgsConstructor
@Slf4j
public class TwitterAdminController {

    private final TwitterService twitterService;

    /**
     * 전체 트윗 목록 조회 (관리자용)
     */
    @GetMapping("/tweets")
    public ResponseEntity<List<InsightsTwitterDTO>> getAllTweets() {
        log.info("관리자 - 전체 트윗 조회");
        List<InsightsTwitterDTO> tweets = twitterService.getAllTweets();
        return ResponseEntity.ok(tweets);
    }

    /**
     * 트윗 추가 (관리자용)
     */
    @PostMapping("/tweets")
    public ResponseEntity<InsightsTwitterDTO> addTweet(@RequestBody InsightsTwitterDTO tweetDTO) {
        log.info("관리자 - 트윗 추가: {}", tweetDTO.getHandle());
        InsightsTwitterDTO savedTweet = twitterService.addTweet(tweetDTO);
        return ResponseEntity.ok(savedTweet);
    }

    /**
     * 트윗 수정 (관리자용)
     */
    @PutMapping("/tweets/{tweetId}")
    public ResponseEntity<InsightsTwitterDTO> updateTweet(
            @PathVariable Long tweetId,
            @RequestBody InsightsTwitterDTO tweetDTO) {
        log.info("관리자 - 트윗 수정: ID {}", tweetId);
        InsightsTwitterDTO updatedTweet = twitterService.updateTweet(tweetId, tweetDTO);
        return ResponseEntity.ok(updatedTweet);
    }

    /**
     * 트윗 삭제 (관리자용)
     */
    @DeleteMapping("/tweets/{tweetId}")
    public ResponseEntity<Map<String, String>> deleteTweet(@PathVariable Long tweetId) {
        log.info("관리자 - 트윗 삭제: ID {}", tweetId);
        twitterService.deleteTweet(tweetId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "트윗이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 더미 데이터 초기화 (관리자용, 테스트용)
     * GET 방식으로 브라우저에서 접근 가능
     */
    @GetMapping("/initialize-dummy")
    public ResponseEntity<Map<String, Object>> initializeDummyData() {
        log.info("관리자 - 더미 데이터 초기화");

        Map<String, Object> response = new HashMap<>();
        try {
            int count = twitterService.initializeDummyData();

            response.put("success", true);
            response.put("count", count);
            response.put("message", count > 0
                ? count + "개의 더미 트윗이 데이터베이스에 저장되었습니다."
                : "이미 트윗 데이터가 존재하여 초기화하지 않았습니다.");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "더미 데이터 초기화 실패: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}