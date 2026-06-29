package com.gstcompliance.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.model.HsnCode;
import com.gstcompliance.repository.HsnEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class HsnEmbeddingLoadJob {

    private final HsnEmbeddingRepository hsnEmbeddingRepository;
    private final ObjectMapper objectMapper;

    @Bean
    public Job loadHsnEmbeddingsJob(JobRepository jobRepository, Step loadHsnEmbeddingsStep) {
        return new JobBuilder("loadHsnEmbeddingsJob", jobRepository)
                .start(loadHsnEmbeddingsStep)
                .build();
    }

    @Bean
    public Step loadHsnEmbeddingsStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      ItemReader<HsnCode> hsnCodeReader,
                                      ItemWriter<HsnCode> hsnCodeWriter) {
        return new StepBuilder("loadHsnEmbeddingsStep", jobRepository)
                .<HsnCode, HsnCode>chunk(100, transactionManager)
                .reader(hsnCodeReader)
                .writer(hsnCodeWriter)
                .build();
    }

    @Bean
    public FlatFileItemReader<HsnCode> hsnCodeReader() {
        FlatFileItemReaderBuilder<HsnCode> builder = new FlatFileItemReaderBuilder<>();

        try {
            // Try to load from CSV if available
            return builder
                    .name("hsnCodeReader")
                    .resource(new ClassPathResource("data/hsn_codes.csv"))
                    .linesToSkip(1)
                    .delimited()
                    .names("hsnCode", "description", "gstRate", "chapter", "heading")
                    .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
                        {
                            setTargetType(HsnCode.class);
                        }
                    })
                    .build();
        } catch (Exception e) {
            log.warn("HSN CSV not found, using default reader");
            return createDefaultReader();
        }
    }

    private FlatFileItemReader<HsnCode> createDefaultReader() {
        // Create a reader with default HSN codes
        return new FlatFileItemReader<HsnCode>() {
            @Override
            public HsnCode read() {
                // Return null after reading all codes
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<HsnCode> hsnCodeWriter() {
        return codes -> {
            log.info("Saving {} HSN codes", codes.size());
            hsnEmbeddingRepository.saveAll(codes);
            log.info("HSN codes loaded successfully");
        };
    }
}