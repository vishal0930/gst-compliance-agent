package com.gstcompliance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceBriefResponse {
    private String userId;
    private String period;
    private String brief;
    private BigDecimal totalSales;
    private BigDecimal totalGst;
    private BigDecimal totalItc;
    private BigDecimal taxLiability;
    private BigDecimal itcAtRisk;
    private List<ActionItemDTO> actionItems;
    private boolean isComplete;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItemDTO {
        private String title;
        private String description;
        private String priority;
        private boolean isCompleted;
    }
}