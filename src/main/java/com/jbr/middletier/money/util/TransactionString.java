package com.jbr.middletier.money.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TransactionString {
    public static String formattedTransactionString(LocalDate date, double amount) {
        return DateTimeFormatter.ofPattern("yyyyMMdd").format(date) + String.format("%.2f",amount);
    }
}
