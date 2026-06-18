package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ticketbooking.booking.structs.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String paymentMethod;
    private String transactionId;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = PaymentStatus.PENDING;
    }
}
