package com.jbr.middletier.money.util;

import com.jbr.middletier.money.dto.*;

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

    private static String getStatementSort(StatementDTO statement, AccountDTO account) {
        if(statement == null) {
            return "999999" + account.getId();
        } else {
            int sortValue = statement.getYear() * 100 + statement.getMonth();
            return sortValue + statement.getAccountId();
        }
    }

    private static int compareStatement(TransactionReportDTO t1, TransactionReportDTO t2){
        // Compare on statement (null treated higher)
        String t1StatementSort = getStatementSort(t1.getStatement(),t1.getAccount());
        String t2StatementSort = getStatementSort(t2.getStatement(),t2.getAccount());

        return t1StatementSort.compareTo(t2StatementSort);
    }

    public static int compare(TransactionReportDTO t1, TransactionReportDTO t2, List<TransactionSortDTO> sorting) {
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
