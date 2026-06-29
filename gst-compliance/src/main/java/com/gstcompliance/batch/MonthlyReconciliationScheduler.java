package com.gstcompliance.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReconciliationScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("reconciliationJob")
    private final Job reconciliationJob;

    /**
     * Scheduled job - Runs at 2:00 AM on the 1st day of every month
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void runMonthlyReconciliation() {
        log.info("Starting scheduled monthly reconciliation job...");

        // Unique parameters ensure Spring Batch allows multiple distinct runs
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(reconciliationJob, jobParameters);
            log.info("Monthly reconciliation job launched successfully");
        } catch (Exception e) {
            log.error("Monthly reconciliation job execution failed: {}", e.getMessage(), e);
        }
    }
}