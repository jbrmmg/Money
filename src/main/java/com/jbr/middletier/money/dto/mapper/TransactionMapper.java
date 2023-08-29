package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.mapper.converter.*;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.manager.StatementManager;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class TransactionMapper extends ModelMapper {
    @Autowired
    public TransactionMapper(AccountManager accountManager, CategoryManager categoryManager, StatementManager statementManager) {
        this.addConverter(new AccountStringConverter());
        this.addConverter(new StringAccountConverter(accountManager));
        this.addConverter(new CategoryStringConverter());
        this.addConverter(new StringCategoryConverter(categoryManager));
        this.addConverter(new LocalDateStringConverter());
        this.addConverter(new StringLocalDateConverter());
        this.addConverter(new TransactionFromDTO(accountManager,categoryManager,statementManager));

        TypeMap<Transaction, TransactionDTO> transactionMapper = this.createTypeMap(Transaction.class, TransactionDTO.class);
        transactionMapper.addMappings(
                mapper -> mapper.map(src -> src.getStatement().getId().getYear(), TransactionDTO::setStatementYear)
        );
        transactionMapper.addMappings(
                mapper -> mapper.map(src -> src.getStatement().getId().getMonth(), TransactionDTO::setStatementMonth)
        );
    }
}
