package org.zerock.finance_dwpj1.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.zerock.finance_dwpj1.entity.notification.Notification;
import org.zerock.finance_dwpj1.repository.notification.NotificationRepository;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    // unread-count
    // latest
    // read

    private final NotificationRepository notificationRepository;

    /**
     * 🔔 안 읽은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public long getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return 0;
        return notificationRepository
                .countByReceiverIdAndIsReadFalse(user.getId());
    }

    @GetMapping("/latest")
    public List<Notification> getLatestNotifications(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return List.of();

        return notificationRepository
                .findTop10ByReceiverIdAndIsReadFalseOrderByCreatedDateDesc(user.getId());
    }

    //알림 “읽음 처리” API
    @PostMapping("/{id}/read")
    public void readNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return;

        Notification notification = notificationRepository
                .findByIdAndReceiverId(id, user.getId())
                .orElseThrow();

        notification.markAsRead();
        notificationRepository.save(notification);
    }

}

