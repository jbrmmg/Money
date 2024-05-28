package com.jbr.middletier.money.config;

import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

//check:
//https://stackoverflow.com/questions/72505311/entitymanager-doesnt-translate-camel-case-to-snake-case
//https://stackoverflow.com/questions/45663025/spring-data-jpa-multiple-enablejparepositories

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.jbr.middletier.money.data.primary.repository",
        transactionManagerRef = "primaryTransactionManager"
)
public class ConfigDbPrimary {
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource primaryDataSource() {
        return primaryDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("primaryDataSource") DataSource dataSource,
                                                                       EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .persistenceUnit("primary")
                .packages("com.jbr.middletier.money.data.primary")
                .properties(additionalJpaProperties())
                .build();
    }

    @Value("${spring.jpa.properties.hibernate.dialect:#{null}}")
    private String hibernateDialect;
    @Value("${spring.jpa.properties.hibernate.show-sql:true}")
    private String hibernateShowSql;
    @Value("${spring.jpa.properties.hibernate.hbm2ddl.auto:none}")
    private String hibernateHbm2ddlAuto;
    @Value("${spring.jpa.properties.hibernate.format_sql:false}")
    private String hibernateFormatSql;

    Map<String,String> additionalJpaProperties() {
        Map<String,String> map = new HashMap<>();

        map.put("hibernate.hbm2ddl.auto", hibernateHbm2ddlAuto);
        map.put("hibernate.show_sql", hibernateShowSql);
        map.put("hibernate.format_sql", hibernateFormatSql);
        if(hibernateDialect != null) {
            map.put("hibernate.dialect", hibernateDialect);
        }

        return map;
    }

    @Primary
    @Bean
    public JpaTransactionManager primaryTransactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.liquibase")
    public LiquibaseProperties primaryLiquibaseProperties() {
        return new LiquibaseProperties();
    }

    @Primary
    @Bean
    public SpringLiquibase primaryLiquibase() {
        return springLiquibase(primaryDataSource(), primaryLiquibaseProperties());
    }

    private static SpringLiquibase springLiquibase(DataSource dataSource, LiquibaseProperties properties) {
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
