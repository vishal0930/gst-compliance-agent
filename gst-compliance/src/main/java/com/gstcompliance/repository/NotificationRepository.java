package com.gstcompliance.repository;

import com.gstcompliance.model.Notification;
import com.gstcompliance.model.Notification.NotificationStatus;
import com.gstcompliance.model.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status, Pageable pageable);

    List<Notification> findByUserIdAndStatusAndCreatedAtAfter(
            UUID userId,
            NotificationStatus status,
            LocalDateTime after
    );

    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.status = :status AND n.expiresAt IS NULL OR n.expiresAt > :now")
    List<Notification> findActiveByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("type") NotificationType type
    );

    void deleteByExpiresAtBefore(LocalDateTime date);
}
