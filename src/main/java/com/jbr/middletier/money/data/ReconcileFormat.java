package com.jbr.middletier.money.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="reconcile_format")
public class ReconcileFormat {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column
    private String headerLine;

    @Column
    private Integer firstLine;

    @Column
    private Integer dateColumn;

    @Column
    private String dateFormat;

    @Column
    private Integer descriptionColumn;

    @Column
    private Integer amountInColumn;

    @Column
    private Integer amountOutColumn;

    @Column
    private Boolean reverse;

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
}
