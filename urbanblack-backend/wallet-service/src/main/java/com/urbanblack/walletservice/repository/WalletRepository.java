package com.urbanblack.walletservice.repository;

import com.urbanblack.walletservice.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    List<Wallet> findByUserIdIn(List<Long> userIds);
}
