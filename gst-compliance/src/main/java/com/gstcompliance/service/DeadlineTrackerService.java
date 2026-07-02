package com.gstcompliance.service;

import com.gstcompliance.model.PenaltyRecord;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineTrackerService {

    private final UserRepository userRepository;
    private final PenaltyCalculationService penaltyCalculationService;
    private final NotificationService notificationService;

    // GST Filing Deadlines (GSTR-3B)
    // Due: 20th of next month
    // Extended: 22nd/24th of next month (for certain states)
    
    /**
     * Track all deadlines and send notifications
     * This should run daily at 02:00 AM
     */
    @Transactional
    public void trackDeadlines() {
        log.info("Starting deadline tracking for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {
                trackUserDeadlines(user);
            } catch (Exception e) {
                log.error("Failed to track deadlines for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
        
        log.info("Completed deadline tracking for {} users", users.size());
    }

    /**
     * Track deadlines for a specific user
     */
    private void trackUserDeadlines(User user) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);
        
        // Get current period (previous month)
        YearMonth currentPeriod = YearMonth.from(today.minusMonths(1));
        String taxPeriod = String.format("%02d-%04d", currentPeriod.getMonthValue(), currentPeriod.getYear());
        
        // GSTR-3B due date (20th of next month)
        LocalDate gstr3bDueDate = LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), 20);
        
        // Check if deadline is in 7 days
        if (today.plusDays(7).isEqual(gstr3bDueDate) || today.plusDays(7).isAfter(gstr3bDueDate)) {
            long daysUntilDue = ChronoUnit.DAYS.between(today, gstr3bDueDate);
            if (daysUntilDue > 0 && daysUntilDue <= 7) {
                notificationService.notifyDeadlineWarning(
                        user.getEmail(),
                        "GSTR-3B",
                        gstr3bDueDate.toString()
                );
                log.info("Sent deadline warning to {} for GSTR-3B due in {} days", user.getEmail(), daysUntilDue);
            }
        }
        
        // Check if deadline is tomorrow
        if (today.plusDays(1).isEqual(gstr3bDueDate)) {
            notificationService.notifyDeadlineTomorrow(
                    user.getEmail(),
                    "GSTR-3B",
                    gstr3bDueDate.toString()
            );
            log.info("Sent deadline tomorrow alert to {} for GSTR-3B", user.getEmail());
        }
        
        // Check if deadline has passed and penalty needs calculation
        if (today.isAfter(gstr3bDueDate)) {
            // Create pending penalty record if not exists
            // Tax liability would need to be calculated from actual data
            // For now, we'll create with zero liability - this should be updated with actual calculation
            penaltyCalculationService.createPendingPenalty(
                    user.getEmail(),
                    taxPeriod,
                    PenaltyRecord.ReturnType.GSTR3B,
                    gstr3bDueDate,
                    java.math.BigDecimal.ZERO // TODO: Calculate actual tax liability
            );
            
            // Calculate penalty if not already calculated
            penaltyCalculationService.calculatePenalty(
                    user.getEmail(),
                    taxPeriod,
                    PenaltyRecord.ReturnType.GSTR3B,
                    gstr3bDueDate,
                    java.math.BigDecimal.ZERO // TODO: Calculate actual tax liability
            );
        }
    }

    /**
     * Get upcoming deadlines for a user
     */
    public List<DeadlineInfo> getUpcomingDeadlines(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);
        
        List<DeadlineInfo> deadlines = new java.util.ArrayList<>();
        
        // GSTR-3B deadline
        LocalDate gstr3bDueDate = LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), 20);
        deadlines.add(new DeadlineInfo(
                "GSTR-3B",
                gstr3bDueDate,
                ChronoUnit.DAYS.between(today, gstr3bDueDate)
        ));
        
        return deadlines;
    }

    /**
     * DTO for deadline information
     */
    public static class DeadlineInfo {
        private final String formType;
        private final LocalDate dueDate;
        private final long daysUntilDue;
        
        public DeadlineInfo(String formType, LocalDate dueDate, long daysUntilDue) {
            this.formType = formType;
            this.dueDate = dueDate;
            this.daysUntilDue = daysUntilDue;
        }
        
        public String getFormType() { return formType; }
        public LocalDate getDueDate() { return dueDate; }
        public long getDaysUntilDue() { return daysUntilDue; }
    }
}
