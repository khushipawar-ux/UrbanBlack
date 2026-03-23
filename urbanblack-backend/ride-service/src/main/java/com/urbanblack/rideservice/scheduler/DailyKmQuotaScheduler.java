package com.urbanblack.rideservice.scheduler;

import com.urbanblack.rideservice.repository.DriverKmLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyKmQuotaScheduler {

    private final DriverKmLedgerRepository ledgerRepository;

    /**
     * Midnight job placeholder – in a full implementation this would
     * recompute next-day quotas based on previous day overuse.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void recomputeQuotas() {
        // Logic is handled during shift end in this simplified version.
        // This scheduler keeps the hook in place for future expansion.
    }
}

