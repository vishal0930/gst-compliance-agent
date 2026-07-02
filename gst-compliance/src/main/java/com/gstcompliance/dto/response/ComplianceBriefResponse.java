package com.gstcompliance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceBriefResponse {

    private UUID id;
    private String userId;
    private String period;
    private String brief;

    private BigDecimal totalSales;
    private BigDecimal totalGst;
    private BigDecimal totalItc;
    private BigDecimal taxLiability;
    private BigDecimal itcAtRisk;

    private List<ActionItemDTO> actionItems;

    @JsonProperty("isComplete")
    private boolean isComplete;

    @JsonProperty("isApproved")
    private boolean isApproved;

    private LocalDateTime generatedAt;
    private LocalDateTime approvedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItemDTO {
        private String title;
        private String description;
        private String priority;
        @JsonProperty("isCompleted")
        private boolean isCompleted;
    }
}