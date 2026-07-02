package com.gstcompliance.agent;

import com.gstcompliance.agent.base.BaseAgent;
import com.gstcompliance.dto.response.ComplianceBriefResponse;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.model.Invoice;
import com.gstcompliance.pipeline.PipelineState;
import com.gstcompliance.service.InvoiceService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ReturnDrafterAgent extends BaseAgent<PipelineState, ComplianceBriefResponse> {

    private final ChatLanguageModel llmModel;
    private final InvoiceService invoiceService;

    private static final int MAX_ACTION_ITEMS = 5;
    private static final BigDecimal CRITICAL_ITC_THRESHOLD = new BigDecimal("50000");

    public ReturnDrafterAgent(
            @Qualifier("invoiceParserModel") ChatLanguageModel llmModel,
            @Lazy InvoiceService invoiceService)  {
        super("ReturnDrafter");
        this.llmModel = llmModel;
        this.invoiceService = invoiceService;
    }

    @Override
    protected ComplianceBriefResponse process(PipelineState state) throws Exception {
        log.info("Drafting return for user: {}, period: {}", state.getUserId(), state.getPeriod());

        ReconciliationData reconciliationData = extractReconciliationData(state);
        FinancialSummary financials = calculateFinancials(state, reconciliationData);
        String brief = generateComplianceBrief(state, reconciliationData, financials);
        List<String> actionStrings = generateActionItems(reconciliationData);

        // Convert the generated action strings into ActionItemDTO objects
        List<ComplianceBriefResponse.ActionItemDTO> actionItems = actionStrings.stream()
                .map(text -> ComplianceBriefResponse.ActionItemDTO.builder()
                        .title(text)
                        .description(text)
                        .priority(text.startsWith("🚨") ? "HIGH" :
                                  text.startsWith("⚠️") ? "MEDIUM" : "INFO")
                        .isCompleted(false)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        if (actionItems.isEmpty()) {
            actionItems = List.of(ComplianceBriefResponse.ActionItemDTO.builder()
                    .title("✅ All invoices matched")
                    .description("No action required for this period.")
                    .priority("INFO")
                    .isCompleted(false)
                    .build());
        }

        return ComplianceBriefResponse.builder()
                .userId(state.getUserId().toString())
                .period(state.getPeriod())
                .brief(brief)
                .totalSales(financials.getTotalSales())
                .totalGst(financials.getTotalGst())
                .totalItc(financials.getTotalItc())
                .taxLiability(financials.getTaxLiability())
                .itcAtRisk(reconciliationData.getItcAtRisk())
                .actionItems(actionItems)
                .isComplete(state.isAllAgentsSuccessful())
                .isApproved(false)
                .build();
    }

    private ReconciliationData extractReconciliationData(PipelineState state) {
        ReconciliationData data = new ReconciliationData();
        ReconciliationResponse reconResult = state.getReconciliationResult();

        if (reconResult == null) {
            log.warn("No reconciliation result found in state");
            return data;
        }

        data.setTotalInvoices(reconResult.getTotalInvoices());
        data.setMatchedCount(reconResult.getMatchedCount());
        data.setMismatchCount(reconResult.getMismatchCount());
        data.setItcAtRisk(reconResult.getItcAtRisk() != null ? reconResult.getItcAtRisk() : BigDecimal.ZERO);
        data.setMismatches(reconResult.getMismatches() != null ? reconResult.getMismatches() : new ArrayList<>());
        data.setSummary(reconResult.getSummary());
        return data;
    }

    private FinancialSummary calculateFinancials(
            PipelineState state,
            ReconciliationData data) {

        FinancialSummary summary = new FinancialSummary();

        try {

            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal totalGst = BigDecimal.ZERO;

            if (state.getInvoiceId() != null) {

                Invoice invoice = invoiceService.getInvoice(state.getInvoiceId());

                totalSales = invoice.getTotalAmount() != null
                        ? invoice.getTotalAmount()
                        : BigDecimal.ZERO;

                totalGst = invoice.getTotalGst() != null
                        ? invoice.getTotalGst()
                        : BigDecimal.ZERO;
            }

            summary.setTotalSales(totalSales);
            summary.setTotalGst(totalGst);
            summary.setTotalItc(data.getItcAtRisk());
            summary.setTaxLiability(totalGst.subtract(data.getItcAtRisk()));

            log.info(
                    "Calculated financials: Sales={}, GST={}, ITC={}, Liability={}",
                    totalSales,
                    totalGst,
                    data.getItcAtRisk(),
                    summary.getTaxLiability()
            );

            return summary;

        } catch (Exception e) {

            log.error("Failed to calculate financials", e);

            summary.setTotalSales(BigDecimal.ZERO);
            summary.setTotalGst(BigDecimal.ZERO);
            summary.setTotalItc(data.getItcAtRisk());
            summary.setTaxLiability(BigDecimal.ZERO);

            return summary;

        }
    }

    private String generateComplianceBrief(PipelineState state, ReconciliationData data, FinancialSummary financials) {
        String prompt = buildDetailedPrompt(state, data, financials);
        try {
            String brief = llmModel.generate(prompt);
            log.info("Generated compliance brief for user: {}, length: {}", state.getUserId(), brief.length());
            return brief;
        } catch (Exception e) {
            log.error("Failed to generate LLM brief", e);
            return buildFallbackBrief(state.getPeriod(), data);
        }
    }

    private String buildDetailedPrompt(PipelineState state, ReconciliationData data, FinancialSummary financials) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a GST compliance advisor for Indian businesses. ")
                .append("Create a clear, professional compliance brief for the business owner.\n\n");

        prompt.append("COMPANY & PERIOD:\n");
        prompt.append("- User: ").append(state.getUserId()).append("\n");
        prompt.append("- Period: ").append(state.getPeriod()).append("\n\n");

        // Include reconciliation summary if available
        if (data.getSummary() != null && !data.getSummary().isEmpty()) {
            prompt.append("RECONCILIATION ENGINE SUMMARY:\n");
            prompt.append(data.getSummary()).append("\n\n");
        }

        prompt.append("RECONCILIATION DETAILS:\n");
        prompt.append("- Total Invoices: ").append(data.getTotalInvoices()).append("\n");
        prompt.append("- Matched Invoices: ").append(data.getMatchedCount()).append("\n");
        prompt.append("- Mismatches Found: ").append(data.getMismatchCount()).append("\n");
        prompt.append("- ITC at Risk: ₹").append(formatCurrency(data.getItcAtRisk())).append("\n\n");

        if (data.getMismatches() != null && !data.getMismatches().isEmpty()) {
            Map<String, Long> mismatchTypes = data.getMismatches().stream()
                    .collect(Collectors.groupingBy(ReconciliationResponse.MismatchDTO::getStatus, Collectors.counting()));

            prompt.append("MISMATCH BREAKDOWN:\n");
            mismatchTypes.forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }

        prompt.append("FINANCIAL SUMMARY:\n");
        prompt.append("- Total Sales: ₹").append(formatCurrency(financials.getTotalSales())).append("\n");
        prompt.append("- Total GST: ₹").append(formatCurrency(financials.getTotalGst())).append("\n");
        prompt.append("- ITC Claimed: ₹").append(formatCurrency(financials.getTotalItc())).append("\n");
        prompt.append("- Tax Liability: ₹").append(formatCurrency(financials.getTaxLiability())).append("\n\n");

        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. If there are NO mismatches: Congratulate the user and confirm no immediate action is required\n");
        prompt.append("2. If there ARE mismatches: Prioritize them by business impact\n");
        prompt.append("3. Explain compliance status in simple, clear terms\n");
        prompt.append("4. Mention the most significant risks (if any)\n");
        prompt.append("5. Avoid technical jargon - write for a business owner\n");
        prompt.append("6. Keep response concise (150-250 words)\n");
        prompt.append("7. Provide clear, actionable recommendations\n\n");

        prompt.append("RESPONSE:\n");

        return prompt.toString();
    }

    private List<String> generateActionItems(ReconciliationData data) {
        List<String> actions = new ArrayList<>();

        if (data.getMismatches() == null || data.getMismatches().isEmpty()) {
            if (data.getItcAtRisk().compareTo(BigDecimal.ZERO) == 0) {
                actions.add("✅ All invoices matched. No action required.");
            }
            return actions;
        }

        Map<String, List<ReconciliationResponse.MismatchDTO>> grouped = data.getMismatches().stream()
                .collect(Collectors.groupingBy(ReconciliationResponse.MismatchDTO::getStatus));

        // Priority 1: Missing invoices - most critical
        List<ReconciliationResponse.MismatchDTO> missingInErp = grouped.getOrDefault("MISSING_IN_ERP", Collections.emptyList());
        List<ReconciliationResponse.MismatchDTO> missingInGstr = grouped.getOrDefault("MISSING_IN_GSTR2B", Collections.emptyList());

        if (!missingInErp.isEmpty()) {
            actions.add("🚨 " + missingInErp.size() + " invoice(s) missing in books. Add immediately to claim ITC.");
        }
        if (!missingInGstr.isEmpty()) {
            actions.add("⚠️ " + missingInGstr.size() + " invoice(s) not in GSTR-2B. Contact suppliers.");
        }

        // Priority 2: Amount mismatches
        List<ReconciliationResponse.MismatchDTO> amountMismatches = grouped.getOrDefault("AMOUNT_MISMATCH", Collections.emptyList());
        if (!amountMismatches.isEmpty()) {
            String sample = amountMismatches.stream().limit(3)
                    .map(ReconciliationResponse.MismatchDTO::getInvoiceNumber)
                    .collect(Collectors.joining(", "));
            actions.add("📊 " + amountMismatches.size() + " amount mismatch(es). Verify: " + sample);
        }

        // Priority 3: GST mismatches
        List<ReconciliationResponse.MismatchDTO> gstMismatches = grouped.getOrDefault("GST_MISMATCH", Collections.emptyList());
        if (!gstMismatches.isEmpty()) {
            actions.add("🧾 " + gstMismatches.size() + " GST mismatch(es). Review rates and calculations.");
        }

        // Priority 4: HSN mismatches
        List<ReconciliationResponse.MismatchDTO> hsnMismatches = grouped.getOrDefault("HSN_MISMATCH", Collections.emptyList());
        if (!hsnMismatches.isEmpty()) {
            actions.add("📋 " + hsnMismatches.size() + " HSN mismatch(es). Correct HSN codes.");
        }

        // Priority 5: Date mismatches
        List<ReconciliationResponse.MismatchDTO> dateMismatches = grouped.getOrDefault("DATE_MISMATCH", Collections.emptyList());
        if (!dateMismatches.isEmpty()) {
            actions.add("📅 " + dateMismatches.size() + " date mismatch(es). Verify invoice dates.");
        }

        // Priority 6: Line item mismatches
        List<ReconciliationResponse.MismatchDTO> lineItemMismatches = grouped.getOrDefault("LINE_ITEM_MISMATCH", Collections.emptyList());
        if (!lineItemMismatches.isEmpty()) {
            actions.add("📦 " + lineItemMismatches.size() + " line item mismatch(es). Review details.");
        }

        // ITC risk
        if (data.getItcAtRisk().compareTo(CRITICAL_ITC_THRESHOLD) > 0) {
            actions.add("💰 High ITC at risk (₹" + formatCurrency(data.getItcAtRisk()) + "). Resolve before GSTR-3B filing.");
        }

        return actions.size() > MAX_ACTION_ITEMS ? actions.subList(0, MAX_ACTION_ITEMS) : actions;
    }

    private String buildFallbackBrief(String period, ReconciliationData data) {
        StringBuilder brief = new StringBuilder();
        brief.append("GST Compliance Summary\n");
        brief.append("- Period: ").append(period).append("\n");

        if (data.getMismatchCount() == 0) {
            brief.append("- Status: ✅ All invoices matched. No action required.\n");
        } else {
            brief.append("- Status: ⚠️ ").append(data.getMismatchCount()).append(" mismatches found\n");
            brief.append("- ITC at Risk: ₹").append(formatCurrency(data.getItcAtRisk())).append("\n");
            brief.append("\nActions Required:\n");
            brief.append("- Review ").append(data.getMismatchCount()).append(" mismatched invoices\n");
            brief.append("- Verify with suppliers\n");
            brief.append("- Resolve before GSTR-3B filing\n");
        }
        return brief.toString();
    }

    // Helper methods
    private String formatCurrency(BigDecimal amount) {
        return amount != null ? String.format("%.2f", amount) : "0.00";
    }

    @lombok.Data
    private static class ReconciliationData {
        private int totalInvoices;
        private int matchedCount;
        private int mismatchCount;
        private BigDecimal itcAtRisk = BigDecimal.ZERO;
        private List<ReconciliationResponse.MismatchDTO> mismatches = new ArrayList<>();
        private String summary;
    }

    @lombok.Data
    private static class FinancialSummary {
        private BigDecimal totalSales = BigDecimal.ZERO;
        private BigDecimal totalGst = BigDecimal.ZERO;
        private BigDecimal totalItc = BigDecimal.ZERO;
        private BigDecimal taxLiability = BigDecimal.ZERO;
    }
}