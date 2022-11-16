package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.LogoDefinition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogoDefinitionRepository extends CrudRepository<LogoDefinition,String> {
}
