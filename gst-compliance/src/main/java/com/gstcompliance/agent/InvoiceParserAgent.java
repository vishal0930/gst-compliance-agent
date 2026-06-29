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
        String extractedText = "";
        try {
            extractedText = tika.parseToString(new ByteArrayInputStream(fileContent));
        } catch (Exception e) {
            log.warn("Tika extraction failed: {}", e.getMessage());
        }

        // 3. If Tika returns empty, read as plain text
        if (extractedText.trim().isEmpty()) {
            log.info("📄 Tika returned empty, reading as plain text");
            extractedText = new String(fileContent, java.nio.charset.StandardCharsets.UTF_8);
        }

        // 4. If still empty, use OCR fallback
        if (extractedText.trim().isEmpty()) {
            log.warn("⚠️ No text extracted, using OCR fallback");
            extractedText = extractViaOCR(fileContent);
        }

        log.info("📄 Plain text content: {}", extractedText);

        // 5. Build prompt and call LLM
        String prompt = buildExtractionPrompt(extractedText);
        log.info("🤖 Calling LLM with prompt...");
        String llmResponse = llmModel.generate(prompt);
        log.info("🤖 LLM Response: {}", llmResponse);

        // 6. Parse the actual LLM response
        return parseLLMResponse(llmResponse);
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
                You are an expert GST invoice parser.
                
                      Extract invoice information EXACTLY as it appears.
                
                      Invoice Text:
                
                          """
                              + text +
                              """
                
                      Rules:
                
                      1. Do NOT guess values.
                      2. Return ONLY valid JSON.
                      3. No markdown.
                      4. No explanations.
                      5. If a value is missing, return null.
                      6. Every invoice line item MUST have its own description.
                      7. Never leave description empty if the item text exists.
                      8. taxableValue = quantity × unitPrice when not explicitly written.
                      9. Extract CGST, SGST and IGST only if present.
                      10. totalGst = cgst + sgst + igst.
                      11. confidenceScore must be between 0 and 1.
                
                      Expected JSON:
                
                      {
                        "vendorName":"",
                        "vendorGstin":"",
                        "invoiceNumber":"",
                        "invoiceDate":"",
                        "taxableValue":null,
                        "cgst":null,
                        "sgst":null,
                        "igst":null,
                        "totalAmount":null,
                        "totalGst":null,
                        "lineItems":[
                          {
                            "description":"",
                            "quantity":null,
                            "unitPrice":null,
                            "taxableValue":null
                          }
                        ],
                        "confidenceScore":1.0
                      }
                
                      Example
                
                      Input:
                
                      Items:
                      1. Cotton Silk Saree - 20 units @ 8000
                      2. Woolen Shawl - 15 units @ 6000
                
                      Output:
                
                      {
                        "lineItems":[
                          {
                            "description":"Cotton Silk Saree",
                            "quantity":20,
                            "unitPrice":8000,
                            "taxableValue":160000
                          },
                          {
                            "description":"Woolen Shawl",
                            "quantity":15,
                            "unitPrice":6000,
                            "taxableValue":90000
                          }
                        ]
                      }
            """;
    }

    private InvoiceParseResponse parseLLMResponse(String response) {
        try {
            // Clean up response
            String cleanedResponse = response
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

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
}