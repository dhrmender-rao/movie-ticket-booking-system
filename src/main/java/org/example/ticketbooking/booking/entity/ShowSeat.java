package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ticketbooking.authanduser.entity.User;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "show_seats", uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowSeatStatus status;

    private LocalDateTime holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = ShowSeatStatus.AVAILABLE;
    }
}
