package com.jbr.middletier.money.data.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportStatusId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "report_year")
    private int year;

    // 0 for annual reports, 1-12 for monthly
    @Column(name = "report_month")
    private int month;

    @Column(name = "report_type")
    private String type;
}
