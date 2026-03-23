package com.urbanblack.paymentservice.repository;

import com.urbanblack.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findTopByRideIdOrderByCreatedAtDesc(String rideId);
    Optional<Payment> findByTxnId(String txnId);

    List<Payment> findByDriverIdAndStatusOrderByCreatedAtDesc(String driverId, Payment.Status status);

    @Query("SELECT p.driverId, SUM(p.totalAmount), COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS' AND p.driverId IS NOT NULL GROUP BY p.driverId")
    List<Object[]> findDriverCollectionSummary();
}

