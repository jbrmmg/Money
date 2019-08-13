package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.Account;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by jason on 07/03/17.
 */
public interface AccountRepository  extends CrudRepository<Account, String> {
}
