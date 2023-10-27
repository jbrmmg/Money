package com.jbr.middletier.money.data;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="rec_file_tran")
public class ReconciliationFileTransaction {
    @EmbeddedId
    private ReconciliationFileTransactionId id;
}
