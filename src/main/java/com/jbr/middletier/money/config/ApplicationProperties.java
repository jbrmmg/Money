package com.jbr.middletier.money.config;

import com.jbr.middletier.money.dto.mapper.DtoComplexModelMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.modelmapper.*;

@Configuration
@ConfigurationProperties(prefix="money")
public class ApplicationProperties {
    private String serviceName;
    private String reportWorking;
    private String reportShare;
    private String regularSchedule;
    private boolean regularEnabled;
    private String reportSchedule;
    private boolean reportEnabled;
    private String archiveSchedule;
    private boolean archiveEnabled;
    private String reconcileFileLocation;
    private Integer smtpPort;

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public void setReportWorking(String reportWorking) { this.reportWorking = reportWorking; }

    public void setReportShare(String reportShare) { this.reportShare = reportShare; }

    public void setRegularSchedule(String regularSchedule) { this.regularSchedule = regularSchedule; }

    public void setRegularEnabled(Boolean regularEnabled) { this.regularEnabled = regularEnabled; }

    public void setReportSchedule(String reportSchedule) { this.reportSchedule = reportSchedule; }

    public void setReportEnabled(Boolean reportEnabled) { this.reportEnabled = reportEnabled; }

    public void setArchiveSchedule(String archiveSchedule) { this.archiveSchedule = archiveSchedule; }

    public void setArchiveEnabled(Boolean archiveEnabled) { this.archiveEnabled = archiveEnabled; }

    public String getServiceName() { return this.serviceName; }

    public String getReportWorking() { return this.reportWorking; }

    public String getReportShare() { return this.reportShare; }

    public String getRegularSchedule() { return this.regularSchedule; }

    public boolean getRegularEnabled() { return this.regularEnabled; }

    public String getReportSchedule() { return this.reportSchedule; }

    public boolean getReportEnabled() { return this.reportEnabled; }

    public String getArchiveSchedule() { return this.archiveSchedule; }

    public boolean getArchiveEnabled() { return this.archiveEnabled; }

    public String getPDFFilename() { return getReportWorking() + "/Report.pdf"; }

    public String getHtmlFilename() { return getReportWorking() + "/Report.html"; }

    public String getReconcileFileLocation() {
        return reconcileFileLocation;
    }

    public void setReconcileFileLocation(String reconcileFileLocation) {
        this.reconcileFileLocation = reconcileFileLocation;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }
}
