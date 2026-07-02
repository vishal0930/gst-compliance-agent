package com.gstcompliance.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Shared numeric conversion utilities.
 * Centralises toBigDecimal so it is not duplicated across InvoiceService,
 * AgentPipelineService and HsnLookupService.
 */
@Slf4j
public final class ConversionUtils {

    private ConversionUtils() {}

    /**
     * Safely converts any numeric-like value to BigDecimal.
     * Returns null when the value is null or blank string.
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;

        if (value instanceof BigDecimal bd) return bd;

        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());

        if (value instanceof String s) {
            s = s.trim();
            if (s.isBlank()) return null;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                log.warn("Unable to convert '{}' to BigDecimal", s);
                return null;
            }
        }

        log.warn("Unsupported numeric value type: {}", value.getClass().getName());
        return null;
    }
}
