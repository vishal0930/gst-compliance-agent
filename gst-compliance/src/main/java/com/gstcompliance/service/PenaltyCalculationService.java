package com.gstcompliance.service;

import com.gstcompliance.exception.ResourceNotFoundException;
import com.gstcompliance.model.PenaltyRecord;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.PenaltyRecordRepository;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyCalculationService {

    private final PenaltyRecordRepository penaltyRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // GST Penalty Constants (as per CBIC guidelines)
    private static final BigDecimal LATE_FEE_PER_DAY = BigDecimal.valueOf(200); // ₹200 per day
    private static final BigDecimal INTEREST_RATE = BigDecimal.valueOf(18); // 18% per annum
    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);

    /**
     * Calculate penalty for a specific period and return type
     */
    @Transactional
    public PenaltyRecord calculatePenalty(String email,
                                         String taxPeriod,
                                         PenaltyRecord.ReturnType returnType,
                                         LocalDate dueDate,
                                         BigDecimal taxLiability) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // Check if penalty already calculated for this period
        PenaltyRecord existing = penaltyRecordRepository.findByUserIdAndTaxPeriodAndReturnType(
                user.getId(),
                taxPeriod,
                returnType
        );

        if (existing != null && existing.getStatus() != PenaltyRecord.PenaltyStatus.PAID) {
            log.info("Penalty already calculated for user {} period {} return type {}", email, taxPeriod, returnType);
            return existing;
        }

        LocalDate today = LocalDate.now();
        LocalDate filingDate = today; // Assuming not filed yet

        // Calculate delay days
        long delayDays = 0;
        if (filingDate.isAfter(dueDate)) {
            delayDays = ChronoUnit.DAYS.between(dueDate, filingDate);
        }

        if (delayDays == 0) {
            log.info("No delay for user {} period {} return type {}", email, taxPeriod, returnType);
            return null;
        }

        // Calculate late fee (₹200 per day)
        BigDecimal lateFeeTotal = LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(delayDays));

        // Calculate interest (18% per annum on tax liability)
        BigDecimal interestAmount = BigDecimal.ZERO;
        if (taxLiability != null && taxLiability.compareTo(BigDecimal.ZERO) > 0) {
            // Interest = Tax Liability * (18/100) * (Delay Days / 365)
            BigDecimal interestRateDecimal = INTEREST_RATE.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal timeFraction = BigDecimal.valueOf(delayDays).divide(DAYS_IN_YEAR, 4, RoundingMode.HALF_UP);
            interestAmount = taxLiability
                    .multiply(interestRateDecimal)
                    .multiply(timeFraction)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Total penalty
        BigDecimal totalPenalty = lateFeeTotal.add(interestAmount);

        PenaltyRecord penaltyRecord = PenaltyRecord.builder()
                .user(user)
                .taxPeriod(taxPeriod)
                .returnType(returnType)
                .dueDate(dueDate)
                .filingDate(filingDate)
                .delayDays((int) delayDays)
                .interestRate(INTEREST_RATE)
                .interestAmount(interestAmount)
                .lateFeePerDay(LATE_FEE_PER_DAY)
                .lateFeeTotal(lateFeeTotal)
                .totalPenalty(totalPenalty)
                .taxLiability(taxLiability)
                .status(PenaltyRecord.PenaltyStatus.CALCULATED)
                .notes(String.format("Calculated %d days delay", delayDays))
                .build();

        PenaltyRecord saved = penaltyRecordRepository.save(penaltyRecord);

        // Send notification
        notificationService.notifyPenaltyCalculated(email, taxPeriod, totalPenalty.doubleValue());

        log.info("Penalty calculated for user {} period {}: delay={} days, lateFee={}, interest={}, total={}",
                email, taxPeriod, delayDays, lateFeeTotal, interestAmount, totalPenalty);

        return saved;
    }

    /**
     * Calculate penalties for all pending returns for a user
     */
    @Transactional
    public List<PenaltyRecord> calculateAllPendingPenalties(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        List<PenaltyRecord> penalties = penaltyRecordRepository.findByUserIdAndStatus(
                user.getId(),
                PenaltyRecord.PenaltyStatus.PENDING
        );

        for (PenaltyRecord penalty : penalties) {
            calculatePenalty(
                    email,
                    penalty.getTaxPeriod(),
                    penalty.getReturnType(),
                    penalty.getDueDate(),
                    penalty.getTaxLiability()
            );
        }

        return penalties;
    }

    /**
     * Mark penalty as paid
     */
    @Transactional
    public void markAsPaid(UUID penaltyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        PenaltyRecord penalty = penaltyRecordRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty record not found"));

        if (!penalty.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Penalty record not found");
        }

        penalty.setStatus(PenaltyRecord.PenaltyStatus.PAID);
        penalty.setPaidAt(java.time.LocalDateTime.now());
        penaltyRecordRepository.save(penalty);

        log.info("Penalty {} marked as paid for user {}", penaltyId, email);
    }

    /**
     * Get all penalties for a user
     */
    public List<PenaltyRecord> getUserPenalties(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return penaltyRecordRepository.findByUserIdOrderByCalculatedAtDesc(user.getId());
    }

    /**
     * Get pending penalty total for a user
     */
    public BigDecimal getPendingPenaltyTotal(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        BigDecimal total = penaltyRecordRepository.sumPendingPenaltiesByUserId(user.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Create pending penalty record (to be calculated later)
     */
    @Transactional
    public PenaltyRecord createPendingPenalty(String email,
                                              String taxPeriod,
                                              PenaltyRecord.ReturnType returnType,
                                              LocalDate dueDate,
                                              BigDecimal taxLiability) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // Check if already exists
        PenaltyRecord existing = penaltyRecordRepository.findByUserIdAndTaxPeriodAndReturnType(
                user.getId(),
                taxPeriod,
                returnType
        );

        if (existing != null) {
            return existing;
        }

        PenaltyRecord penaltyRecord = PenaltyRecord.builder()
                .user(user)
                .taxPeriod(taxPeriod)
                .returnType(returnType)
                .dueDate(dueDate)
                .taxLiability(taxLiability)
                .status(PenaltyRecord.PenaltyStatus.PENDING)
                .notes("Pending calculation - will be calculated after due date")
                .build();

        return penaltyRecordRepository.save(penaltyRecord);
    }
}
