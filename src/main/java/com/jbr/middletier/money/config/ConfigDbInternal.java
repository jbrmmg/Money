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

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.jbr.middletier.money.data.internal.repository",
        entityManagerFactoryRef = "internalEntityManagerFactory",
        transactionManagerRef = "internalTransactionManager"
)
public class ConfigDbInternal extends ConfigDbCommon {
    @Bean
    @ConfigurationProperties("spring.datasource.internal")
    public DataSourceProperties internalDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.internal.hikari")
    public DataSource internalDataSource() {
        return internalDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate internalJdbcTemplate(@Qualifier("internalDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Value("${spring.jpa.properties.internal.hibernate.dialect:#{null}}")
    private String hibernateDialect;
    @Value("${spring.jpa.properties.internal.hibernate.show-sql:true}")
    private String hibernateShowSql;
    @Value("${spring.jpa.properties.internal.hibernate.hbm2ddl.auto:none}")
    private String hibernateHbm2ddlAuto;
    @Value("${spring.jpa.properties.internal.hibernate.format_sql:false}")
    private String hibernateFormatSql;

    @Bean
    public LocalContainerEntityManagerFactoryBean internalEntityManagerFactory(@Qualifier("internalDataSource") DataSource dataSource,
                                                                       EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .persistenceUnit("internal")
                .packages("com.jbr.middletier.money.data.internal")
                .properties(additionalJpaProperties(
                        hibernateHbm2ddlAuto,
                        hibernateShowSql,
                        hibernateFormatSql,
                        hibernateDialect
                ))
                .build();
    }

    @Bean
    public JpaTransactionManager internalTransactionManager(@Qualifier("internalEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

    @Bean
    @ConfigurationProperties(prefix = "internal.liquibase")
    public LiquibaseProperties internalLiquibaseProperties() {
        return new LiquibaseProperties();
    }

    @Primary
    @Bean
    public SpringLiquibase internalLiquibase(@Qualifier("internalDataSource") DataSource dataSource,
                                             @Qualifier("internalLiquibaseProperties") LiquibaseProperties properties) {
        return springLiquibase(dataSource, properties);
    }
}
