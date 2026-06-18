package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.Notification;
import org.example.ticketbooking.booking.structs.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.status = 'PENDING'
              AND n.scheduledAt <= :now
            """)
    List<Notification> findPendingDueNotifications(@Param("now") LocalDateTime now);

    boolean existsByBookingIdAndType(Long bookingId, NotificationType type);

    List<Notification> findByBookingId(Long bookingId);
}
