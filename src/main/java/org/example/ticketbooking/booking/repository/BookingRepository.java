package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.Booking;
import org.example.ticketbooking.booking.structs.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.show s
            JOIN FETCH s.movie
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theater t
            JOIN FETCH t.city
            WHERE b.user.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.id = :bookingId")
    Optional<Booking> findByIdAndUserId(@Param("bookingId") Long bookingId, @Param("userId") Long userId);

    List<Booking> findByShowIdAndStatus(Long showId, BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.items i
            JOIN FETCH i.showSeat ss
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithItems(@Param("id") Long id);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.show.id = :showId
              AND b.status = 'CONFIRMED'
            """)
    List<Booking> findConfirmedBookingsByShowId(@Param("showId") Long showId);

    /**
     * Fetches booking with full association chain needed for notifications and responses.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.show s
            JOIN FETCH s.movie
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theater t
            JOIN FETCH t.city
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithFullDetails(@Param("id") Long id);
}
