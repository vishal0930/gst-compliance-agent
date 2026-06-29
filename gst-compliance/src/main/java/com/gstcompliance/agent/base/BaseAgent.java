package com.gstcompliance.agent.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.UUID;

@Slf4j
public abstract class BaseAgent<T, R> {

    private final String agentName;

    public BaseAgent(String agentName) {
        this.agentName = agentName;
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public AgentResult<R> execute(T input) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Agent [{}] starting with correlationId: {}", agentName, correlationId);

        long startTime = System.currentTimeMillis();

        try {
            R result = process(input);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Agent [{}] completed in {}ms", agentName, duration);
            return AgentResult.success(result, correlationId, duration);

        } catch (Exception e) {
            log.error("Agent [{}] failed: {}", agentName, e.getMessage(), e);
            return AgentResult.failure(e.getMessage(), correlationId);
        }
    }

    protected abstract R process(T input) throws Exception;
}