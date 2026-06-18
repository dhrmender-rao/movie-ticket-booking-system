package org.example.ticketbooking.booking.repository;

import org.example.ticketbooking.booking.entity.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
    List<RefundPolicy> findAllByOrderByHoursBeforeShowDesc();
}
