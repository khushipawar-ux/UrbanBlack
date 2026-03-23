package com.urbanblack.notification.repository;

import com.urbanblack.notification.entity.NotificationLog;
import com.urbanblack.notification.entity.enums.NotificationStatus;
import com.urbanblack.notification.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByRecipientEmailOrderByCreatedAtDesc(String email);

    List<NotificationLog> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    List<NotificationLog> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<NotificationLog> findBySourceServiceOrderByCreatedAtDesc(String sourceService);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);
}
