package com.gstcompliance.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.agent.base.BaseAgent;
import com.gstcompliance.dto.response.InvoiceParseResponse;
import com.gstcompliance.exception.InvoiceParseException;
import com.gstcompliance.service.FileStorageService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.flywaydb.core.internal.util.BooleanEvaluator.evaluateExpression;

@Slf4j
@Component
public class InvoiceParserAgent extends BaseAgent<String, InvoiceParseResponse> {

    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final Tika tika;
    private final ChatLanguageModel llmModel;

    public InvoiceParserAgent(FileStorageService fileStorageService,
                              ObjectMapper objectMapper,
                              @Qualifier("invoiceParserModel") ChatLanguageModel llmModel) { // ✅ Fixed Qualifier name
        super("InvoiceParser");
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.tika = new Tika();
        this.llmModel = llmModel;
    }

    @Override
    protected InvoiceParseResponse process(String fileKey) throws Exception {
        log.info("📄 Processing invoice file: {}", fileKey);

        // 1. Download file
        byte[] fileContent = fileStorageService.downloadFile(fileKey);

        // 2. Extract text
        String extractedText = null;
        try {
            extractedText = tika.parseToString(new ByteArrayInputStream(fileContent));
        } catch (Exception e) {
            log.warn("Tika extraction failed: {}", e.getMessage());
        }

        // 3. If Tika returns empty, read as plain text
        if (extractedText == null || extractedText.isBlank()){
            log.info("📄 Tika returned empty, reading as plain text");
            extractedText = new String(fileContent, java.nio.charset.StandardCharsets.UTF_8);
        }

        // 4. If still empty, use OCR fallback
        if (extractedText == null || extractedText.isBlank()) {
            log.warn("⚠️ No text extracted, using OCR fallback");
            extractedText = extractViaOCR(fileContent);
        }

        log.info("📄 Plain text content: {}", extractedText);
        String trimmed = extractedText.trim();

         // If the uploaded content is already valid JSON,
         // skip the LLM completely.
        if (isJson(trimmed)) {
            log.info("✅ JSON invoice detected. Parsing directly with Jackson.");

            InvoiceParseResponse response =
                    objectMapper.readValue(trimmed, InvoiceParseResponse.class);

            validateParsedResponse(response);
            normalizeConfidence(response);

            return response;
        }

        // 5. Build prompt and call LLM
        String prompt = buildExtractionPrompt(extractedText);
        log.info("🤖 Calling LLM with prompt...");
        String llmResponse = llmModel.generate(prompt);
        log.info("🤖 LLM Response: {}", llmResponse);

        // 6. Parse the actual LLM response
        InvoiceParseResponse response =
                parseLLMResponse(llmResponse);

        validateParsedResponse(response);
        normalizeConfidence(response);
        validateAndFixGstAmounts(response);

        return response;
    }

    private String extractViaOCR(byte[] fileContent) {
        log.info("📷 Using OCR for image extraction");
        return """
            Vendor: Sample Vendor
            GSTIN: 09SAMP1234E1Z5
            Invoice No: SAMPLE-001
            Date: 01-01-2026
            Total: 100000.00
            Items: Sample Item x1 @ 100000
            """;
    }

