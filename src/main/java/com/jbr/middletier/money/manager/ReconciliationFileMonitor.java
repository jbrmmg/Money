package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.stereotype.Component;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;

import java.io.File;
import java.nio.file.Files;
import java.util.*;


@Component
public class ReconciliationFileMonitor extends FileSystemWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationFileMonitor.class);

    private final ApplicationProperties applicationProperties;

    private final ReconciliationFileManager reconciliationFileManager;

    @Autowired
    public ReconciliationFileMonitor(ApplicationProperties applicationProperties,
                                     ReconciliationFileManager reconciliationFileManager) {
        this.applicationProperties = applicationProperties;
        this.reconciliationFileManager = reconciliationFileManager;
    }

    @PostConstruct
    private void startup() {
        // Just exit if no reconciliation location
        if(this.applicationProperties.getReconcileFileLocation() == null) {
            LOG.info("No reconciliation file location - not monitoring.");
            return;
        }

        if(this.applicationProperties.getReconcileFileLocation().trim().isEmpty()) {
            LOG.info("Blank reconciliation file location - not monitoring.");
            return;
        }

        File reconciliationFileLocation = new File(this.applicationProperties.getReconcileFileLocation());

        if(!Files.exists(reconciliationFileLocation.toPath())) {
            LOG.info("Reconciliation file location does not exist " + this.applicationProperties.getReconcileFileLocation() + " - not monitoring.");
            return;
        }

        // Before monitoring starts, clear current data, process the files currently in the database.
        this.reconciliationFileManager.clearFileData();
        if(reconciliationFileLocation.listFiles() != null) {
            Set<ChangedFile> files = new HashSet<>();
            for (File nextFile : Objects.requireNonNull(reconciliationFileLocation.listFiles())) {
                files.add(new ChangedFile(reconciliationFileLocation, nextFile, ChangedFile.Type.ADD));
            }
            Set<ChangedFiles> changes = new HashSet<>();
            changes.add(new ChangedFiles(reconciliationFileLocation, files));
            this.reconciliationFileManager.onChange(changes);
        }

        this.addSourceDirectory(reconciliationFileLocation);
        this.addListener(this.reconciliationFileManager);

        LOG.info(this.applicationProperties.getReconcileFileLocation() + " - monitoring");
        this.start();
    }
}
