package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.LogoDefinition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogoDefinitionRepository extends CrudRepository<LogoDefinition,String> {
}
