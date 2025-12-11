package org.zerock.finance_dwpj1.scheduler.content;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContentCleanupScheduler {

    private final ContentReviewRepository repository;

    // 🕒 매일 새벽 3시에 실행 (cron은 필요하면 나중에 바꿀 수 있음)
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredPosts() {

        LocalDateTime limit = LocalDateTime.now().minusDays(7);

        var expiredPosts = repository.findByIsDeletedTrueAndDeletedAtBefore(limit);

        if (!expiredPosts.isEmpty()) {
            repository.deleteAll(expiredPosts);
            System.out.println("🗑 자동 삭제: " + expiredPosts.size() + "건 처리 완료");
        }
    }
}
