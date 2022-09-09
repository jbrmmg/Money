package com.jbr.middletier.money.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionString {
    public static String formattedTransactionString(Date date, double amount) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date) + String.format("%.2f",amount);
    }
}
