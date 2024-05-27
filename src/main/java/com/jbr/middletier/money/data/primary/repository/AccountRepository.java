package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.Account;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by jason on 07/03/17.
 */
public interface AccountRepository  extends CrudRepository<Account, String> {
}
