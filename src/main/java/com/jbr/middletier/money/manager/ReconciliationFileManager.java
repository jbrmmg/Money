package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.ReconcileFormat;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.dataaccess.ReconcileFormatRepository;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.reconciliation.FileFormatDescription;
import com.jbr.middletier.money.reconciliation.FileFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Controller
public class ReconciliationFileManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationFileManager.class);

    private final ApplicationProperties applicationProperties;
    private final ReconcileFormatRepository reconcileFormatRepository;

    public ReconciliationFileManager(ApplicationProperties applicationProperties,
                                     ReconcileFormatRepository reconcileFormatRepository) {
        this.applicationProperties = applicationProperties;
        this.reconcileFormatRepository = reconcileFormatRepository;
    }

    public List<ReconciliationFileDTO> getFiles() {
        LOG.info("Get files from {}", applicationProperties.getReconcileFileLocation());

        final File folder = new File(applicationProperties.getReconcileFileLocation());

        List<ReconciliationFileDTO> result = new ArrayList<>();

        for(final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if(!fileEntry.isDirectory() && fileEntry.getPath().endsWith(".csv")) {
                ReconciliationFileDTO newReconciliationFile = new ReconciliationFileDTO();
                newReconciliationFile.setFilename(fileEntry.getPath());

                result.add(newReconciliationFile);
            }
        }

        return result;
    }

    private List<ReconcileFileLine> readContents(File transactionFile) throws IOException {
        List<ReconcileFileLine> result = new ArrayList<>();

        try(Stream<String> stream = Files.lines(transactionFile.toPath())) {
            stream.forEach(s -> {
                // Remove any characters that are not principal
                s = s.replaceAll("\\P{InBasic_Latin}", "");

                while (s.contains("  ") || s.contains("\t")) {
                    s = s.replace("  ", " ");
                    s = s.replace("\t", " ");
                }

                result.add(new ReconcileFileLine(s.trim()));
            });
        }

        return result;
    }

    private boolean validFormattedDate(String dateFormat, String dateValue) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            LocalDate.parse(dateValue,formatter);
        } catch (DateTimeParseException invalid) {
            return false;
        }

        return true;
    }

    private FileFormatDescription determineFileFormat(List<ReconcileFileLine> contents) {
        // The first line before the transactions indicates the structure of the file.
        for(ReconcileFileLine nextLine : contents) {
            FileFormatDescription format = new FileFormatDescription(reconcileFormatRepository, nextLine.getLine());
            if(format.getValid()) {
                return format;
            }
        }

        // Cannot be determinded on the title line, does it match formats that have no title?
        if(contents.size() > 0) {
            for (ReconcileFormat next : reconcileFormatRepository.findByHeaderLineIsNull()) {
                ReconcileFileLine firstLine = contents.get(0);

                if(validFormattedDate(next.getDateFormat(),firstLine.getColumns().get(next.getDateColumn()))) {
                    return new FileFormatDescription(next);
                }
            }
        }

        return new FileFormatDescription();
    }

    private TransactionDTO processLine(FileFormatDescription format, ReconcileFileLine line) {
        try {
            // We need to get 3 things from the line; date, description and amount.
            TransactionDTO result = new TransactionDTO();
            result.setDate(format.getDate(line));
            result.setAmount(format.getAmount(line));
            result.setDescription(format.getDescription(line));

            return result;
        } catch (FileFormatException ex) {
            // The line is ignored if it cannot be processes.
            LOG.warn("Line is ignored {} {}", line.getLine(), ex.getMessage());
            return null;
        }
    }

    private List<TransactionDTO> processFile(FileFormatDescription format, List<ReconcileFileLine> contents) {
        LOG.info("Process file with format {}", format);

        List<TransactionDTO> result = new ArrayList<>();

        if(!format.getValid()) {
            return result;
        }

        // Process the contents starting at the first line.
        int line = 0;
        for(ReconcileFileLine nextLine : contents) {
            if(line < format.getFirstLine()) {
                line++;
                continue;
            }

            // Process this line.
            TransactionDTO transaction = processLine(format, nextLine);
            if(transaction != null) {
                result.add(transaction);
            }
        }

        return result;
    }

    public List<TransactionDTO> getFileTransactions(ReconciliationFileDTO file) throws IOException {
        LOG.info("Get transactions from {}", file.getFilename());

        // Get the full filename
        File transactionFile = new File(file.getFilename());

        if(!Files.exists(transactionFile.toPath())) {
            throw new FileNotFoundException("Cannot find " +  transactionFile.getName());
        }

        // Read the file into a list of strings.
        List<ReconcileFileLine> contents = readContents(transactionFile);

        // Determine the file format
        FileFormatDescription formatDescription = determineFileFormat(contents);

        // Process the file.
        List<TransactionDTO> result = processFile(formatDescription, contents);

        LOG.info("Processed file, found {} transactions", result.size());

        return result;
    }
}
