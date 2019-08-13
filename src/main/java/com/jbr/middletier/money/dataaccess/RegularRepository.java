package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.Regular;
import org.springframework.data.repository.CrudRepository;

public interface RegularRepository  extends CrudRepository<Regular, Integer> {
}
