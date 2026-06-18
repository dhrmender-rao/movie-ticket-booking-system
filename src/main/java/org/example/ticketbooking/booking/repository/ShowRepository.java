package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.Show;
import org.example.ticketbooking.booking.structs.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
            SELECT s FROM Show s
            JOIN FETCH s.movie
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theater t
            JOIN FETCH t.city
            WHERE t.city.id = :cityId
              AND s.status = :status
              AND s.startTime >= :from
              AND s.startTime < :to
            ORDER BY s.startTime
            """)
    List<Show> findByCityIdAndStatusAndStartTimeBetween(
            @Param("cityId") Long cityId,
            @Param("status") ShowStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT s FROM Show s
            JOIN FETCH s.movie
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theater t
            JOIN FETCH t.city
            WHERE t.city.id = :cityId
              AND s.movie.id = :movieId
              AND s.status = :status
              AND s.startTime >= :from
            ORDER BY s.startTime
            """)
    List<Show> findByCityIdAndMovieIdAndStatus(
            @Param("cityId") Long cityId,
            @Param("movieId") Long movieId,
            @Param("status") ShowStatus status,
            @Param("from") LocalDateTime from);

    List<Show> findByStatus(ShowStatus status);

    @Query("""
            SELECT s FROM Show s
            JOIN FETCH s.movie
            JOIN FETCH s.screen sc
            JOIN FETCH sc.theater t
            JOIN FETCH t.city
            WHERE s.id = :id
            """)
    Optional<Show> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT s FROM Show s
            WHERE s.status = 'ACTIVE'
              AND s.startTime BETWEEN :from AND :to
            """)
    List<Show> findActiveShowsStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
