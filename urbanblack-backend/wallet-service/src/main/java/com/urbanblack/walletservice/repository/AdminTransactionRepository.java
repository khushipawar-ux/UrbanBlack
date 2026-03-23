package com.urbanblack.walletservice.repository;

import com.urbanblack.walletservice.entity.AdminTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AdminTransactionRepository extends JpaRepository<AdminTransaction, Long> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AdminTransaction t WHERE t.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByCreatedAtBetween(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    List<AdminTransaction> findAllByOrderByCreatedAtDesc();
}

