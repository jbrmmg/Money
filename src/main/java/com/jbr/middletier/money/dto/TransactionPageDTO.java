package com.jbr.middletier.money.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TransactionPageDTO {
    private final int totalCount;
    private final int pageNumber;
    private final int maxPageSize;
    private final List<TransactionReportDTO> transactions;
}
