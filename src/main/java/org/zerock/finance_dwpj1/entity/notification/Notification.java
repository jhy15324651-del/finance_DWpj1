package org.zerock.finance_dwpj1.entity.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.zerock.finance_dwpj1.entity.notification.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 받는 유저
    @Column(nullable = false)
    private Long receiverId;

    // 알림 종류 (COMMENT / TAG / NOTICE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 알림 메시지
    @Column(nullable = false, length = 200)
    private String message;

    // 클릭 시 이동할 URL
    @Column(nullable = false, length = 300)
    private String targetUrl;

    // 읽음 여부
    @Column(nullable = false)
    private boolean isRead;

    // 생성 시간
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @Builder
    public Notification(Long receiverId,
                        NotificationType type,
                        String message,
                        String targetUrl) {
        this.receiverId = receiverId;
        this.type = type;
        this.message = message;
        this.targetUrl = targetUrl;
        this.isRead = false; // ⭐ 기본값 강제
    }

    // ===== 비즈니스 메서드 =====

    public void markAsRead() {
        this.isRead = true;
    }
}
