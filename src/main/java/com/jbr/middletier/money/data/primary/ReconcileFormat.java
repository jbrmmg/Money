package com.jbr.middletier.money.data.primary;

import jakarta.persistence.*;

@Entity
@Table(name="reconcile_format")
public class ReconcileFormat {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name="header_line")
    private String headerLine;

    @Column(name="first_line")
    private Integer firstLine;

    @Column(name="date_column")
    private Integer dateColumn;

    @Column(name="date_format")
    private String dateFormat;

    @Column(name="description_column")
    private Integer descriptionColumn;

    @Column(name="amount_in_column")
    private Integer amountInColumn;

    @Column(name="amount_out_column")
    private Integer amountOutColumn;

    @Column
    private Boolean reverse;

    @JoinColumn(name="account_id")
    @ManyToOne()
    private Account account;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHeaderLine() {
        return headerLine;
    }

    public void setHeaderLine(String headerLine) {
        this.headerLine = headerLine;
    }

    public Integer getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(Integer firstLine) {
        this.firstLine = firstLine;
    }

    public Integer getDateColumn() {
        return dateColumn;
    }

    public void setDateColumn(Integer dateColumn) {
        this.dateColumn = dateColumn;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Integer getDescriptionColumn() {
        return descriptionColumn;
    }

    public void setDescriptionColumn(Integer descriptionColumn) {
        this.descriptionColumn = descriptionColumn;
    }

    public Integer getAmountInColumn() {
        return amountInColumn;
    }

    public void setAmountInColumn(Integer amountInColumn) {
        this.amountInColumn = amountInColumn;
    }

    public Integer getAmountOutColumn() {
        return amountOutColumn;
    }

    public void setAmountOutColumn(Integer amountOutColumn) {
        this.amountOutColumn = amountOutColumn;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
