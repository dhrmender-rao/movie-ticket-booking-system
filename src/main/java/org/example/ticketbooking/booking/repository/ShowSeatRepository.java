package org.example.ticketbooking.booking.repository;

import jakarta.persistence.LockModeType;
import org.example.ticketbooking.booking.entity.ShowSeat;
import org.example.ticketbooking.booking.structs.enums.ShowSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.id IN :ids")
    List<ShowSeat> findAllByIdWithLock(@Param("ids") List<Long> ids);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.id IN :ids")
    List<ShowSeat> findByShowIdAndIdIn(@Param("showId") Long showId, @Param("ids") List<Long> ids);

    @Modifying
    @Query("""
            UPDATE ShowSeat ss
            SET ss.status = 'AVAILABLE', ss.holdExpiresAt = NULL, ss.heldByUser = NULL
            WHERE ss.status = 'HELD' AND ss.holdExpiresAt < :now
            """)
    int releaseExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.status = :status")
    List<ShowSeat> findByShowIdAndStatus(@Param("showId") Long showId, @Param("status") ShowSeatStatus status);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.status = 'BOOKED'")
    List<ShowSeat> findBookedSeatsByShowId(@Param("showId") Long showId);
}
