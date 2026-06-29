package com.gstcompliance.integration;

import com.gstcompliance.GstComplianceApplication;
import com.gstcompliance.pipeline.AgentPipelineService;
import com.gstcompliance.pipeline.PipelineState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GstComplianceApplication.class)
@Testcontainers
public class PipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private AgentPipelineService pipelineService;

    @Test
    void testFullPipelineExecution() throws Exception {
        // Given
        String userId = "test@example.com";
        String period = "01-2026";
        List<String> fileKeys = List.of("invoice1.pdf", "invoice2.pdf");

        // When
        CompletableFuture<PipelineState> future = pipelineService.runPipeline(userId, period, fileKeys);
        PipelineState state = future.get();

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getStatus()).isEqualTo(PipelineState.PipelineStatus.COMPLETED);
        assertThat(state.isAllAgentsSuccessful()).isTrue();
        assertThat(state.getParsedInvoices()).isNotEmpty();
    }
}