package com.jbr.middletier.money.data.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Table(name="transaction_report")
public class TransactionReport {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name="transaction_id")
    private Integer transactionId;

    @Column(name="amount")
    private BigDecimal amount;

    @Column(name="date")
    private String date;

    @Column(name="account_id")
    private String accountId;

    @Column(name="category_id")
    private String categoryId;

    @Column(name="description")
    private String description;

    @Column(name="search_description")
    private String searchDescription;

    @Column(name="opposite_id")
    private Integer oppositeId;

    @Column(name="statement_open_balance")
    private BigDecimal statementOpenBalance;

    @Column(name="statement_month")
    private Integer statementMonth;

    @Column(name="statement_year")
    private Integer statementYear;

    @Column(name="statement_sort")
    private Integer statementSort;

    @Column(name="statement_age")
    private Integer statementAge;

    @Column(name="predicted")
    private Boolean predicted;

    @Column(name="from_reconciliation")
    private Boolean fromReconciliation;

    @Column(name="locked")
    private Boolean locked;

    @Column(name="action_update")
    private Boolean actionUpdate;

    @Column(name="action_reconcile")
    private Boolean actionReconcile;

    @Column(name="action_unreconcile")
    private Boolean actionUnreconcile;

    @Column(name="action_delete")
    private Boolean actionDelete;
}
