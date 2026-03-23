package com.urbanblack.walletservice.controller;

import com.urbanblack.walletservice.entity.AdminWallet;
import com.urbanblack.walletservice.entity.WalletTransaction;
import com.urbanblack.walletservice.service.WalletService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        AdminWallet admin = walletService.getAdminSummary();
        return ResponseEntity.ok(AdminSummaryResponse.builder()
                .totalProfit(admin.getBalance().doubleValue())
                .totalDistributed(0.0) // This would need a sum query from transactions
                .build());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<?>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(walletService.getAdminTransactions(PageRequest.of(page, size)));
    }

    @PatchMapping("/users/{userId}/adjust")
    public ResponseEntity<?> adjustUserWallet(@PathVariable Long userId, @RequestBody AdjustmentRequest request) {
        walletService.adjustUserWallet(userId, request.getAmount(), request.getType(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/freeze")
    public ResponseEntity<?> freezeUserWallet(@PathVariable Long userId) {
        walletService.freezeWallet(userId, true);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/unfreeze")
    public ResponseEntity<?> unfreezeUserWallet(@PathVariable Long userId) {
        walletService.freezeWallet(userId, false);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/adjust")
    public ResponseEntity<?> adjustAdminWallet(@RequestBody AdjustmentRequest request) {
        walletService.adjustAdminWallet(request.getAmount(), request.getType());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserWalletController.WalletInfoResponse> getUserWalletSummary(@PathVariable Long userId) {
        com.urbanblack.walletservice.entity.Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(UserWalletController.WalletInfoResponse.builder()
                .balance(wallet.getBalance().doubleValue())
                .totalEarned(wallet.getTotalEarned().doubleValue())
                .totalSpent(wallet.getTotalSpent() != null ? wallet.getTotalSpent().doubleValue() : 0.0)
                .lastUpdated(wallet.getLastUpdated() != null ? wallet.getLastUpdated().toString() : "")
                .build());
    }

    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<org.springframework.data.domain.Page<com.urbanblack.walletservice.entity.WalletTransaction>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(walletService.getUserTransactions(userId, org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @Data
    @Builder
    public static class AdminSummaryResponse {
        private Double totalProfit;
        private Double totalDistributed;
    }

    @Data
    public static class AdjustmentRequest {
        private BigDecimal amount;
        private WalletTransaction.TransactionType type;
        private String reason;
    }
}
