package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

import java.util.Optional;

public class TransactionDTOToReportDTO extends AbstractConverter<TransactionDTO, TransactionReportDTO> {
    private final AccountManager accountManager;
    private final CategoryManager categoryManager;
    private final StatementManager statementManager;

    public TransactionDTOToReportDTO(AccountManager accountManager,
                                     CategoryManager categoryManager,
                                     StatementManager statementManager) {
        this.accountManager = accountManager;
        this.categoryManager = categoryManager;
        this.statementManager = statementManager;
    }

    @Override
    protected TransactionReportDTO convert(TransactionDTO transaction) {
        if(transaction == null)
            return null;

        TransactionReportDTO result = new TransactionReportDTO();

        result.setTransactionId(transaction.getId());
        result.setDate(transaction.getDate());
        result.setDescription(transaction.getDescription());
        result.setFromReconciliation(false);
        result.setPredicted(false);
        if(transaction.getAccountId() != null) {
            result.setAccount(this.accountManager.getExternalIfValid(transaction.getAccountId()).orElse(null));
        }
        Optional<Account> account = this.accountManager.getIfValid(transaction.getAccountId());
        if(transaction.getHasStatement() && account.isPresent()) {
            result.setStatement(this.statementManager.getStatementExternal(account.get(),transaction.getStatementMonth(),transaction.getStatementYear()).orElse(null));
        } else {
            result.setStatement(null);
            result.setActionReconcile(true);
            result.setActionDelete(true);
        }
        result.setAmount(new FinancialAmount(transaction.getAmount()));
        if(transaction.getCategoryId() != null) {
            result.setCategory(this.categoryManager.getExternalIfValid(transaction.getCategoryId()).orElse(null));
        }
        result.setOppositeId(transaction.getOppositeTransactionId());
        result.setActionUpdate(true);

        return result;
    }
}
