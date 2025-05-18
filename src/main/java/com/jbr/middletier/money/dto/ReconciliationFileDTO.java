package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
public class ReconciliationFileDTO {
    @Pattern(regexp="^[\\da-zA-Z_./\\\\]{1,40}",message="File can only contain letters or digits up to 45 characters.")
    private String filename;
    private AccountDTO account;
    private LocalDateTime lastModified;
    private Long size;
    @Pattern(regexp="^[\\da-zA-Z]{1,100}$",message="Error can only contain letters or digits up to 45 characters.")
    private String error;
    private int transactionCount;
    private BigDecimal creditSum;
    private BigDecimal debitSum;
    private LocalDate earliestTransaction;
    private LocalDate latestTransaction;
    private Boolean loaded;
}
