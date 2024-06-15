package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.primary.ReconciliationFile;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.manager.AccountManager;
import org.modelmapper.AbstractConverter;

public class ReconciliationFileToDTO extends AbstractConverter<ReconciliationFile, ReconciliationFileDTO> {
    private final AccountManager accountManager;

    public ReconciliationFileToDTO(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    private AccountDTO getAccount(String id) {
        try {
            return this.accountManager.getExternal(id);
        } catch (UpdateDeleteAccountException e) {
            return null;
        }
    }

    @Override
    protected ReconciliationFileDTO convert(ReconciliationFile reconciliationFile) {
        ReconciliationFileDTO result = new ReconciliationFileDTO();

        result.setFilename(reconciliationFile.getName());
        if(reconciliationFile.getAccount() != null) {
            result.setAccount(getAccount(reconciliationFile.getAccount().getId()));
        }
        result.setError(reconciliationFile.getError());
        result.setSize(reconciliationFile.getSize());
        result.setLastModified(reconciliationFile.getLastModified());
        result.setLoaded(reconciliationFile.getLoaded());

        return result;
    }
}
