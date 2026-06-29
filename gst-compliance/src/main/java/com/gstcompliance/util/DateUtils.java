package com.gstcompliance.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class DateUtils {

    // renamed or used consistently
    private static final DateTimeFormatter DD_MM_YYYY =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter DD_MMM_YYYY =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH);

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Fix: Changed from DATE_FORMATTER to DD_MM_YYYY
    public String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DD_MM_YYYY);
    }

    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    // Fix: Changed from DATE_FORMATTER to DD_MM_YYYY
    public static LocalDate parseDate(String dateStr) {

        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, ISO);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(dateStr, DD_MM_YYYY);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(dateStr, DD_MMM_YYYY);
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Unsupported date format: " + dateStr);
    }

    public long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public boolean isPastDue(LocalDate dueDate) {
        if (dueDate == null) return false;
        return LocalDate.now().isAfter(dueDate);
    }

    public int getDaysRemaining(LocalDate dueDate) {
        if (dueDate == null) return 0;
        return (int) LocalDate.now().until(dueDate, ChronoUnit.DAYS);
    }

    public String getMonthYear(int month, int year) {
        return String.format("%02d-%04d", month, year);
    }
}