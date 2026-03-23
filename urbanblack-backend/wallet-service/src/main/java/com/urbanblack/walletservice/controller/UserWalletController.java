package com.urbanblack.walletservice.controller;

import com.urbanblack.walletservice.entity.Wallet;
import com.urbanblack.walletservice.entity.WalletTransaction;
import com.urbanblack.walletservice.service.WalletService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<WalletInfoResponse> getMyWallet(@RequestParam Long userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(WalletInfoResponse.builder()
                .balance(wallet.getBalance().doubleValue())
                .totalEarned(wallet.getTotalEarned().doubleValue())
                .totalSpent(wallet.getTotalSpent() != null ? wallet.getTotalSpent().doubleValue() : 0.0)
                .lastUpdated(wallet.getLastUpdated() != null ? wallet.getLastUpdated().toString() : "")
                .build());
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<Page<TransactionHistoryResponse>> getMyTransactions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<WalletTransaction> txns = walletService.getUserTransactions(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(txns.map(t -> TransactionHistoryResponse.builder()
                .type(t.getType() != null ? t.getType().name() : "CREDIT")
                .source(t.getSource() != null ? t.getSource().name() : "REWARD")
                .uplineLevel(t.getUplineLevel())
                .amount(t.getAmount().doubleValue())
                .build()));
    }

    @Data
    @Builder
    public static class WalletInfoResponse {
        private Double balance;
        private Double totalEarned;
        private Double totalSpent;
        private String lastUpdated;
    }

    @Data
    @Builder
    public static class TransactionHistoryResponse {
        private String type;
        private String source;
        private Integer uplineLevel;
        private Double amount;
    }
}
