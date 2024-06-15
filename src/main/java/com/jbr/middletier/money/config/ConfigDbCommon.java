package com.jbr.middletier.money.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class ConfigDbCommon {
    protected ConfigDbCommon() {
    }

    protected Map<String,String> additionalJpaProperties(String hibernateHbm2ddlAuto,
                                               String hibernateShowSql,
                                               String hibernateFormatSql,
                                               String hibernateDialect) {
        Map<String,String> map = new HashMap<>();

        map.put("hibernate.hbm2ddl.auto", hibernateHbm2ddlAuto);
        map.put("hibernate.show_sql", hibernateShowSql);
        map.put("hibernate.format_sql", hibernateFormatSql);
        if(hibernateDialect != null) {
            map.put("hibernate.dialect", hibernateDialect);
        }

        return map;
    }

    protected SpringLiquibase springLiquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setContexts(properties.getContexts());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setRollbackFile(properties.getRollbackFile());
        return liquibase;
    }
}