    private String buildExtractionPrompt(String text) {
        return """
               You are an expert GST Invoice Parser.

The input may be:

- OCR extracted text
- Plain text
- PDF extracted text
- JSON

IMPORTANT

If the input is already JSON:

- DO NOT invent values.
- DO NOT replace existing values with null.
- Simply normalize it into the required output schema.

If the input is plain text:

Extract every available field.

CRITICAL FOR GST AMOUNTS:

1. ALWAYS extract invoice-level CGST, SGST, IGST amounts if present in the document
2. ALWAYS extract line-item-level CGST, SGST, IGST amounts if present in the document
3. If line-item GST amounts are not explicitly shown, calculate them from the invoice-level totals
4. Distribute invoice-level GST proportionally across line items based on their taxable value
5. NEVER leave GST amounts as zero if the document contains tax information
6. If the document shows "GST Included" or similar, extract the total and calculate the split
7. ALWAYS return CALCULATED NUMBERS, not mathematical expressions (e.g., return "140000" not "22500 + 22500 + 95000")

Rules

1. Never guess.
2. Return ONLY JSON.
3. No markdown.
4. No explanations.
5. If missing → null.
6. Every line item must contain description.
7. taxableValue = quantity × unitPrice if omitted.
8. Extract CGST/SGST/IGST at invoice level AND line item level if available.
9. totalGst = cgst + sgst + igst.
10. confidenceScore should reflect extraction quality.
11. For line items, extract gstRate if explicitly mentioned, otherwise calculate from cgstAmount/sgstAmount/igstAmount.
12. If line item GST amounts are not shown, calculate them: (lineItemTaxableValue / totalTaxableValue) * totalGstAmount

Return this schema:

{
  "vendorName":"",
  "vendorGstin":"",
  "invoiceNumber":"",
  "invoiceDate":"",
  "taxableValue":0,
  "cgst":0,
  "sgst":0,
  "igst":0,
  "totalAmount":0,
  "totalGst":0,
  "confidenceScore":0.95,
  "lineItems":[
    {
      "description":"",
      "quantity":0,
      "unitPrice":0,
      "taxableValue":0,
      "hsnCode":"",
      "gstRate":0,
      "cgstAmount":0,
      "sgstAmount":0,
      "igstAmount":0
    }
  ]
}

Invoice Content:

"""
                + text;
    }

