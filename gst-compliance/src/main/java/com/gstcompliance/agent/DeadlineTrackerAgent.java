package com.gstcompliance.agent;

import com.gstcompliance.agent.base.BaseAgent;
import com.gstcompliance.service.PenaltyCalculatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class DeadlineTrackerAgent extends BaseAgent<Map<String, Object>, List<Map<String, Object>>> {

    private final PenaltyCalculatorService penaltyCalculatorService;

    public DeadlineTrackerAgent(PenaltyCalculatorService penaltyCalculatorService) {
        super("DeadlineTracker");
        this.penaltyCalculatorService = penaltyCalculatorService;
    }

    @Override
    protected List<Map<String, Object>> process(Map<String, Object> request) throws Exception {
        log.info("Calculating deadlines for user");

        String gstin = (String) request.get("gstin");
        String turnoverSlab = (String) request.getOrDefault("turnoverSlab", "BELOW_5CR");
        int month = (int) request.getOrDefault("month", LocalDate.now().getMonthValue());
        int year = (int) request.getOrDefault("year", LocalDate.now().getYear());

        List<Map<String, Object>> deadlines = new ArrayList<>();

        // GSTR-3B deadline
        Map<String, Object> gstr3b = calculateDeadline("GSTR-3B", month, year, turnoverSlab);
        deadlines.add(gstr3b);

        // GSTR-1 deadline
        Map<String, Object> gstr1 = calculateDeadline("GSTR-1", month, year, turnoverSlab);
        deadlines.add(gstr1);

        // Annual return (GSTR-9)
        Map<String, Object> gstr9 = calculateAnnualDeadline(year);
        deadlines.add(gstr9);

        log.info("Generated {} deadlines", deadlines.size());
        return deadlines;
    }

    private Map<String, Object> calculateDeadline(String formType, int month, int year, String turnoverSlab) {
        LocalDate dueDate;
        int dayOfMonth = formType.equals("GSTR-3B") ? 20 : 11;

        dueDate = LocalDate.of(year, month, 1)
                .plusMonths(1)
                .withDayOfMonth(dayOfMonth);

        // Adjust for weekends/holidays (simplified)
        if (dueDate.getDayOfWeek().getValue() == 6 || dueDate.getDayOfWeek().getValue() == 7) {
            dueDate = dueDate.plusDays(2);
        }

        long daysRemaining = LocalDate.now().until(dueDate).getDays();
        boolean isOverdue = daysRemaining < 0;

        Map<String, Object> deadline = new HashMap<>();
        deadline.put("formType", formType);
        deadline.put("dueDate", dueDate.toString());
        deadline.put("daysRemaining", Math.max(0, daysRemaining));
        deadline.put("isOverdue", isOverdue);
        deadline.put("priority", getPriority(daysRemaining));
        deadline.put("description", formType + " filing");

        // Calculate penalty if overdue
        if (isOverdue) {
            Map<String, Object> penalty = penaltyCalculatorService.calculateLateFeeByTurnover(
                    turnoverSlab, dueDate);
            deadline.put("penaltyPerDay", penalty.get("penaltyPerDay"));
            deadline.put("totalPenalty", penalty.get("totalPenalty"));
            deadline.put("daysOverdue", Math.abs(daysRemaining));
        } else {
            deadline.put("penaltyPerDay", 0);
            deadline.put("totalPenalty", 0);
            deadline.put("daysOverdue", 0);
        }

        return deadline;
    }

    private Map<String, Object> calculateAnnualDeadline(int year) {
        LocalDate dueDate = LocalDate.of(year + 1, 12, 31);
        long daysRemaining = LocalDate.now().until(dueDate).getDays();

        Map<String, Object> deadline = new HashMap<>();
        deadline.put("formType", "GSTR-9");
        deadline.put("dueDate", dueDate.toString());
        deadline.put("daysRemaining", Math.max(0, daysRemaining));
        deadline.put("isOverdue", daysRemaining < 0);
        deadline.put("priority", daysRemaining < 30 ? "HIGH" : "MEDIUM");
        deadline.put("description", "Annual return filing");
        deadline.put("penaltyPerDay", 0);
        deadline.put("totalPenalty", 0);
        deadline.put("daysOverdue", 0);

        return deadline;
    }

    private String getPriority(long daysRemaining) {
        if (daysRemaining < 0) return "CRITICAL";
        if (daysRemaining <= 3) return "HIGH";
        if (daysRemaining <= 15) return "MEDIUM";
        return "LOW";
    }
}