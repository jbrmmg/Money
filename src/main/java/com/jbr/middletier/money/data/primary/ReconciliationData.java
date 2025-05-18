package com.jbr.middletier.money.data.primary;

import com.jbr.middletier.money.util.TransactionString;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jason on 11/04/17.
 */
@Getter
@Entity
@Table(name="reconciliation_data")
public class ReconciliationData {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Setter
    @JoinColumn(name="category")
    @ManyToOne
    private Category category;

    @Setter
    @Column(name="description", length = 40)
    private String description;

    @Setter
    @Column(name="date")
    private LocalDate date;

    @Setter
    @Column(name="amount")
    private BigDecimal amount;

    @Setter
    @Column(name="account_id")
    private String accountId;

    @Override
    public String toString() {
        return TransactionString.formattedTransactionString(this.date,this.amount);
    }
}
