package org.zerock.finance_dwpj1.controller.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.zerock.finance_dwpj1.entity.user.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.zerock.finance_dwpj1.entity.notification.Notification;
import org.zerock.finance_dwpj1.repository.notification.NotificationRepository;
import org.zerock.finance_dwpj1.repository.user.UserRepository;
import org.zerock.finance_dwpj1.service.user.CustomUserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping("/notifications")
    public String notificationPage(
            @AuthenticationPrincipal CustomUserDetails user,
            Model model) {

        if (user == null) return "redirect:/user/login";

        List<Notification> notifications =
                notificationRepository.findByReceiverIdOrderByCreatedDateDesc(user.getId());
        model.addAttribute("notifications", notifications);

        User entity = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));

        model.addAttribute("interestedTags", entity.getInterestedTags());
        model.addAttribute("notificationEnabled", entity.getNotificationEnabled()); // ✅ 추가

        return "notification/notification-list";
    }

    @GetMapping("/notifications/{id}")
    public String readAndRedirect(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) return "redirect:/user/login";

        Notification notification = notificationRepository
                .findByIdAndReceiverId(id, user.getId())
                .orElseThrow();

        notification.markAsRead();
        notificationRepository.save(notification);

        return "redirect:" + notification.getTargetUrl();
    }

    @PostMapping("/notifications/tags")
    public String updateInterestedTags(
            @AuthenticationPrincipal CustomUserDetails user,
            String interestedTags) {

        if (user == null) return "redirect:/user/login";

        User entity = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));

        entity.setInterestedTags(normalizeTags(interestedTags));
        userRepository.save(entity);

        return "redirect:/notifications";
    }

    private String normalizeTags(String input) {
        if (input == null || input.isBlank()) return "";

        return Arrays.stream(input.split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceAll("^#+", "#"))
                .map(s -> s.startsWith("#") ? s : "#" + s)
                .distinct()
                .limit(5)
                .collect(Collectors.joining(" "));
    }

    @PostMapping("/notifications/toggle")
    public String toggleNotification(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) return "redirect:/user/login";

        User entity = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));

        // ⭐ 핵심: 현재 값 반전
        entity.setNotificationEnabled(
                !entity.getNotificationEnabled()
        );

        userRepository.save(entity);

        return "redirect:/notifications";
    }

}