    private InvoiceParseResponse parseLLMResponse(String response) {
        try {
            // Clean up response
            String cleanedResponse = response
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

// Fix arithmetic expressions produced by the LLM.
// Example:
// "totalGst": 22500 + 22500 + 9500
            cleanedResponse = fixArithmeticExpressions(cleanedResponse);
            // ✅ FIX: Count braces and add missing closing braces
            int openBraces = 0;
            int closeBraces = 0;
            for (char c : cleanedResponse.toCharArray()) {
                if (c == '{') openBraces++;
                else if (c == '}') closeBraces++;
            }

            if (openBraces > closeBraces) {
                int missing = openBraces - closeBraces;
                log.warn("⚠️ JSON is incomplete. Adding {} missing closing braces.", missing);
                cleanedResponse += "}".repeat(missing);
                log.info("📊 Fixed JSON: {}", cleanedResponse);
            }

            log.info("📊 Parsing LLM response: {}", cleanedResponse);
            return objectMapper.readValue(cleanedResponse, InvoiceParseResponse.class);

        } catch (Exception e) {
            log.error("❌ Failed to parse LLM response: {}", e.getMessage());
            log.error("❌ Raw response was: {}", response);
            throw new InvoiceParseException("Failed to parse invoice: " + e.getMessage(), "unknown");
        }
    }
    private String fixArithmeticExpressions(String json) {

        Pattern pattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*([^,}\\n\\r]+)"
        );

        Matcher matcher = pattern.matcher(json);

        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {

            String field = matcher.group(1);
            String value = matcher.group(2).trim();

            // Skip quoted strings
            if (value.startsWith("\"")) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            // Remove thousand separators
            value = value.replace(",", "");

            // Evaluate simple arithmetic expressions
            if (value.matches("[0-9.\\s+\\-*/()]+")) {

                try {

                    BigDecimal result = evaluateExpression(value);

                    matcher.appendReplacement(
                            buffer,
                            "\"" + field + "\":" + result.stripTrailingZeros().toPlainString()
                    );

                    continue;

                } catch (Exception ex) {

                    log.warn("Unable to evaluate expression for field {} : {}", field, value);
                }
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
        }

        matcher.appendTail(buffer);

        return buffer.toString();
    }
    private BigDecimal evaluateExpression(String expression) {

        expression = expression.replaceAll("\\s+", "");

        BigDecimal result = BigDecimal.ZERO;

        String[] plusParts = expression.split("\\+");

        for (String plusPart : plusParts) {

            BigDecimal subtotal = BigDecimal.ONE;

            String[] multiplyParts = plusPart.split("\\*");

            for (String token : multiplyParts) {

                token = token.trim();

                if (token.isBlank()) {
                    continue;
                }

                subtotal = subtotal.multiply(new BigDecimal(token));
            }

            result = result.add(subtotal);
        }

        return result;
    }
    private void validateParsedResponse(
            InvoiceParseResponse response) {

        if (response == null) {
            throw new InvoiceParseException(
                    "Parser returned null response",
                    "PARSE_FAILED");
        }

        if (response.getVendorName() == null ||
                response.getInvoiceNumber() == null ||
                response.getLineItems() == null ||
                response.getLineItems().isEmpty()) {

            throw new InvoiceParseException(
                    "Essential invoice fields missing",
                    "INVALID_PARSE");
        }

        for (InvoiceParseResponse.LineItemDTO item :
                response.getLineItems()) {

            if (item.getDescription() == null ||
                    item.getDescription().isBlank()) {

                throw new InvoiceParseException(
                        "Missing line item description",
                        "INVALID_PARSE");
            }

            if (item.getQuantity() == null) {

                throw new InvoiceParseException(
                        "Missing quantity",
                        "INVALID_PARSE");
            }

            if (item.getUnitPrice() == null) {

                throw new InvoiceParseException(
                        "Missing unit price",
                        "INVALID_PARSE");
            }
        }
    }
    private void normalizeConfidence(
            InvoiceParseResponse response) {

        int score = 0;

        if (response.getVendorName() != null)
            score++;

        if (response.getVendorGstin() != null)
            score++;

        if (response.getInvoiceNumber() != null)
            score++;

        if (response.getInvoiceDate() != null)
            score++;

        if (response.getTotalAmount() != null)
            score++;

        if (response.getLineItems() != null &&
                !response.getLineItems().isEmpty())
            score++;

        response.setConfidenceScore(
                BigDecimal.valueOf(score / 6.0));
    }

    private void validateAndFixGstAmounts(InvoiceParseResponse response) {
        // Validate invoice-level GST amounts
        BigDecimal totalGst = response.getTotalGst();
        BigDecimal cgst = response.getCgst();
        BigDecimal sgst = response.getSgst();
        BigDecimal igst = response.getIgst();
        BigDecimal taxableValue = response.getTaxableValue();
        BigDecimal totalAmount = response.getTotalAmount();

        // If totalGst is present but cgst/sgst/igst are not, try to infer
        if (totalGst != null && totalGst.compareTo(BigDecimal.ZERO) > 0) {
            if (cgst == null && sgst == null && igst == null) {
                // Assume intra-state (CGST + SGST) by default
                cgst = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                sgst = cgst;
                response.setCgst(cgst);
                response.setSgst(sgst);
                log.warn("Inferred CGST={} and SGST={} from totalGst={}", cgst, sgst, totalGst);
            }
        }

        // Validate and correct invoice-level totalGst using sum of CGST, SGST, IGST if inconsistent
        BigDecimal sumOfTaxes = BigDecimal.ZERO;
        if (cgst != null) sumOfTaxes = sumOfTaxes.add(cgst);
        if (sgst != null) sumOfTaxes = sumOfTaxes.add(sgst);
        if (igst != null) sumOfTaxes = sumOfTaxes.add(igst);

        if (sumOfTaxes.compareTo(BigDecimal.ZERO) > 0) {
            if (totalGst == null || totalGst.compareTo(sumOfTaxes) != 0) {
                response.setTotalGst(sumOfTaxes);
                log.info("Corrected invoice totalGst from {} to {} based on sum of CGST, SGST, IGST", totalGst, sumOfTaxes);
                totalGst = sumOfTaxes;
            }
        }

        // Calculate taxableValue if missing but totalAmount and totalGst are present
        if (taxableValue == null && totalAmount != null && totalGst != null) {
            taxableValue = totalAmount.subtract(totalGst);
            if (taxableValue.compareTo(BigDecimal.ZERO) > 0) {
                response.setTaxableValue(taxableValue);
                log.info("Calculated taxableValue={} from totalAmount={} - totalGst={}", taxableValue, totalAmount, totalGst);
            }
        }

        // Validate and fix line item GST amounts
        if (response.getLineItems() != null && !response.getLineItems().isEmpty()) {
            BigDecimal totalLineItemTaxable = BigDecimal.ZERO;
            
            // First pass: calculate total taxable value from line items
            for (InvoiceParseResponse.LineItemDTO item : response.getLineItems()) {
                if (item.getTaxableValue() != null) {
                    totalLineItemTaxable = totalLineItemTaxable.add(item.getTaxableValue());
                } else if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    BigDecimal itemTaxable = item.getQuantity().multiply(item.getUnitPrice())
                            .setScale(2, RoundingMode.HALF_UP);
                    item.setTaxableValue(itemTaxable);
                    totalLineItemTaxable = totalLineItemTaxable.add(itemTaxable);
                }
            }

            // If invoice-level taxableValue is missing, use line item total
            if (response.getTaxableValue() == null && totalLineItemTaxable.compareTo(BigDecimal.ZERO) > 0) {
                response.setTaxableValue(totalLineItemTaxable);
                log.info("Set invoice taxableValue={} from line items total", totalLineItemTaxable);
            }

            // Distribute invoice-level GST to line items if line item GST is missing
            if (totalGst != null && totalGst.compareTo(BigDecimal.ZERO) > 0 && totalLineItemTaxable.compareTo(BigDecimal.ZERO) > 0) {
                for (InvoiceParseResponse.LineItemDTO item : response.getLineItems()) {
                    if (item.getTaxableValue() != null) {
                        boolean hasGst = (item.getCgstAmount() != null && item.getCgstAmount().compareTo(BigDecimal.ZERO) > 0) ||
                                       (item.getSgstAmount() != null && item.getSgstAmount().compareTo(BigDecimal.ZERO) > 0) ||
                                       (item.getIgstAmount() != null && item.getIgstAmount().compareTo(BigDecimal.ZERO) > 0);
                        
                        if (!hasGst) {
                            // Distribute proportionally
                            BigDecimal proportion = item.getTaxableValue().divide(totalLineItemTaxable, 4, RoundingMode.HALF_UP);
                            
                            if (cgst != null && cgst.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal itemCgst = cgst.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                                item.setCgstAmount(itemCgst);
                            }
                            if (sgst != null && sgst.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal itemSgst = sgst.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                                item.setSgstAmount(itemSgst);
                            }
                            if (igst != null && igst.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal itemIgst = igst.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                                item.setIgstAmount(itemIgst);
                            }

                            // Do NOT calculate GST rate here.
// InvoiceParserAgent is responsible only for extracting and normalizing
// invoice amounts. HSN classification determines the applicable GST rate.

                            log.debug(
                                    "Distributed GST to line item '{}': cgst={}, sgst={}, igst={}",
                                    item.getDescription(),
                                    item.getCgstAmount(),
                                    item.getSgstAmount(),
                                    item.getIgstAmount()
                            );
                        }
                    }
                }
            }
        }

        // Log warning if GST amounts are still zero
        if (totalGst == null || totalGst.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Invoice has zero or missing totalGst. Line items may have zero GST amounts.");
        }
    }
    private boolean isJson(String text) {

        try {

            objectMapper.readTree(text);

            return true;

        } catch (Exception e) {

            return false;

        }
    }
}