package com.jbr.middletier.money.util;

import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.TransactionSortDTO;
import com.jbr.middletier.money.dto.TransactionSortField;
import com.jbr.middletier.money.dto.TransactionSortType;

import java.util.ArrayList;
import java.util.List;

public class TransactionSorting {
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
            t1Category = t1.getCategory().getName();
        }

        if(t2.getCategory() != null) {
            t2Category = t2.getCategory().getName();
        }

        return t1Category.compareTo(t2Category);
    }

    private static int compareAccount(TransactionReportDTO t1, TransactionReportDTO t2) {
        // Compare on account name.
        return t1.getAccount().getName().compareTo(t2.getAccount().getName());
    }

    public static int compare(TransactionReportDTO t1, TransactionReportDTO t2, List<TransactionSortDTO> sorting) {
        for(TransactionSortDTO sort : getFullSort(sorting)) {
            // Get the next sort field result.
            int nextResult = switch (sort.getField()) {
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
