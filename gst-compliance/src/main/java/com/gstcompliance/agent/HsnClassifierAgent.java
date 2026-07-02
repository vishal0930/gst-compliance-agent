package com.gstcompliance.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.agent.base.BaseAgent;
import com.gstcompliance.cache.HsnCacheService;
import com.gstcompliance.dto.response.InvoiceParseResponse;
import com.gstcompliance.model.HsnCode;
import com.gstcompliance.service.HsnLookupService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gstcompliance.util.ConversionUtils.toBigDecimal;

@Component
@Slf4j
public class HsnClassifierAgent extends BaseAgent<List<InvoiceParseResponse.LineItemDTO>, List<Map<String, Object>>> {

    private final HsnLookupService hsnLookupService;
    private final ChatLanguageModel llmModel;
    private final ObjectMapper objectMapper;
    private final HsnCacheService hsnCacheService;
    private final HsnClassifierAgent self;

    public HsnClassifierAgent(HsnLookupService hsnLookupService,
                              @Qualifier("invoiceParserModel") ChatLanguageModel llmModel,
                              ObjectMapper objectMapper,
                              HsnCacheService hsnCacheService,
                              @org.springframework.context.annotation.Lazy HsnClassifierAgent self) {
        super("HsnClassifier");
        this.hsnLookupService = hsnLookupService;
        this.llmModel = llmModel;
        this.objectMapper = objectMapper;
        this.hsnCacheService = hsnCacheService;
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

    public Map<String, Object> classifyItem(InvoiceParseResponse.LineItemDTO item) {
        String description = item.getDescription();
        log.debug("Classifying: {}", description);

        // Check Redis cache first - disabled to avoid type deserialization issues returning null/Double rates
        /*
        Map<String, Object> cachedResult = hsnCacheService.getCachedClassification(description);

        if (cachedResult != null) {

            BigDecimal confidence = toBigDecimal(cachedResult.get("confidence"));

            if (confidence != null &&
                    confidence.compareTo(BigDecimal.valueOf(0.7)) >= 0) {

                log.info("Cache hit for '{}' with confidence {}", description, confidence);
                return cachedResult;
            }
        }
        */

        try {
            List<HsnCode> candidates = hsnLookupService.findTopCandidates(description, 5);
            log.info("================ HSN CANDIDATES =================");
            log.info("Top HSN candidates:");
            for (HsnCode c : candidates) {
                log.info("HSN={} GST={} DESC={}",
                        c.getHsnCode(),
                        c.getGstRate(),
                        c.getDescription());
            }
            log.info("================================================");
            log.info("Found {} candidates for '{}'", candidates.size(), description);

            if (candidates.isEmpty()) {
                log.warn("No candidates found for '{}', using fallback", description);
                Map<String, Object> fallback = createFallbackResult(description);
                // hsnCacheService.cacheClassification(description, fallback);
                return fallback;
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
                    .orElseGet(() -> {

                        log.warn(
                                "LLM selected invalid HSN. Falling back to {}",
                                candidates.get(0).getHsnCode()
                        );

                        return candidates.get(0);
                    });
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

            // Cache the result if confidence is >= 0.7
            BigDecimal confidence = toBigDecimal(result.get("confidence"));

            if (confidence != null &&
                    confidence.compareTo(BigDecimal.valueOf(0.7)) >= 0) {

                result.put("confidence", confidence);

                // hsnCacheService.cacheClassification(description, result);
            }

            return result;

        } catch (Exception e) {
            log.error("Classification failed for '{}': {}", description, e.getMessage());
            Map<String, Object> fallback = createFallbackResult(description);
            // hsnCacheService.cacheClassification(description, fallback);
            return fallback;
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
    private BigDecimal toBigDecimal(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        return new BigDecimal(value.toString());
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
        sb.append("""
You are an expert Indian GST HSN Classification Assistant.

Your task is to classify the product into ONE HSN code from the provided candidates.

IMPORTANT RULES:

1. You MUST choose ONLY one HSN from the candidate list.
2. NEVER invent or generate a new HSN code.
3. Prefer the candidate whose description best matches the product.
4. Prefer specific product classifications over generic categories.
5. Consider the complete product description, including:
   - Product type
   - Material
   - Industry
   - Intended use
   - Packaging (if relevant)
6. Do NOT select a generic petroleum heading if a more specific lubricating preparation or industrial product exists.
7. If none of the candidates clearly match, choose the closest candidate and set confidence below 0.80.
8. Confidence must be between 0.0 and 1.0.
9. Keep the reasoning concise (maximum 20 words).

Return ONLY valid JSON.

{
  "hsnCode": "",
  "confidence": 0.0,
  "reasoning": ""
}

""");
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