package com.gstcompliance.batch;

import com.gstcompliance.agent.Gstr2bReconcilerAgent;
import com.gstcompliance.agent.base.AgentResult;
import com.gstcompliance.dto.request.ReconcileRequest;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.model.ReconciliationRecord;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.ReconciliationRepository;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class MonthlyReconciliationBatchConfig {

    private final UserRepository userRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final Gstr2bReconcilerAgent reconcilerAgent;

    /**
     * Main job definition
     */
    @Bean(name = "reconciliationJob")
    public Job reconciliationJob(JobRepository jobRepository, Step monthlyReconciliationStep) {
        return new JobBuilder("reconciliationJob", jobRepository)
                .start(monthlyReconciliationStep)
                .build();
    }

    /**
     * Step that processes all users in chunks
     */
    @Bean
    public Step monthlyReconciliationStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          ItemReader<User> userReader,
                                          ItemProcessor<User, ReconciliationRecord> reconciliationProcessor,
                                          ItemWriter<ReconciliationRecord> reconciliationWriter) {
        return new StepBuilder("monthlyReconciliationStep", jobRepository)
                .<User, ReconciliationRecord>chunk(10, transactionManager)
                .reader(userReader)
                .processor(reconciliationProcessor)
                .writer(reconciliationWriter)
                .build();
    }

    /**
     * Reader that loads all active users
     */
    @Bean
    public ItemReader<User> userReader() {
        return new ItemReader<User>() {
            private List<User> users;
            private int index = 0;

            @Override
            public User read() {
                if (users == null) {
                    users = userRepository.findAll();
                    log.info("Loaded {} active users for reconciliation", users.size());
                }

                if (index < users.size()) {
                    return users.get(index++);
                }
                return null;
            }
        };
    }

    /**
     * Processor that performs reconciliation for each user
     */
    @Bean
    public ItemProcessor<User, ReconciliationRecord> reconciliationProcessor() {
        return user -> {
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            int month = previousMonth.getMonthValue();
            int year = previousMonth.getYear();
            String taxPeriod = String.format("%02d-%04d", month, year);

            log.info("Processing reconciliation for user: {}, period: {}", user.getEmail(), taxPeriod);

            ReconciliationRecord record = new ReconciliationRecord();
            record.setUser(user);
            record.setTaxPeriod(taxPeriod);
            record.setStatus(ReconciliationRecord.Status.RUNNING);
            record.setStartedAt(LocalDateTime.now());

            try {
                ReconcileRequest request = ReconcileRequest.builder()
                        .userEmail(user.getEmail())
                        .month(month)
                        .year(year)
                        .build();

                AgentResult<ReconciliationResponse> result = reconcilerAgent.execute(request);

                if (!result.isSuccess()) {
                    throw new RuntimeException(result.getErrorMessage());
                }

                ReconciliationResponse response = result.getData();

                record.setTotalInvoices(response.getTotalInvoices());
                record.setMatchedCount(response.getMatchedCount());
                record.setMismatchCount(response.getMismatchCount());
                record.setItcAtRisk(response.getItcAtRisk());
                record.setStatus(ReconciliationRecord.Status.DONE);
                record.setCompletedAt(LocalDateTime.now());

                log.info("Reconciliation completed for user: {}, matched: {}, mismatches: {}, ITC at risk: ₹{}",
                        user.getEmail(), response.getMatchedCount(),
                        response.getMismatchCount(), response.getItcAtRisk());

            } catch (Exception e) {
                log.error("Reconciliation failed for user: {}", user.getEmail(), e);
                record.setStatus(ReconciliationRecord.Status.FAILED);
                record.setCompletedAt(LocalDateTime.now());
            }

            return record;
        };
    }

    /**
     * Writer that saves all reconciliation records
     */
    @Bean
    public ItemWriter<ReconciliationRecord> reconciliationWriter() {
        return records -> {
            log.info("Saving {} reconciliation records", records.size());
            reconciliationRepository.saveAll(records);
        };
    }
}