package com.urbanblack.walletservice.repository;

import com.urbanblack.walletservice.entity.AdminWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminWalletRepository extends JpaRepository<AdminWallet, Long> {
}
