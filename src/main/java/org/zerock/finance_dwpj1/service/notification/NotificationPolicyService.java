package org.zerock.finance_dwpj1.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.entity.notification.NotificationType;
import org.zerock.finance_dwpj1.entity.user.User;
import org.zerock.finance_dwpj1.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
public class NotificationPolicyService {

    private final UserRepository userRepository;

    public boolean canSendNotification(Long receiverId, NotificationType type) {

        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null) return false;

        // 🔕 알림 OFF + 공지가 아니면 차단
        if (!receiver.getNotificationEnabled()
                && type != NotificationType.NOTICE) {
            return false;
        }

        return true;
    }
}

