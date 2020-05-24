package com.jbr.middletier.money.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="money")
public class ApplicationProperties {
    private String serviceName;
    private String webLogUrl;
    private String reportWorking;
    private String reportShare;
    private String regularSchedule;
    private boolean regularEnabled;

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public void setWebLogUrl(String webLogUrl) { this.webLogUrl = webLogUrl; }

    public void setReportWorking(String reportWorking) { this.reportWorking = reportWorking; }

    public void setReportShare(String reportShare) { this.reportShare = reportShare; }

    public void setRegularSchedule(String regularSchedule) { this.regularSchedule = regularSchedule; }

    public void setRegularEnabled(Boolean regularEnabled) { this.regularEnabled = regularEnabled; }

    public String getServiceName() { return this.serviceName; }

    public String getWebLogUrl() { return this.webLogUrl; }

    public String getReportWorking() { return this.reportWorking; }

    public String getReportShare() { return this.reportShare; }

    public String getRegularSchedule() { return this.regularSchedule; }

    public boolean getRegularEnabled() { return this.regularEnabled; }

    public String getPDFFilename() { return getReportWorking() + "/Report.pdf"; }

    public String getHtmlFilename() { return getReportWorking() + "/Report.html"; }
}
