package com.jbr.middletier.money.util;

import com.jbr.middletier.money.schedule.AdjustmentType;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class DateAdjustmentUtil {
    private DateAdjustmentUtil() {}

    public static LocalDate adjustForWeekend(LocalDate date, AdjustmentType adjustment) {
        if (adjustment == null) {
            return date;
        }

        int adjustmentAmt = switch (adjustment) {
            case AT_FORWARD -> 1;
            case AT_BACKWARD -> -1;
            default -> 0;
        };

        if (adjustmentAmt != 0) {
            while (DayOfWeek.SUNDAY.equals(date.getDayOfWeek()) || DayOfWeek.SATURDAY.equals(date.getDayOfWeek())) {
                date = date.plusDays(adjustmentAmt);
            }
        }

        return date;
    }
}
