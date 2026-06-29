package com.gstcompliance.exception;

public class AgentException extends RuntimeException {
    private final String agentName;
    private final String correlationId;

    public AgentException(String agentName, String message) {
        super(message);
        this.agentName = agentName;
        this.correlationId = null;
    }

    public AgentException(String agentName, String message, String correlationId) {
        super(message);
        this.agentName = agentName;
        this.correlationId = correlationId;
    }

    public AgentException(String agentName, String message, Throwable cause) {
        super(message, cause);
        this.agentName = agentName;
        this.correlationId = null;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}