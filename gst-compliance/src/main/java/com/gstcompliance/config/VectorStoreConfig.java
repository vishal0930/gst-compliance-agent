package com.gstcompliance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class VectorStoreConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // pgvector configuration
    @Bean
    public boolean initializeVectorExtension(DataSource dataSource) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector extension initialized successfully");
            return true;
        } catch (Exception e) {
            log.warn("pgvector extension may already exist: {}", e.getMessage());
            return false;
        }
    }

    // Embedding dimension
    public static final int EMBEDDING_DIMENSION = 768;
}