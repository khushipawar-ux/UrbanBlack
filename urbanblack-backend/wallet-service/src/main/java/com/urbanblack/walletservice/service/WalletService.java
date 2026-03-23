package com.urbanblack.walletservice.service;

import com.urbanblack.walletservice.entity.AdminTransaction;
import com.urbanblack.walletservice.entity.AdminWallet;
import com.urbanblack.walletservice.entity.Wallet;
import com.urbanblack.walletservice.entity.WalletTransaction;
import com.urbanblack.walletservice.repository.AdminTransactionRepository;
import com.urbanblack.walletservice.repository.AdminWalletRepository;
import com.urbanblack.walletservice.repository.WalletRepository;
import com.urbanblack.walletservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final AdminWalletRepository adminWalletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AdminTransactionRepository adminTransactionRepository;

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .totalEarned(BigDecimal.ZERO)
                        .totalSpent(BigDecimal.ZERO)
                        .build()));
    }

    public Page<WalletTransaction> getUserTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findByBeneficiaryUserOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void batchCredit(List<Long> userIds, BigDecimal amount) {
        if (userIds == null || userIds.isEmpty()) return;

        List<Wallet> wallets = walletRepository.findByUserIdIn(userIds);
        Map<Long, Wallet> walletMap = wallets.stream()
                .collect(Collectors.toMap(Wallet::getUserId, w -> w));

        List<Wallet> toSave = new ArrayList<>();
        for (Long userId : userIds) {
            Wallet wallet = walletMap.get(userId);
            if (wallet == null) {
                wallet = Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .totalEarned(BigDecimal.ZERO)
                        .build();
            }
            wallet.setBalance(wallet.getBalance().add(amount));
            wallet.setTotalEarned(wallet.getTotalEarned().add(amount));
            wallet.setLastUpdated(java.time.LocalDateTime.now());
            toSave.add(wallet);
        }
        walletRepository.saveAll(toSave);
    }

    @Transactional
    public void creditAdmin(BigDecimal amount) {
        AdminWallet adminWallet = getAdminInternal();
        adminWallet.setBalance(adminWallet.getBalance().add(amount));
        adminWalletRepository.save(adminWallet);
    }

    public AdminWallet getAdminSummary() {
        return getAdminInternal();
    }

    public Page<AdminTransaction> getAdminTransactions(Pageable pageable) {
        return adminTransactionRepository.findAll(pageable);
    }

    @Transactional
    public Wallet adjustUserWallet(Long userId, BigDecimal amount, WalletTransaction.TransactionType type, String reason) {
        Wallet wallet = getWalletByUserId(userId);
        if (type == WalletTransaction.TransactionType.CREDIT) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }
        wallet.setLastUpdated(java.time.LocalDateTime.now());
        
        transactionRepository.save(WalletTransaction.builder()
                .beneficiaryUser(userId)
                .type(type)
                .source(WalletTransaction.TransactionSource.ADMIN)
                .amount(amount)
                .referenceId(reason)
                .build());
        
        return walletRepository.save(wallet);
    }

    /**
     * Debits wallet for a ride payment with idempotency protection.
     * If the same idempotencyKey is retried, this method becomes a no-op.
     */
    @Transactional
    public Wallet debitForRide(Long userId, BigDecimal amount, String rideId, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getWalletByUserId(userId);
        }
        if (idempotencyKey != null && transactionRepository.existsByBeneficiaryUserAndIdempotencyKey(userId, idempotencyKey)) {
            return getWalletByUserId(userId);
        }

        Wallet wallet = getWalletByUserId(userId);
        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            throw new IllegalStateException("Wallet is frozen");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setTotalSpent(wallet.getTotalSpent().add(amount));
        wallet.setLastUpdated(java.time.LocalDateTime.now());
        walletRepository.save(wallet);

        transactionRepository.save(WalletTransaction.builder()
                .beneficiaryUser(userId)
                .type(WalletTransaction.TransactionType.DEBIT)
                .source(WalletTransaction.TransactionSource.RIDE_PAYMENT)
                .amount(amount)
                .referenceId(rideId)
                .idempotencyKey(idempotencyKey)
                .build());

        return wallet;
    }

    @Transactional
    public void adjustAdminWallet(BigDecimal amount, WalletTransaction.TransactionType type) {
        AdminWallet admin = getAdminInternal();
        if (type == WalletTransaction.TransactionType.CREDIT) {
            admin.setBalance(admin.getBalance().add(amount));
        } else {
            admin.setBalance(admin.getBalance().subtract(amount));
        }
        adminWalletRepository.save(admin);
    }

    @Transactional
    public void freezeWallet(Long userId, boolean freeze) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setFrozen(freeze);
        wallet.setLastUpdated(java.time.LocalDateTime.now());
        walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debitForOveruse(Long userId, BigDecimal amount, String shiftId, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getWalletByUserId(userId);
        }
        if (idempotencyKey != null && transactionRepository.existsByBeneficiaryUserAndIdempotencyKey(userId, idempotencyKey)) {
            return getWalletByUserId(userId);
        }

        Wallet wallet = getWalletByUserId(userId);
        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            throw new IllegalStateException("Wallet is frozen");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            // If insufficient for overuse, we might still debit and leave them in negative or just zero it out
            // For now, let's keep it consistent: throw exception OR just take what's left.
            // Requirement usually allows negative balance for fees. 
            // Let's stick to standard debit logic for now.
            throw new IllegalStateException("Insufficient wallet balance for KM overuse fee");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setTotalSpent(wallet.getTotalSpent().add(amount));
        wallet.setLastUpdated(java.time.LocalDateTime.now());
        walletRepository.save(wallet);

        transactionRepository.save(WalletTransaction.builder()
                .beneficiaryUser(userId)
                .type(WalletTransaction.TransactionType.DEBIT)
                .source(WalletTransaction.TransactionSource.KM_OVERUSE)
                .amount(amount)
                .referenceId(shiftId)
                .idempotencyKey(idempotencyKey)
                .build());

        return wallet;
    }

    private AdminWallet getAdminInternal() {
        return adminWalletRepository.findAll().stream().findFirst()
                .orElseGet(() -> adminWalletRepository.save(AdminWallet.builder()
                        .balance(BigDecimal.ZERO)
                        .build()));
    }
}
