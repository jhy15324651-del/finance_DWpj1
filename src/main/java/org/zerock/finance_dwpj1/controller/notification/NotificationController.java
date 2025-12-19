package org.zerock.finance_dwpj1.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.finance_dwpj1.entity.notification.Notification;
import org.zerock.finance_dwpj1.repository.notification.NotificationRepository;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/notifications")
    public String notificationPage(
            @AuthenticationPrincipal CustomUserDetails user,
            Model model
    ) {
        if (user == null) return "redirect:/user/login";

        List<Notification> notifications =
                notificationRepository.findByReceiverIdOrderByCreatedDateDesc(user.getId());

        model.addAttribute("notifications", notifications);

        return "notification/notification-list";

    }

    @GetMapping("/notifications/{id}")
    public String readAndRedirect(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return "redirect:/user/login";

        Notification notification = notificationRepository
                .findByIdAndReceiverId(id, user.getId())
                .orElseThrow();

        // 읽음 처리
        notification.markAsRead();
        notificationRepository.save(notification);

        // 원래 목적지로 이동
        return "redirect:" + notification.getTargetUrl();
    }



}
