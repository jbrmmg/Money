package com.jbr.middletier.money.reconciliation;

import com.jbr.middletier.money.data.ReconcileFormat;
import com.jbr.middletier.money.dataaccess.ReconcileFormatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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

    private String getColumnValue(int index, List<String> columns) throws FileFormatException {
        if(index < columns.size()) {
            return unQuote(columns.get(index).trim());
        }

        throw new FileFormatException("Required index out of range");
    }

    public LocalDate getDate(List<String> columns) throws FileFormatException {
        String value = getColumnValue(getDateColumn(),columns);

        LocalDate result;
        try {
            result = LocalDate.parse(value,DateTimeFormatter.ofPattern(getDateFormat()));
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

            if(getReverse()) {
                numericValue *= -1;
            }
        } catch (NumberFormatException ex) {
            throw new FileFormatException("Cannot convert the string to an amount " + ex.getMessage());
        }

        return numericValue;
    }

    private double internalGetSplitAmount(List<String> columns) throws FileFormatException {
        double inAmount = internalGetAmount(columns,getAmountInColumn());
        double outAmount = internalGetAmount(columns,getAmountOutColumn()) * -1;

        return inAmount + outAmount;
    }

    public double getAmount(List<String> columns) throws FileFormatException {
        if(getSingleAmount()) {
            return internalGetAmount(columns,getAmountInColumn());
        }

        return internalGetSplitAmount(columns);
    }

    public String getDescription(List<String> columns) throws FileFormatException {
        return getColumnValue(getDescriptionColumn(),columns);
    }
}
