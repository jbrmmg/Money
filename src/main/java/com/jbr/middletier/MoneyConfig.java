package com.jbr.middletier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Created by jason on 08/02/17.
 */
@Configuration
@ComponentScan("com.jbr.middletier")
public class MoneyConfig {
    @Value("${middle.tier.money.db.url}")
    private String url;

    @Value("${middle.tier.money.db.username:}")
    private String username;

    @Value("${middle.tier.money.db.password:}")
    private String password;

    @Value("${middle.tier.money.db.driver:com.mysql.cj.jdbc.Driver}")
    private String driver;

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(driver);
        dataSourceBuilder.url(url);

        if(username.length() > 0) {
            dataSourceBuilder.username(username);
        }

        if(password.length() > 0) {
            dataSourceBuilder.password(password);
        }

        return dataSourceBuilder.build();
    }
}
