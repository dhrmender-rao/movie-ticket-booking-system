package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ticketbooking.booking.structs.enums.DayType;
import org.example.ticketbooking.booking.structs.enums.SeatType;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_tiers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_type", "day_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
}
