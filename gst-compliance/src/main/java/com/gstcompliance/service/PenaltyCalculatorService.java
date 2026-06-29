package com.gstcompliance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PenaltyCalculatorService {

    private static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("50.00");
    private static final BigDecimal INTEREST_RATE_PA = new BigDecimal("18.0");
    private static final BigDecimal ANNUAL_DAYS = new BigDecimal("365");

    public Map<String, Object> calculatePenalty(LocalDate dueDate, BigDecimal taxAmount) {
        log.info("Calculating penalty for due date: {}, tax amount: {}", dueDate, taxAmount);

        Map<String, Object> result = new HashMap<>();

        if (LocalDate.now().isBefore(dueDate)) {
            result.put("status", "ON_TIME");
            result.put("daysOverdue", 0);
            result.put("lateFee", BigDecimal.ZERO);
            result.put("interest", BigDecimal.ZERO);
            result.put("totalPenalty", BigDecimal.ZERO);
            return result;
        }

        long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());

        // Late fee calculation
        BigDecimal lateFee = LATE_FEE_PER_DAY.multiply(new BigDecimal(daysOverdue));

        // Interest calculation (18% p.a.)
        BigDecimal dailyInterestRate = INTEREST_RATE_PA.divide(ANNUAL_DAYS, 4, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal interest = taxAmount.multiply(dailyInterestRate)
                .multiply(new BigDecimal(daysOverdue))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPenalty = lateFee.add(interest);

        result.put("status", "OVERDUE");
        result.put("daysOverdue", daysOverdue);
        result.put("lateFee", lateFee);
        result.put("interest", interest);
        result.put("totalPenalty", totalPenalty);
        result.put("severity", getSeverity(daysOverdue));

        log.info("Penalty calculated: total ₹{}", totalPenalty);
        return result;
    }

    private String getSeverity(long daysOverdue) {
        if (daysOverdue <= 3) return "LOW";
        if (daysOverdue <= 15) return "MEDIUM";
        if (daysOverdue <= 30) return "HIGH";
        return "CRITICAL";
    }

    public Map<String, Object> calculateLateFeeByTurnover(String turnoverSlab, LocalDate dueDate) {
        // Different penalties for different turnover slabs
        BigDecimal basePenalty = LATE_FEE_PER_DAY;

        if ("ABOVE_5CR".equals(turnoverSlab)) {
            basePenalty = new BigDecimal("100.00");
        } else if ("BELOW_5CR".equals(turnoverSlab)) {
            basePenalty = new BigDecimal("25.00");
        }

        long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        if (daysOverdue <= 0) {
            daysOverdue = 0;
        }

        BigDecimal totalPenalty = basePenalty.multiply(new BigDecimal(daysOverdue));

        Map<String, Object> result = new HashMap<>();
        result.put("penaltyPerDay", basePenalty);
        result.put("daysOverdue", daysOverdue);
        result.put("totalPenalty", totalPenalty);

        return result;
    }
}