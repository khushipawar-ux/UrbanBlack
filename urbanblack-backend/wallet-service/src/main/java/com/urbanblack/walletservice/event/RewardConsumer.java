package com.urbanblack.walletservice.event;

import com.urbanblack.common.dto.RewardEvent;
import com.urbanblack.common.dto.UplineRecord;
import com.urbanblack.walletservice.entity.AdminTransaction;
import com.urbanblack.walletservice.entity.WalletTransaction;
import com.urbanblack.walletservice.repository.AdminTransactionRepository;
import com.urbanblack.walletservice.repository.WalletTransactionRepository;
import com.urbanblack.walletservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardConsumer {

    private final WalletTransactionRepository txnRepo;
    private final AdminTransactionRepository adminTxnRepo;
    private final WalletService walletService;

    @RabbitListener(queues = "reward.queue")
    @Transactional
    public void consumeRewardEvent(RewardEvent event) {
        log.info("Consuming reward event for node: {}", event.getTriggeringNodeId());

        List<UplineRecord> uplines = event.getUplines();
        if (uplines == null) uplines = new ArrayList<>();

        BigDecimal rewardPerLevel = event.getRewardPerLevel() != null 
                ? BigDecimal.valueOf(event.getRewardPerLevel()) : BigDecimal.ONE;
        BigDecimal totalPool = event.getTotalDeduction() != null
                ? BigDecimal.valueOf(event.getTotalDeduction()) : new BigDecimal("10");

        BigDecimal carryOver = BigDecimal.ZERO;
        List<WalletTransaction> txns = new ArrayList<>();
        java.util.Map<Long, BigDecimal> credits = new java.util.HashMap<>();

        for (UplineRecord u : uplines) {
            BigDecimal currentReward = rewardPerLevel.add(carryOver);
            carryOver = BigDecimal.ZERO;

            if (Boolean.TRUE.equals(u.getActive())) {
                // Check Wallet Cap Rule (New: 1022 Rule)
                com.urbanblack.walletservice.entity.Wallet wallet = walletService.getWalletByUserId(u.getUserId());
                BigDecimal currentBalance = wallet.getBalance();
                BigDecimal threshold = new BigDecimal("1022.0");

                if (currentBalance.compareTo(threshold) >= 0) {
                    log.info("[RewardConsumer] Wallet Cap Reached (>= 1022) for user {}. Rolling up ₹{} to next upliner.", 
                            u.getUserId(), currentReward);
                    carryOver = currentReward; // Pass total amount to next level
                    continue; 
                }

                txns.add(WalletTransaction.builder()
                        .triggeringNode(event.getTriggeringNodeId())
                        .beneficiaryNode(u.getNodeId())
                        .beneficiaryUser(u.getUserId())
                        .uplineLevel(u.getLevel())
                        .type(WalletTransaction.TransactionType.CREDIT)
                        .source(WalletTransaction.TransactionSource.REWARD)
                        .amount(currentReward)
                        .build());
                
                credits.put(u.getUserId(), credits.getOrDefault(u.getUserId(), BigDecimal.ZERO).add(currentReward));
            } else {
                log.info("Diverting ₹{} roll-up amount for inactive node {} to next level.", currentReward, u.getNodeId());
                carryOver = currentReward;
            }
        }

        if (!txns.isEmpty()) {
            txnRepo.saveAll(txns);
            credits.forEach((userId, amount) -> walletService.adjustUserWallet(
                    userId, amount, WalletTransaction.TransactionType.CREDIT, "ROLLUP_REWARD_" + event.getRideId())
            );
        }

        // Final Admin Calculation: Fixed commission + anything that rolled up past Level 9
        BigDecimal distributedAmount = txns.stream()
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal adminAmount = totalPool.subtract(distributedAmount);

        if (adminAmount.compareTo(BigDecimal.ZERO) > 0) {
            adminTxnRepo.save(AdminTransaction.builder()
                    .triggeringNode(event.getTriggeringNodeId())
                    .amount(adminAmount)
                    .build());
            walletService.creditAdmin(adminAmount);
        }

        log.info("Reward distribution completed. UplineRewardsPaid={}, Distributed={}, Admin={}",
                uplines.size(), distributedAmount, adminAmount);
    }
}
