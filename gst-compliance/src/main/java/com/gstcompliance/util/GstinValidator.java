package com.gstcompliance.util;

import org.springframework.stereotype.Component;

@Component
public class GstinValidator {

    public boolean isValid(String gstin) {
        if (gstin == null || gstin.length() != 15) {
            return false;
        }

        // Basic validation: first 2 chars state code
        String stateCode = gstin.substring(0, 2);
        if (!stateCode.matches("\\d{2}")) {
            return false;
        }

        // Last char should be checksum
        // Simple validation for Phase 1 - will be enhanced in Phase 2
        return gstin.matches("\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}\\d{1}[A-Z]{1}\\d{1}");
    }

    public String extractStateCode(String gstin) {
        if (isValid(gstin)) {
            return gstin.substring(0, 2);
        }
        return null;
    }
}