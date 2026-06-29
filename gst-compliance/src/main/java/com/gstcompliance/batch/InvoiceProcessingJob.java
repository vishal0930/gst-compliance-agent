package com.gstcompliance.batch;

import com.gstcompliance.model.Invoice;
import com.gstcompliance.repository.InvoiceRepository;
import com.gstcompliance.agent.InvoiceParserAgent;
import com.gstcompliance.dto.response.InvoiceParseResponse;
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
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class InvoiceProcessingJob {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceParserAgent invoiceParserAgent;

    @Bean
    public Job processPendingInvoicesJob(JobRepository jobRepository, Step processPendingInvoicesStep) {
        return new JobBuilder("processPendingInvoicesJob", jobRepository)
                .start(processPendingInvoicesStep)
                .build();
    }

    @Bean
    public Step processPendingInvoicesStep(JobRepository jobRepository,
                                           PlatformTransactionManager transactionManager,
                                           ItemReader<Invoice> invoiceReader,
                                           ItemProcessor<Invoice, Invoice> invoiceProcessor,
                                           ItemWriter<Invoice> invoiceWriter) {
        return new StepBuilder("processPendingInvoicesStep", jobRepository)
                .<Invoice, Invoice>chunk(10, transactionManager)
                .reader(invoiceReader)
                .processor(invoiceProcessor)
                .writer(invoiceWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Invoice> invoiceReader() {
        RepositoryItemReader<Invoice> reader = new RepositoryItemReader<>();
        reader.setRepository(invoiceRepository);
        reader.setMethodName("findAll");
        reader.setPageSize(10);
        reader.setSort(Map.of("createdAt", Sort.Direction.ASC));
        return reader;
    }

    @Bean
    public ItemProcessor<Invoice, Invoice> invoiceProcessor() {
        return invoice -> {
            log.info("Processing invoice: {}", invoice.getId());

            try {
                // Process invoice through agent
                var result = invoiceParserAgent.execute(invoice.getFileKey());

                if (result.isSuccess()) {
                    // ✅ FIX: parsedData is InvoiceParseResponse, not Map
                    InvoiceParseResponse parsedData = (InvoiceParseResponse) result.getData();

                    // Use getter methods instead of getOrDefault
                    invoice.setVendorName(parsedData.getVendorName() != null ?
                            parsedData.getVendorName() : "UNKNOWN");
                    invoice.setVendorGstin(parsedData.getVendorGstin() != null ?
                            parsedData.getVendorGstin() : "UNKNOWN");
                    invoice.setInvoiceNumber(parsedData.getInvoiceNumber() != null ?
                            parsedData.getInvoiceNumber() : "UNKNOWN");
                    invoice.setTotalAmount(parsedData.getTotalAmount() != null ?
                            parsedData.getTotalAmount() : BigDecimal.ZERO);
                    invoice.setTotalGst(parsedData.getTotalGst() != null ?
                            parsedData.getTotalGst() : BigDecimal.ZERO);
                    invoice.setConfidenceScore(parsedData.getConfidenceScore() != null ?
                            parsedData.getConfidenceScore() : BigDecimal.ZERO);

                    invoice.setParseStatus(Invoice.ParseStatus.DONE.name());
                } else {
                    invoice.setParseStatus(Invoice.ParseStatus.FAILED.name());
                }

                return invoice;

            } catch (Exception e) {
                log.error("Failed to process invoice: {}", e.getMessage());
                invoice.setParseStatus(Invoice.ParseStatus.FAILED.name());
                return invoice;
            }
        };
    }

    @Bean
    public ItemWriter<Invoice> invoiceWriter() {
        return invoices -> {
            log.info("Saving {} processed invoices", invoices.size());
            invoiceRepository.saveAll(invoices);
        };
    }
}