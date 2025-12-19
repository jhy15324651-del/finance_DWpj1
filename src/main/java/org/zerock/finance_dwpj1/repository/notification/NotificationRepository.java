package org.zerock.finance_dwpj1.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.finance_dwpj1.entity.notification.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // 🔔 안 읽은 알림 개수
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    // 🔔 최신 알림 목록 (드롭다운)
    List<Notification> findTop10ByReceiverIdOrderByCreatedDateDesc(Long receiverId);

    // 🔔 알림 읽음 처리 시 소유자 검증
    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    // 🔔 알림 드롭다운에 안읽은 알림만 내려오게 처리
    List<Notification> findTop10ByReceiverIdAndIsReadFalseOrderByCreatedDateDesc(Long receiverId);

    // 🔔 “전체 알림” 조회 메서드 추가
    List<Notification> findByReceiverIdOrderByCreatedDateDesc(Long receiverId);

}
