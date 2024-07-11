package com.jbr.middletier.money.util;

import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.dto.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionSorting {
    private TransactionSorting() {
    }

    private static boolean listContainsField(List<TransactionSortDTO> list, TransactionSortField field) {
        for(TransactionSortDTO next : list) {
            if(next.getField().equals(field)) {
                return true;
            }
        }

        return false;
    }

    private static List<TransactionSortDTO> getFullSort(List<TransactionSortDTO> sorting) {
        // Add in any missing fields.
        List<TransactionSortDTO> result = new ArrayList<>(sorting);

        for(TransactionSortField next : TransactionSortField.values()) {
            // Is this field in the list?
            if(!listContainsField(result, next)) {
                result.add(new TransactionSortDTO(next,TransactionSortType.ASCENDING));
            }
        }

        return result;
    }

    private static int compareDate(TransactionReportDTO t1, TransactionReportDTO t2) {
        // Compare on the date
        return t1.getDate().compareTo(t2.getDate());
    }

    private static int compareAmount(TransactionReportDTO t1, TransactionReportDTO t2) {
        // Compare the amount.
        return t1.getAmount().compareTo(t2.getAmount());
    }

    private static int compareDescription(TransactionReportDTO t1, TransactionReportDTO t2) {
        // Compare on description.
        return t1.getDescription().compareTo(t2.getDescription());
    }

    private static int compareCategory(TransactionReportDTO t1, TransactionReportDTO t2) {
        // Compare on category (null treated as blank).
        String t1Category = "";
        String t2Category = "";

        if(t1.getCategory() != null) {
            t1Category = t1.getCategory().getId();
        }

        if(t2.getCategory() != null) {
            t2Category = t2.getCategory().getId();
        }

        return t1Category.compareTo(t2Category);
    }

    private static int compareAccount(TransactionReportDTO t1, TransactionReportDTO t2) {
        // Compare on account name.
        return t1.getAccount().getId().compareTo(t2.getAccount().getId());
    }

    private static int getStatementSort(StatementDTO statement) {
        if(statement == null) {
            return 999999;
        } else {
            return statement.getYear() * 100 + statement.getMonth();
        }
    }

    private static int compareStatement(TransactionReportDTO t1, TransactionReportDTO t2){
        // Compare on statement (null treated higher)
        Integer t1StatementSort = getStatementSort(t1.getStatement());
        Integer t2StatementSort = getStatementSort(t2.getStatement());

        return t1StatementSort.compareTo(t2StatementSort);
    }

    private static int getTypeSort(TransactionReportDTO t, LocalDate today) {
        // Generate a value based on the details:
        //   Open Balance = 0
        //   Transaction on or before today = 1
        //   Today balance = 2
        //   Transaction after today = 3;
        //   Future Balance = 4.
        if(t.getType() == TransactionReportTypeDTO.OPEN_BALANCE) {
            return 0;
        }

        if(t.getType() == TransactionReportTypeDTO.FUTURE_BALANCE) {
            return 4;
        }

        if(t.getType() == TransactionReportTypeDTO.TODAY_BALANCE) {
            return 2;
        }

        // If we get here then the value is based on the date compared to today.
        LocalDate transactionDate = LocalDate.parse(t.getDate(), Constants.MONEY_DATE_FORMATTER);

        if(transactionDate.isAfter(today)) {
            return 3;
        }

        return 1;
    }

    public static int compareOnType(TransactionReportDTO t1, TransactionReportDTO t2, LocalDate today) {
        // Get the type sort value for each.
        Integer t1TypeSort = getTypeSort(t1,today);
        Integer t2TypeSort = getTypeSort(t2,today);

        return t1TypeSort.compareTo(t2TypeSort);
    }

    public static int compare(TransactionReportDTO t1, TransactionReportDTO t2, List<TransactionSortDTO> sorting, LocalDate today) {
        // There are 4 types of transaction report; Transaction, Opening Balance, Today's Balance and Future Balance.
        // Opening Balance should always be sorted to be first.
        // Today's Balance should be after any transaction on or before today.
        // Future Balance should always be last.

        // If the types are different, then compare on the type.
        if(t1.getType() != t2.getType()) {
            return compareOnType(t1, t2, today);
        }

        // Compare standard criteria.
        for(TransactionSortDTO sort : getFullSort(sorting)) {
            // Get the next sort field result.
            int nextResult = switch (sort.getField()) {
                case STATEMENT -> compareStatement(t1,t2);
                case DATE -> compareDate(t1, t2);
                case AMOUNT -> compareAmount(t1, t2);
                case DESCRIPTION -> compareDescription(t1, t2);
                case CATEGORY -> compareCategory(t1, t2);
                case ACCOUNT -> compareAccount(t1, t2);
            };

            // If its different then that is the overall result.
            if(nextResult != 0) {
                if(sort.getType() == TransactionSortType.ASCENDING) {
                    return nextResult;
                }
                return nextResult * -1;
            }
        }

        return 0;
    }
}
