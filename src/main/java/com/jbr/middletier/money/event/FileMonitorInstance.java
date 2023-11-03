package com.jbr.middletier.money.event;

import com.jbr.middletier.money.manager.ReconciliationFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class FileMonitorInstance {
    private static final Logger LOG = LoggerFactory.getLogger(FileMonitorInstance.class);
    private boolean filesUpdated;
    private LocalDateTime updateTime;

    public FileMonitorInstance(ReconciliationFileManager reconciliationFileManager) {
        reconciliationFileManager.monitorInstance(this);
        this.filesUpdated = false;
        LOG.info("created " + this);
    }

    public void setFilesUpdated() {
        this.filesUpdated = true;
    }

    public ServerSentEvent<String> test (Long a) {
        this.updateTime = LocalDateTime.now();
        boolean updated = this.filesUpdated;
        this.filesUpdated = false;
        LOG.info("test " + a.toString() + " " + this + " " + updated);
        return ServerSentEvent.<String> builder()
                .data(Boolean.toString(updated))
                .build();
    }

    public long getAge() {
        return ChronoUnit.SECONDS.between(this.updateTime, LocalDateTime.now());
    }
}
