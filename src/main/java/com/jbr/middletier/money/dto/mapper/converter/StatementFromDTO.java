package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.manager.AccountManager;
import org.modelmapper.AbstractConverter;

public class StatementFromDTO extends AbstractConverter<StatementDTO,Statement> {
    private final AccountManager accountManager;

    public StatementFromDTO(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Override
    protected Statement convert(StatementDTO source) {
        Statement result = new Statement();

        StatementId statementId = new StatementId();
        result.setId(statementId);

        statementId.setAccount(accountManager.getIfValid(source.getAccountId()).orElse(null));
        statementId.setMonth(source.getMonth());
        statementId.setYear(source.getYear());

        result.setLocked(source.getLocked());
        result.setOpenBalance(source.getOpenBalance());

        return result;
    }
}
