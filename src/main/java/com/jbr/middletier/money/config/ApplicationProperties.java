package com.jbr.middletier.money.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
@ConfigurationProperties(prefix="money")
public class ApplicationProperties {
    @Getter
    @Setter
    private String serviceName;
    @Getter
    @Setter
    private String reportWorking;
    @Getter
    @Setter
    private String reportShare;
    @Getter
    @Setter
    private String regularSchedule;
    private boolean regularEnabled;
    @Getter
    @Setter
    private String reportSchedule;
    private boolean reportEnabled;
    @Getter
    @Setter
    private String archiveSchedule;
    private boolean archiveEnabled;
    @Setter
    @Getter
    private String reconcileFileLocation;
    @Setter
    @Getter
    private String version;
    @Setter
    @Getter
    private Integer smtpPort;
    @Setter
    private LocalDate today;

    public void setRegularEnabled(Boolean regularEnabled) { this.regularEnabled = regularEnabled; }

    public void setReportEnabled(Boolean reportEnabled) { this.reportEnabled = reportEnabled; }

    public void setArchiveEnabled(Boolean archiveEnabled) { this.archiveEnabled = archiveEnabled; }

    public boolean getRegularEnabled() { return this.regularEnabled; }

    public boolean getReportEnabled() { return this.reportEnabled; }

    public boolean getArchiveEnabled() { return this.archiveEnabled; }

    public String getPDFFilename() { return getReportWorking() + "/Report.pdf"; }

    public String getHtmlFilename() { return getReportWorking() + "/Report.html"; }

    public LocalDate getToday() {
        if(this.today == null) {
            return LocalDate.now();
        }

        return today;
    }

}
