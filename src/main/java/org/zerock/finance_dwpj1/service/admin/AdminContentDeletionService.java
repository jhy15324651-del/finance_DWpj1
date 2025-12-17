package org.zerock.finance_dwpj1.service.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.finance_dwpj1.entity.audit.AuditDeleteLog;
import org.zerock.finance_dwpj1.entity.content.ContentReview;
import org.zerock.finance_dwpj1.entity.insights.InsightsNews;
import org.zerock.finance_dwpj1.entity.user.Role;
import org.zerock.finance_dwpj1.repository.audit.AuditDeleteLogRepository;
import org.zerock.finance_dwpj1.repository.content.ContentReviewRepository;
import org.zerock.finance_dwpj1.repository.insights.InsightsNewsRepository;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 전용 콘텐츠 삭제 서비스
 * - 소프트 삭제 처리
 * - 감사 로그 기록
 * - 하드 삭제 (스케줄러 호출용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminContentDeletionService {

    private final InsightsNewsRepository newsRepository;
    private final ContentReviewRepository contentReviewRepository;
    private final AuditDeleteLogRepository auditLogRepository;

    /**
     * 뉴스 소프트 삭제 (관리자용)
     */
    @Transactional
    public void softDeleteNews(Long newsId, String deleteReason, HttpServletRequest request) {
        InsightsNews news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: " + newsId));

        // 이미 삭제된 경우 예외 발생
        if (news.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 뉴스입니다: " + newsId);
        }

        // 관리자 정보 추출
        String adminEmail = getCurrentAdminEmail();
        Long adminUserId = getCurrentAdminUserId();
        String adminRole = "ROLE_ADMIN";

        // 소프트 삭제 실행
        news.softDelete(adminEmail, deleteReason);
        newsRepository.save(news);

        // 감사 로그 기록
        AuditDeleteLog auditLog = AuditDeleteLog.createSoftDeleteLog(
                AuditDeleteLog.TargetType.NEWS,
                newsId,
                news.getTitle(),
                adminUserId,
                adminEmail,
                adminRole,
                deleteReason,
                getClientIp(request),
                request.getHeader("User-Agent")
        );
        auditLogRepository.save(auditLog);

        log.info("뉴스 소프트 삭제 완료 - ID: {}, 관리자: {}, 사유: {}", newsId, adminEmail, deleteReason);
    }

    /**
     * 콘텐츠 리뷰 소프트 삭제 (관리자용)
     */
    @Transactional
    public void softDeleteContentReview(Long reviewId, String deleteReason, HttpServletRequest request) {
        ContentReview review = contentReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠 리뷰를 찾을 수 없습니다: " + reviewId));

        // 이미 삭제된 경우 예외 발생
        if (review.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 콘텐츠 리뷰입니다: " + reviewId);
        }

        // 관리자 정보 추출
        String adminEmail = getCurrentAdminEmail();
        Long adminUserId = getCurrentAdminUserId();
        String adminRole = "ROLE_ADMIN";

        // 소프트 삭제 실행
        review.softDelete(adminEmail, deleteReason);
        contentReviewRepository.save(review);

        // 감사 로그 기록
        AuditDeleteLog auditLog = AuditDeleteLog.createSoftDeleteLog(
                AuditDeleteLog.TargetType.CONTENT_REVIEW,
                reviewId,
                review.getTitle(),
                adminUserId,
                adminEmail,
                adminRole,
                deleteReason,
                getClientIp(request),
                request.getHeader("User-Agent")
        );
        auditLogRepository.save(auditLog);

        log.info("콘텐츠 리뷰 소프트 삭제 완료 - ID: {}, 관리자: {}, 사유: {}", reviewId, adminEmail, deleteReason);
    }

    /**
     * 뉴스 하드 삭제 (30일 경과 후 자동 실행용)
     */
    @Transactional
    public int hardDeleteExpiredNews() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<InsightsNews> expiredNews = newsRepository.findAll().stream()
                .filter(news -> news.getIsDeleted() != null && news.getIsDeleted())
                .filter(news -> news.getDeletedAt() != null)
                .filter(news -> news.getDeletedAt().isBefore(thirtyDaysAgo))
                .toList();

        if (expiredNews.isEmpty()) {
            log.info("하드 삭제할 뉴스가 없습니다.");
            return 0;
        }

        int count = 0;
        for (InsightsNews news : expiredNews) {
            // 감사 로그 기록 (하드 삭제)
            AuditDeleteLog auditLog = AuditDeleteLog.createHardDeleteLog(
                    AuditDeleteLog.TargetType.NEWS,
                    news.getId(),
                    news.getTitle()
            );
            auditLogRepository.save(auditLog);

            // 물리 삭제
            newsRepository.delete(news);
            count++;

            log.info("뉴스 하드 삭제 완료 - ID: {}, 제목: {}", news.getId(), news.getTitle());
        }

        log.info("총 {}개의 뉴스가 하드 삭제되었습니다.", count);
        return count;
    }

    /**
     * 콘텐츠 리뷰 하드 삭제 (30일 경과 후 자동 실행용)
     */
    @Transactional
    public int hardDeleteExpiredContentReviews() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<ContentReview> expiredReviews = contentReviewRepository
                .findByIsDeletedTrueAndDeletedAtBefore(thirtyDaysAgo);

        if (expiredReviews.isEmpty()) {
            log.info("하드 삭제할 콘텐츠 리뷰가 없습니다.");
            return 0;
        }

        int count = 0;
        for (ContentReview review : expiredReviews) {
            // 감사 로그 기록 (하드 삭제)
            AuditDeleteLog auditLog = AuditDeleteLog.createHardDeleteLog(
                    AuditDeleteLog.TargetType.CONTENT_REVIEW,
                    review.getId(),
                    review.getTitle()
            );
            auditLogRepository.save(auditLog);

            // 물리 삭제
            contentReviewRepository.delete(review);
            count++;

            log.info("콘텐츠 리뷰 하드 삭제 완료 - ID: {}, 제목: {}", review.getId(), review.getTitle());
        }

        log.info("총 {}개의 콘텐츠 리뷰가 하드 삭제되었습니다.", count);
        return count;
    }

    /**
     * 관리자 목록 조회용 - 전체 뉴스 (삭제 포함)
     */
    @Transactional(readOnly = true)
    public List<InsightsNews> getAllNewsForAdmin() {
        return newsRepository.findAll();
    }

    /**
     * 관리자 목록 조회용 - 전체 콘텐츠 리뷰 (삭제 포함)
     */
    @Transactional(readOnly = true)
    public List<ContentReview> getAllContentReviewsForAdmin() {
        return contentReviewRepository.findAll();
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * 현재 로그인한 관리자의 이메일 추출
     */
    private String getCurrentAdminEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                return userDetails.getUsername(); // 이메일 반환
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }

        // 개발 모드에서 인증 없이 접근하는 경우
        return "ADMIN_UNKNOWN";
    }

    /**
     * 현재 로그인한 관리자의 User ID 추출
     */
    private Long getCurrentAdminUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                return userDetails.getId();  // ✅ getUserId() → getId()로 수정
            }
        }

        return null;
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}