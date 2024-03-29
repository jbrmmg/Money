package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.dto.MatchDataDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.reconciliation.MatchData;
import org.modelmapper.AbstractConverter;

public class MatchDataToDTO extends AbstractConverter<MatchData, MatchDataDTO> {
    private final TransactionToDTO transactionToDTO;
    private final LocalDateStringConverter localDateStringConverter;

    public MatchDataToDTO(TransactionToDTO transactionToDTO, LocalDateStringConverter localDateStringConverter) {
        this.transactionToDTO = transactionToDTO;
        this.localDateStringConverter = localDateStringConverter;
    }

    @Override
    protected MatchDataDTO convert(MatchData matchData) {
        MatchDataDTO result = new MatchDataDTO();

        result.setId(matchData.getId());
        result.setDescription(matchData.getDescription());
        if(matchData.getAccount() != null) {
            result.setAccountId(matchData.getAccount().getId());
        }
        if(matchData.getCategory() != null) {
            result.setCategoryId(matchData.getCategory().getId());
            result.setColour(matchData.getCategory().getColour());
        }
        result.setAfterAmount(matchData.getAfterAmount());
        result.setBeforeAmount(matchData.getBeforeAmount());
        result.setBackwardAction(matchData.getBackwardAction());
        result.setForwardAction(matchData.getForwardAction());
        result.setAmount(matchData.getAmount());
        result.setDate(localDateStringConverter.convert(matchData.getDate()));

        if(matchData.getTransaction() != null) {
            TransactionDTO transactionDTO = transactionToDTO.convert(matchData.getTransaction());
            result.setTransaction(transactionDTO);
        }

        return result;
    }
}
