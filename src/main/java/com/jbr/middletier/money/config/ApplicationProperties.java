package com.jbr.middletier.money.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="money")
public class ApplicationProperties {
    private String serviceName;
    private String webLogUrl;
    private String reportWorking;
    private String reportShare;

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public void setWebLogUrl(String webLogUrl) { this.webLogUrl = webLogUrl; }

    public void setReportWorking(String reportWorking) { this.reportWorking = reportWorking; }

    public void setReportShare(String reportShare) { this.reportShare = reportShare; }

    public String getServiceName() { return this.serviceName; }

    public String getWebLogUrl() { return this.webLogUrl; }

    public String getReportWorking() { return this.reportWorking; }

    public String getReportShare() { return this.reportShare; }
}
