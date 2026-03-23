package com.urbanblack.walletservice.repository;

import com.urbanblack.walletservice.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByCreatedAtBetween(@Param("start") LocalDateTime start, 
                                           @Param("end") LocalDateTime end);

    List<WalletTransaction> findAllByOrderByCreatedAtDesc();

    Page<WalletTransaction> findByBeneficiaryUserOrderByCreatedAtDesc(Long beneficiaryUser, Pageable pageable);

    boolean existsByBeneficiaryUserAndIdempotencyKey(Long beneficiaryUser, String idempotencyKey);
}
