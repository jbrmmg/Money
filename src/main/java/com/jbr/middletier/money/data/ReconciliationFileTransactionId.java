package com.jbr.middletier.money.data;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class ReconciliationFileTransactionId implements Serializable {
    @NotNull
    @ManyToOne
    @JoinColumn(name="file_name")
    private ReconciliationFile file;

    @NotNull
    private Integer line;

    public ReconciliationFileTransactionId(ReconciliationFile file, int line) {
        this.file = file;
        this.line = line;
    }

    public ReconciliationFileTransactionId() {
        this.file = null;
        this.line = 0;
    }

    public ReconciliationFile getFile() {
        return file;
    }

    public void setFile(ReconciliationFile file) {
        this.file = file;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof ReconciliationFileTransactionId transactionId)) {
            return false;
        }

        return this.toString().equalsIgnoreCase(transactionId.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return this.file.getName() + "-" + line;
    }
}
