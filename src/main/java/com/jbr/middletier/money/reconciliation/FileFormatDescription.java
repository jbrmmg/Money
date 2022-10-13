package com.jbr.middletier.money.reconciliation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FileFormatDescription {
    private boolean valid;
    private int firstLine;
    private int date;
    private String dateFormat;
    private int description;
    private int amountIn;
    private int amountOut;
    private boolean reverse;

    public FileFormatDescription(String titleLine, int line) {
        this.valid = false;

        if(titleLine.equalsIgnoreCase("Date,Description,Card Member,Account #,Amount")) {
            // This is an AMEX file
            this.valid = true;
            this.firstLine = line + 1;
            this.date = 0;
            this.description = 1;
            this.amountIn = 4;
            this.amountOut = 4;
            this.dateFormat = "dd/MM/yyyy";
            this.reverse = true;
            return;
        }

        if(titleLine.equalsIgnoreCase("Date,Description,Amount,Balance")) {
            // This is an First Direct file
            this.valid = true;
            this.firstLine = line + 1;
            this.date = 0;
            this.description = 1;
            this.amountIn = 2;
            this.amountOut = 2;
            this.dateFormat = "dd/MM/yyyy";
            this.reverse = false;
            return;
        }

        if(titleLine.equalsIgnoreCase("Date Processed,Description,Amount,")) {
            // This is an JLP file
            this.valid = true;
            this.firstLine = line + 1;
            this.date = 0;
            this.description = 1;
            this.amountIn = 2;
            this.amountOut = 2;
            this.dateFormat = "dd-MMM-yyyy";
            this.reverse = true;
            return;
        }

        if(titleLine.equalsIgnoreCase("Date,Description,Amount(GBP)")) {
            // This is an JLP 2 file
            this.valid = true;
            this.firstLine = line + 1;
            this.date = 0;
            this.description = 1;
            this.amountIn = 2;
            this.amountOut = 2;
            this.dateFormat = "dd/MM/yyyy";
            this.reverse = true;
            return;
        }

        if(titleLine.equalsIgnoreCase("\"Date\",\"Transactions\",\"Location\",\"Paid out\",\"Paid in\"")) {
            // This is a nationwide file
            this.valid = true;
            this.firstLine = line + 5;
            this.date = 0;
            this.description = 1;
            this.amountIn = 4;
            this.amountOut = 3;
            this.dateFormat = "dd MMM yyyy";
            this.reverse = false;
            return;
        }
    }

    public FileFormatDescription() {
        this.valid = false;
    }

    public boolean getValid() {
        return this.valid;
    }

    public int getFirstLine() {
        return this.firstLine;
    }

    private String unQuote(String quoted) {
        if(quoted.startsWith("\"") && quoted.length() >= 2) {
            return quoted.substring(1,quoted.length()-1);
        }

        return quoted;
    }

    private String getColumnValue(int index, List<String> columns) throws FileFormatException {
        if(index < columns.size()) {
            return unQuote(columns.get(index).trim());
        }

        throw new FileFormatException("Required index out of range");
    }

    public LocalDate getDate(List<String> columns) throws FileFormatException {
        String value = getColumnValue(this.date,columns);

        LocalDate result;
        try {
            result = LocalDate.parse(value,DateTimeFormatter.ofPattern(this.dateFormat));
        } catch (DateTimeParseException ex) {
            throw new FileFormatException("Cannot convert the string to a date " + ex.getMessage());
        }

        return result;
    }

    private double internalGetAmount(List<String> columns, int index) throws FileFormatException {
        String value = getColumnValue(index,columns).replace(",","").replace("£","");
        if(value.trim().length() == 0) {
            return 0;
        }

        double numericValue;

        try {
            numericValue = Double.parseDouble(value);

            if(reverse) {
                numericValue *= -1;
            }
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Cannot convert the string to an amount " + ex.getMessage());
        }

        return numericValue;
    }

    private double internalGetSplitAmount(List<String> columns) throws FileFormatException {
        double inAmount = internalGetAmount(columns,amountIn);
        double outAmount = internalGetAmount(columns,amountOut) * -1;

        return inAmount + outAmount;
    }

    public double getAmount(List<String> columns) throws FileFormatException {
        if(this.amountIn == this.amountOut) {
            return internalGetAmount(columns,this.amountIn);
        }

        return internalGetSplitAmount(columns);
    }

    public String getDescription(List<String> columns) throws FileFormatException {
        return getColumnValue(this.description,columns);
    }
}
