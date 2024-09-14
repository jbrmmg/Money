package com.jbr.middletier.money.reconciliation;

import com.jbr.middletier.money.data.primary.ReconcileFormat;
import com.jbr.middletier.money.data.primary.repository.ReconcileFormatRepository;
import com.jbr.middletier.money.manager.ReconcileFileLine;
import com.jbr.middletier.money.util.FinancialAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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

        throw new FileFormatException(line.getLineNumber(),"Required index out of range");
    }

    public LocalDate getDate(ReconcileFileLine line) throws FileFormatException {
        String value = getColumnValue(getDateColumn(),line);

        // If the date is a specific value then ignore it.
        // JBR-441: make this part of the format description database data
        if(value.equalsIgnoreCase("pending")) {
            return null;
        }

        LocalDate result;
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(getDateFormat())
                    .toFormatter(Locale.ENGLISH);

            result = LocalDate.parse(value,formatter);
        } catch (DateTimeParseException ex) {
            throw new FileFormatException(line.getLineNumber(),"Cannot convert the string to a date " + ex.getMessage());
        }

        return result;
    }

    private BigDecimal internalGetAmount(ReconcileFileLine line, int index) throws FileFormatException {
        String value = getColumnValue(index,line).replace(",","").replace("£","");
        if(value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal numericValue;

        try {
            numericValue =  new BigDecimal(value);

            if(getReverse()) {
                numericValue = FinancialAmount.flipSign(numericValue);
            }
        } catch (NumberFormatException ex) {
            throw new FileFormatException(line.getLineNumber(),"Cannot convert the string to an amount " + ex.getMessage());
        }

        return numericValue;
    }

    private BigDecimal internalGetSplitAmount(ReconcileFileLine line) throws FileFormatException {
        BigDecimal inAmount = internalGetAmount(line,getAmountInColumn());
        BigDecimal outAmount = FinancialAmount.flipSign(internalGetAmount(line,getAmountOutColumn()));

        if(FinancialAmount.negative(inAmount)) {
            inAmount = FinancialAmount.flipSign(inAmount);
        }

        if(FinancialAmount.positive(outAmount)) {
            outAmount = FinancialAmount.flipSign(outAmount);
        }

        return inAmount.add(outAmount);
    }

    public BigDecimal getAmount(ReconcileFileLine line) throws FileFormatException {
        if(getSingleAmount()) {
            return internalGetAmount(line,getAmountInColumn());
        }

        return internalGetSplitAmount(line);
    }

    public String getDescription(ReconcileFileLine line) throws FileFormatException {
        String description = getColumnValue(getDescriptionColumn(),line).trim().replaceAll("[^\\da-zA-Z ./?\\-*#'&():+-,!]","");
        return description.substring(0, Math.min(description.length(), 40));
    }

    public String getAccountId() {
        if(this.reconcileFormat != null && this.reconcileFormat.getAccount() != null) {
            return this.reconcileFormat.getAccount().getId();
        }

        return null;
    }
}
