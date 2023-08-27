package com.jbr.middletier.money.reconciliation;

import com.jbr.middletier.money.data.ReconcileFormat;
import com.jbr.middletier.money.dataaccess.ReconcileFormatRepository;
import com.jbr.middletier.money.manager.ReconcileFileLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class FileFormatDescription {
    private static final Logger LOG = LoggerFactory.getLogger(FileFormatDescription.class);

    private ReconcileFormat reconcileFormat;

    public FileFormatDescription(ReconcileFormatRepository reconcileFormatRepository, String titleLine) {
        this.reconcileFormat = null;
        for(ReconcileFormat next : reconcileFormatRepository.findAllByHeaderLine(titleLine)) {
            LOG.info("Found format with id {}", next.getId());
            this.reconcileFormat = next;
        }
    }

    public FileFormatDescription(ReconcileFormat format) {
        this.reconcileFormat = format;
    }

    public FileFormatDescription() {
        this.reconcileFormat = null;
    }

    public boolean getValid() {
        return this.reconcileFormat != null;
    }

    public int getFirstLine() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getFirstLine();

        return 0;
    }

    private int getDateColumn() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getDateColumn();

        return 0;
    }

    private String getDateFormat() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getDateFormat();

        return "dd/MM/yyyy";
    }

    private boolean getReverse() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getReverse();

        return false;
    }

    private boolean getSingleAmount() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getAmountInColumn().equals(this.reconcileFormat.getAmountOutColumn());

        return false;
    }

    private int getAmountInColumn() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getAmountInColumn();

        return 0;
    }

    private int getAmountOutColumn() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getAmountOutColumn();

        return 0;
    }

    private int getDescriptionColumn() {
        if(this.reconcileFormat != null)
            return this.reconcileFormat.getDescriptionColumn();

        return 0;
    }

    private String unQuote(String quoted) {
        if(quoted.startsWith("\"") && quoted.length() >= 2) {
            return quoted.substring(1,quoted.length()-1);
        }

        return quoted;
    }

    private String getColumnValue(int index, ReconcileFileLine line) throws FileFormatException {
        if(index < line.getColumns().size()) {
            return unQuote(line.getColumns().get(index).trim());
        }

        throw new FileFormatException("Required index out of range");
    }

    public LocalDate getDate(ReconcileFileLine line) throws FileFormatException {
        String value = getColumnValue(getDateColumn(),line);

        LocalDate result;
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(getDateFormat())
                    .toFormatter(Locale.ENGLISH);

            result = LocalDate.parse(value,formatter);
        } catch (DateTimeParseException ex) {
            throw new FileFormatException("Cannot convert the string to a date " + ex.getMessage());
        }

        return result;
    }

    private double internalGetAmount(ReconcileFileLine line, int index) throws FileFormatException {
        String value = getColumnValue(index,line).replace(",","").replace("£","");
        if(value.trim().length() == 0) {
            return 0;
        }

        double numericValue;

        try {
            numericValue = Double.parseDouble(value);

            if(getReverse()) {
                numericValue *= -1;
            }
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Cannot convert the string to an amount " + ex.getMessage());
        }

        return numericValue;
    }

    private double internalGetSplitAmount(ReconcileFileLine line) throws FileFormatException {
        double inAmount = internalGetAmount(line,getAmountInColumn());
        double outAmount = internalGetAmount(line,getAmountOutColumn()) * -1;

        if(inAmount < 0) {
            inAmount *= -1;
        }

        if(outAmount > 0) {
            outAmount *= -1;
        }

        return inAmount + outAmount;
    }

    public double getAmount(ReconcileFileLine line) throws FileFormatException {
        if(getSingleAmount()) {
            return internalGetAmount(line,getAmountInColumn());
        }

        return internalGetSplitAmount(line);
    }

    public String getDescription(ReconcileFileLine line) throws FileFormatException {
        String description = getColumnValue(getDescriptionColumn(),line).trim();
        return description.substring(0, Math.min(description.length(), 40));
    }
}
