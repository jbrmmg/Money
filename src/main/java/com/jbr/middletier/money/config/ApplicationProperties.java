package com.jbr.middletier.money.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="money")
public class ApplicationProperties {
    private String serviceName;
    private String webLogUrl;

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public void setWebLogUrl(String webLogUrl) { this.webLogUrl = webLogUrl; }

    public String getServiceName() { return this.serviceName; }

    public String getWebLogUrl() { return this.webLogUrl; }
}
