package org.zerock.finance_dwpj1.scheduler.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.finance_dwpj1.service.admin.AdminContentDeletionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 30일 경과 콘텐츠 자동 하드 삭제 스케줄러
 *
 * 실행 주기: 매일 새벽 3시
 * - 소프트 삭제 후 30일이 지난 뉴스와 콘텐츠 리뷰를 물리 삭제
 * - 감사 로그에 HARD_DELETE 기록
 * - 중복 실행 방지 (Idempotent)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentHardDeleteScheduler {

    private final AdminContentDeletionService deletionService;

    /**
     * 매일 새벽 3시에 실행
     * cron = "초 분 시 일 월 요일"
     * "0 0 3 * * *" = 매일 새벽 3시 0분 0초
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void executeHardDelete() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("========================================");
        log.info("하드 삭제 스케줄러 시작: {}", timestamp);
        log.info("========================================");

        try {
            // 뉴스 하드 삭제
            int deletedNewsCount = deletionService.hardDeleteExpiredNews();
            log.info("뉴스 하드 삭제 완료: {}건", deletedNewsCount);

            // 콘텐츠 리뷰 하드 삭제
            int deletedReviewCount = deletionService.hardDeleteExpiredContentReviews();
            log.info("콘텐츠 리뷰 하드 삭제 완료: {}건", deletedReviewCount);

            log.info("========================================");
            log.info("하드 삭제 스케줄러 정상 종료");
            log.info("총 삭제: 뉴스 {}건, 콘텐츠 리뷰 {}건", deletedNewsCount, deletedReviewCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("하드 삭제 스케줄러 실행 중 오류 발생", e);
            log.error("오류 메시지: {}", e.getMessage());
            log.error("========================================");
        }
    }

    /**
     * 수동 실행용 메소드 (테스트/디버깅용)
     * 필요시 관리자 API에서 호출 가능
     */
    public void executeManually() {
        log.info("[수동 실행] 하드 삭제 스케줄러 시작");
        executeHardDelete();
    }
}