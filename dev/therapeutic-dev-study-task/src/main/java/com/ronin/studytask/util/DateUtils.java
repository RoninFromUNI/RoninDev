package com.ronin.studytask.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date comparison and formatting operations.
 */
public class DateUtils {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private DateUtils() {
        // utility class — prevent instantiation
    }

    /**
     * Compares two LocalDate values for chronological ordering.
     * Returns a negative value if {@code a} is before {@code b},
     * zero if they are equal, and a positive value if {@code a} is after {@code b}.
     *
     * @param a the first date
     * @param b the second date
     * @return comparison result suitable for use in sorting
     */
    public static int compare(LocalDate a, LocalDate b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;   // nulls sort to the end
        if (b == null) return -1;
        return b.compareTo(a);
    }

    /**
     * Returns true if the given due date is in the past (before today).
     *
     * @param dueDate the date to check
     * @return true if overdue, false otherwise (or if null)
     */
    public static boolean isOverdue(LocalDate dueDate) {
        if (dueDate == null) return false;
        return dueDate.isBefore(LocalDate.now());
    }

    /**
     * Returns the number of days between today and the given date.
     * Positive if the date is in the future, negative if in the past.
     *
     * @param date the target date
     * @return number of days until the date
     */
    public static long daysUntil(LocalDate date) {
        if (date == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    /**
     * Formats a LocalDate as "dd MMM yyyy" (e.g., "15 Mar 2026").
     *
     * @param date the date to format
     * @return formatted date string, or "N/A" if null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DISPLAY_FORMAT);
    }
}
