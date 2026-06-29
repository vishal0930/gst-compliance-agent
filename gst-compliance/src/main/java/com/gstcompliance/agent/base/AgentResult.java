package com.gstcompliance.agent.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private String correlationId;
    private Long processingTimeMs;
    private Double confidenceScore;

    public static <T> AgentResult<T> success(T data, String correlationId, Long processingTimeMs) {
        AgentResult<T> result = new AgentResult<>();
        result.setSuccess(true);
        result.setData(data);
        result.setCorrelationId(correlationId);
        result.setProcessingTimeMs(processingTimeMs);
        result.setConfidenceScore(1.0);
        return result;
    }

    public static <T> AgentResult<T> failure(String errorMessage, String correlationId) {
        AgentResult<T> result = new AgentResult<>();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setCorrelationId(correlationId);
        result.setConfidenceScore(0.0);
        return result;
    }
}