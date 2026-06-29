package com.gstcompliance.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.agent.base.BaseAgent;
import com.gstcompliance.dto.response.InvoiceParseResponse;
import com.gstcompliance.model.HsnCode;
import com.gstcompliance.service.HsnLookupService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HsnClassifierAgent extends BaseAgent<List<InvoiceParseResponse.LineItemDTO>, List<Map<String, Object>>> {

    private final HsnLookupService hsnLookupService;
    private final ChatLanguageModel llmModel;
    private final ObjectMapper objectMapper;
    private final HsnClassifierAgent self;

    public HsnClassifierAgent(HsnLookupService hsnLookupService,
                              @Qualifier("invoiceParserModel") ChatLanguageModel llmModel,
                              ObjectMapper objectMapper,
                              @org.springframework.context.annotation.Lazy HsnClassifierAgent self) {
        super("HsnClassifier");
        this.hsnLookupService = hsnLookupService;
        this.llmModel = llmModel;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Override
    protected List<Map<String, Object>> process(List<InvoiceParseResponse.LineItemDTO> items) throws Exception {
        log.info("Classifying {} items", items.size());

        List<Map<String, Object>> results = new ArrayList<>();
        for (InvoiceParseResponse.LineItemDTO item : items) {
            Map<String, Object> result = self.classifyItem(item);
            results.add(result);
        }

        return results;
    }

    @Cacheable(value = "hsnClassification", key = "#item.description.trim().toLowerCase()", unless = "#result.confidence < 0.7")
    public Map<String, Object> classifyItem(InvoiceParseResponse.LineItemDTO item) {
        String description = item.getDescription();
        log.debug("Classifying: {}", description);

        try {
            List<HsnCode> candidates = hsnLookupService.findTopCandidates(description, 5);
            log.info("Found {} candidates for '{}'", candidates.size(), description);

            if (candidates.isEmpty()) {
                log.warn("No candidates found for '{}', using fallback", description);
                return createFallbackResult(description);
            }

            String prompt = buildClassificationPrompt(description, candidates);
            String llmResponse = llmModel.generate(prompt);
            log.debug("LLM Response: {}", llmResponse);

            Map<String, Object> parsedResponse = parseLLMResponse(llmResponse);

            // Safely extract HSN code, handling both string and number types
            Object hsnObject = parsedResponse.getOrDefault("hsnCode", candidates.get(0).getHsnCode());
            String selectedHsn = String.valueOf(hsnObject);

            // Log if LLM selected an HSN not in candidates
            if (candidates.stream().noneMatch(c -> c.getHsnCode().equals(selectedHsn))) {
                log.warn(
                        "LLM selected HSN {} which is not in candidate list. Falling back to {}",
                        selectedHsn,
                        candidates.get(0).getHsnCode()
                );
            }

            HsnCode selectedCode = candidates.stream()
                    .filter(c -> c.getHsnCode().equals(selectedHsn))
                    .findFirst()
                    .orElse(candidates.get(0));
            log.info(
                    "HSN Entity -> HSN={}, GST={}, CGST={}, SGST={}, IGST={}",
                    selectedCode.getHsnCode(),
                    selectedCode.getGstRate(),
                    selectedCode.getCgstRate(),
                    selectedCode.getSgstRate(),
                    selectedCode.getIgstRate()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("description", description);
            result.put("hsnCode", selectedCode.getHsnCode());
            result.put("gstRate", selectedCode.getGstRate());
            result.put("cgstRate", selectedCode.getCgstRate());
            result.put("sgstRate", selectedCode.getSgstRate());
            result.put("igstRate", selectedCode.getIgstRate());
            result.put("confidence", parsedResponse.getOrDefault("confidence", BigDecimal.valueOf(0.90)));
            result.put("needsReview", false);
            result.put("reasoning", parsedResponse.getOrDefault("reasoning", "Matched from HSN master"));

            return result;

        } catch (Exception e) {
            log.error("Classification failed for '{}': {}", description, e.getMessage());
            return createFallbackResult(description);
        }
    }

    private Map<String, Object> createFallbackResult(String description) {

        Map<String, Object> result = new HashMap<>();

        result.put("description", description);
        result.put("hsnCode", null);
        result.put("gstRate", null);
        result.put("cgstRate", null);
        result.put("sgstRate", null);
        result.put("igstRate", null);

        result.put("confidence", BigDecimal.ZERO);
        result.put("needsReview", true);

        result.put(
                "reasoning",
                "Automatic HSN classification failed. Manual review required."
        );

        return result;
    }

    private Map<String, Object> parseLLMResponse(String response) {
        try {
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            int openBraces = 0, closeBraces = 0;
            for (char c : cleaned.toCharArray()) {
                if (c == '{') openBraces++;
                else if (c == '}') closeBraces++;
            }
            if (openBraces > closeBraces) {
                cleaned += "}".repeat(openBraces - closeBraces);
            }

            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String buildClassificationPrompt(String description, List<HsnCode> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Given the item description and HSN code candidates, select the most appropriate HSN code.\n\n");
        sb.append("ITEM: ").append(description).append("\n\n");
        sb.append("CANDIDATES:\n");

        for (HsnCode code : candidates) {
            sb.append(code.getHsnCode()).append("\n");
            sb.append("  Description: ").append(code.getDescription()).append("\n");
            sb.append("  GST: ").append(code.getGstRate()).append("%\n");
            if (code.getCgstRate() != null) {
                sb.append("  CGST: ").append(code.getCgstRate()).append("%\n");
            }
            if (code.getSgstRate() != null) {
                sb.append("  SGST: ").append(code.getSgstRate()).append("%\n");
            }
            if (code.getIgstRate() != null) {
                sb.append("  IGST: ").append(code.getIgstRate()).append("%\n");
            }
            sb.append("\n");
        }

        sb.append("Respond with valid JSON ONLY:\n");
        sb.append("{\"hsnCode\": \"\", \"confidence\": 0.0, \"reasoning\": \"\"}");
        return sb.toString();
    }
}