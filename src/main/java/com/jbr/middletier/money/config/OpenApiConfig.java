package com.jbr.middletier.money.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moneyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Money Manager API")
                        .description("REST API for personal financial management, transaction tracking, and reconciliation.")
                        .version("v1")
                        .contact(new Contact()
                                .name("jbrmmg")
                                .email("jbrmmg2011@gmail.com")));
    }
}
