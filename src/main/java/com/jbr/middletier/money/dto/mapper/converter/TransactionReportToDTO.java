package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;

public class TransactionReportToDTO extends AbstractConverter<TransactionReport, TransactionReportDTO> {
    private FinancialAmount getAmount(Double value){
        if(value == null){
            return new FinancialAmount();
        }

        return new FinancialAmount(value);
    }

    @Override
    protected TransactionReportDTO convert(TransactionReport transactionReport) {
        TransactionReportDTO result = new TransactionReportDTO();

        result.setId(transactionReport.getId());
        result.setDate(transactionReport.getDate());
        result.setAmount(getAmount(transactionReport.getAmount()));
        // TODO
//        result.setAccount();
        result.setActionUpdateCategory(transactionReport.getActionUpdateCategory());
        result.setActionUpdate(transactionReport.getActionUpdate());
        result.setActionReconcile(transactionReport.getActionReconcile());
        result.setActionUnreconcile(transactionReport.getActionUnreconcile());
        result.setActionDelete(transactionReport.getActionDelete());
        result.setDescription(transactionReport.getDescription());
        result.setPredicted(transactionReport.getPredicted());
        // TODO
//        result.setCategory();
        result.setOppositeId(transactionReport.getOppositeId());
        // TODO
//        result.setStatement();
        result.setFromReconciliation(transactionReport.getFromReconciliation());

        return result;
    }
}
