package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "refund_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Minimum hours before show start required for this refund percentage to apply.
     * e.g., hoursBeforeShow=48 means: if cancelling 48+ hours before show, refundPercentage applies.
     * Policies are evaluated in descending order of hoursBeforeShow.
     */
    @Column(nullable = false)
    private int hoursBeforeShow;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    private String description;
}
