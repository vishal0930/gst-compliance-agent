package com.gstcompliance.service;

import com.gstcompliance.cache.DashboardCacheService;
import com.gstcompliance.cache.HsnCacheService;
import com.gstcompliance.cache.VendorCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledJobService {

    private final DeadlineTrackerService deadlineTrackerService;
    private final PenaltyCalculationService penaltyCalculationService;
    private final NotificationService notificationService;
    private final DashboardCacheService dashboardCacheService;
    private final HsnCacheService hsnCacheService;
    private final VendorCacheService vendorCacheService;

    /**
     * Daily job at 02:00 AM
     * - Track deadlines and send notifications
     * - Calculate penalties for overdue returns
     * - Refresh dashboard cache
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyDeadlineAndPenaltyJob() {
        log.info("=== STARTING DAILY DEADLINE & PENALTY JOB ===");
        
        try {
            // Track deadlines and send notifications
            log.info("Step 1: Tracking deadlines...");
            deadlineTrackerService.trackDeadlines();
            
            // Calculate penalties for overdue returns
            log.info("Step 2: Calculating penalties...");
            // Penalty calculation is handled within deadline tracker
            
            // Refresh dashboard cache
            log.info("Step 3: Refreshing dashboard cache...");
            dashboardCacheService.evictAll();
            
            log.info("=== COMPLETED DAILY DEADLINE & PENALTY JOB ===");
        } catch (Exception e) {
            log.error("Error in daily deadline & penalty job: {}", e.getMessage(), e);
        }
    }

    /**
     * Hourly job
     * - Clear expired cache entries
     * - Clean up expired notifications
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyCacheCleanupJob() {
        log.info("=== STARTING HOURLY CACHE CLEANUP JOB ===");
        
        try {
            // Clean up expired notifications
            log.info("Cleaning up expired notifications...");
            notificationService.cleanupExpiredNotifications();
            
            log.info("=== COMPLETED HOURLY CACHE CLEANUP JOB ===");
        } catch (Exception e) {
            log.error("Error in hourly cache cleanup job: {}", e.getMessage(), e);
        }
    }

    /**
     * Weekly job (every Sunday at 03:00 AM)
     * - Clean up old HSN cache entries
     * - Clean up old vendor cache entries
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void weeklyCacheCleanupJob() {
        log.info("=== STARTING WEEKLY CACHE CLEANUP JOB ===");
        
        try {
            // Evict all HSN cache (will be rebuilt on demand)
            log.info("Evicting HSN cache...");
            hsnCacheService.evictAll();
            
            // Evict all vendor cache (will be rebuilt on demand)
            log.info("Evicting vendor cache...");
            vendorCacheService.evictAll();
            
            log.info("=== COMPLETED WEEKLY CACHE CLEANUP JOB ===");
        } catch (Exception e) {
            log.error("Error in weekly cache cleanup job: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing
     */
    public void triggerDailyJob() {
        log.info("Manually triggering daily job at {}", LocalDateTime.now());
        dailyDeadlineAndPenaltyJob();
    }

    /**
     * Manual trigger for testing
     */
    public void triggerHourlyJob() {
        log.info("Manually triggering hourly job at {}", LocalDateTime.now());
        hourlyCacheCleanupJob();
    }

    /**
     * Manual trigger for testing
     */
    public void triggerWeeklyJob() {
        log.info("Manually triggering weekly job at {}", LocalDateTime.now());
        weeklyCacheCleanupJob();
    }
}
