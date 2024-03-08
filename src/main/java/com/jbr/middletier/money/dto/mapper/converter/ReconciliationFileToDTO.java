package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.ReconciliationFile;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.dto.mapper.AccountMapper;
import org.modelmapper.AbstractConverter;

public class ReconciliationFileToDTO extends AbstractConverter<ReconciliationFile, ReconciliationFileDTO> {
    private final AccountMapper accountMapper;

    public ReconciliationFileToDTO(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    protected ReconciliationFileDTO convert(ReconciliationFile reconciliationFile) {
        ReconciliationFileDTO result = new ReconciliationFileDTO();

        result.setFilename(reconciliationFile.getName());
        if(reconciliationFile.getAccount() != null) {
            result.setAccount(accountMapper.map(reconciliationFile.getAccount(), AccountDTO.class));
        }
        result.setError(reconciliationFile.getError());
        result.setSize(reconciliationFile.getSize());
        result.setLastModified(reconciliationFile.getLastModified());

        return result;
    }
}
