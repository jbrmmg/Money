package com.jbr.middletier.money.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="money",ignoreUnknownFields = true)
public class ApplicationProperties {
    private String serviceName;

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceName() { return this.serviceName; }
}
