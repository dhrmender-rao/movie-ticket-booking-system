package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ticketbooking.booking.structs.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discount_codes", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int usedCount;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (!active) active = true;
        if (usedCount == 0) usedCount = 0;
    }
}
