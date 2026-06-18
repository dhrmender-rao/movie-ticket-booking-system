package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    List<Theater> findByCityId(Long cityId);
}
