package com.jbr.middletier.money.data.primary;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Setter
@Getter
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
