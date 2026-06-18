package org.example.ticketbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.ticketbooking.booking.structs.enums.SeatType;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(columnNames = {"screen_id", "row_label", "col_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "col_number", nullable = false)
    private int colNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;
}
