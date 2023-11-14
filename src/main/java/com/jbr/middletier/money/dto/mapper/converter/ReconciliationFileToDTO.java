package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.ReconciliationFile;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import org.modelmapper.AbstractConverter;

public class ReconciliationFileToDTO extends AbstractConverter<ReconciliationFile, ReconciliationFileDTO> {
    @Override
    protected ReconciliationFileDTO convert(ReconciliationFile reconciliationFile) {
        ReconciliationFileDTO result = new ReconciliationFileDTO();

        result.setFilename(reconciliationFile.getName());

        return result;
    }
}
