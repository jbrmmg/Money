package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

import java.math.BigDecimal;
import java.util.Optional;

public class TransactionReportToDTO extends AbstractConverter<TransactionReport, TransactionReportDTO> {
    private final AccountManager accountManager;
    private final CategoryManager categoryManager;
    private final StatementManager statementManager;

    public TransactionReportToDTO(AccountManager accountManager, CategoryManager categoryManager, StatementManager statementManager) {
        this.accountManager = accountManager;
        this.categoryManager = categoryManager;
        this.statementManager = statementManager;
    }

    private FinancialAmount getAmount(BigDecimal value){
        if(value == null){
            return new FinancialAmount();
        }

        return new FinancialAmount(value);
    }

    private AccountDTO getAccount(String id) {
        try {
            return this.accountManager.getExternal(id);
        } catch (UpdateDeleteAccountException e) {
            return null;
        }
    }

    private CategoryDTO getCategory(String id) {
        try {
            return this.categoryManager.getExternal(id);
        } catch (UpdateDeleteCategoryException e) {
            return null;
        }
    }

    private StatementDTO getStatement(String accountId, int statementMonth, int statementYear) {
        try {
            Optional<StatementDTO> statement = this.statementManager.getStatementExternal(this.accountManager.get(accountId),statementMonth,statementYear);

            if(statement.isPresent()) {
                return statement.get();
            }
        } catch (UpdateDeleteAccountException ignored) {
            return null;
        }

        return null;
    }

    @Override
    protected TransactionReportDTO convert(TransactionReport transactionReport) {
        TransactionReportDTO result = new TransactionReportDTO();

        result.setType(TransactionReportTypeDTO.TRANSACTION);
        result.setId(transactionReport.getId());
        result.setTransactionId(transactionReport.getTransactionId());
        result.setDate(transactionReport.getDate());
        result.setAmount(getAmount(transactionReport.getAmount()));
        result.setAccount(getAccount(transactionReport.getAccountId()));
        result.setActionUpdate(transactionReport.getActionUpdate());
        result.setActionReconcile(transactionReport.getActionReconcile());
        result.setActionUnreconcile(transactionReport.getActionUnreconcile());
        result.setActionDelete(transactionReport.getActionDelete());
        result.setDescription(transactionReport.getDescription());
        result.setPredicted(transactionReport.getPredicted());
        result.setCategory(getCategory(transactionReport.getCategoryId()));
        result.setOppositeId(transactionReport.getOppositeId());
        if(transactionReport.getAccountId() != null && transactionReport.getStatementYear() != null && transactionReport.getStatementMonth() != null) {
            result.setStatement(getStatement(transactionReport.getAccountId(), transactionReport.getStatementMonth(), transactionReport.getStatementYear()));
        }
        result.setFromReconciliation(transactionReport.getFromReconciliation());

        return result;
    }
}
