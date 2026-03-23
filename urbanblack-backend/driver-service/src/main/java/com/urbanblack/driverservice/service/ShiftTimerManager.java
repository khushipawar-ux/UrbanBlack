package com.urbanblack.driverservice.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages scheduled auto-clock-out timers per shift.
 * Each shift gets a timer that fires after the remaining allowed seconds.
 */
@Component
public class ShiftTimerManager {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    /**
     * Schedule a task to run after {@code delaySeconds} seconds.
     * If a timer already exists for this shiftId it is cancelled first.
     *
     * @param shiftId      unique shift identifier
     * @param task         the runnable to execute (auto-complete the shift)
     * @param delaySeconds seconds until the timer fires
     */
    public void schedule(String shiftId, Runnable task, long delaySeconds) {
        cancel(shiftId); // cancel any existing timer for this shift
        ScheduledFuture<?> future = scheduler.schedule(task, delaySeconds, TimeUnit.SECONDS);
        timers.put(shiftId, future);
    }

    /**
     * Cancel and remove the timer for the given shift (if any).
     *
     * @param shiftId unique shift identifier
     */
    public void cancel(String shiftId) {
        ScheduledFuture<?> future = timers.remove(shiftId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
}