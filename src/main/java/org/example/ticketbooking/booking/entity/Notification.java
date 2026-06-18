package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.booking.structs.enums.NotificationStatus;
import org.example.ticketbooking.booking.structs.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = NotificationStatus.PENDING;
        if (scheduledAt == null) scheduledAt = LocalDateTime.now();
    }
}
