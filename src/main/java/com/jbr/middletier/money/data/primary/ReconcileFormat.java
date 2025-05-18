package com.jbr.middletier.money.data.primary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}
