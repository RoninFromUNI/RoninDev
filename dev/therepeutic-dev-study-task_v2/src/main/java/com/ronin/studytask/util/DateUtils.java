package com.ronin.studytask.util;

import java.time.LocalDate;

public class DateUtils {

    private DateUtils()
    {

    }
    public static int compareDates(LocalDate a, LocalDate b)
    {
        return b.compareTo(a);
    }
    public static boolean isOverdue(LocalDate dueDate, LocalDate today)
    {
        return dueDate.isBefore(today);
    }
}
