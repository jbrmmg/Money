package com.jbr.middletier.money.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TransactionString {
    private TransactionString() {
        // Hide the public constructor.
    }

    public static String formattedTransactionString(LocalDate date, double amount) {
        return DateTimeFormatter.ofPattern("yyyyMMdd").format(date) + String.format("%.2f",amount);
    }
}
