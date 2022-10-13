package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.FileResponse;
import com.jbr.middletier.money.dataaccess.ReconcileFormatRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.reconciliation.FileFormatDescription;
import com.jbr.middletier.money.reconciliation.FileFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Controller
public class ReconciliationFileManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationFileManager.class);

    final private ApplicationProperties applicationProperties;
    final private ReconcileFormatRepository reconcileFormatRepository;

    public ReconciliationFileManager(ApplicationProperties applicationProperties,
                                     ReconcileFormatRepository reconcileFormatRepository) {
        this.applicationProperties = applicationProperties;
        this.reconcileFormatRepository = reconcileFormatRepository;
    }

    public List<FileResponse> getFiles() {
        LOG.info("Get files from {}", applicationProperties.getReconcileFileLocation());

        final File folder = new File(applicationProperties.getReconcileFileLocation());

        List<FileResponse> result = new ArrayList<>();

        for(final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if(!fileEntry.isDirectory() && fileEntry.getPath().endsWith(".csv")) {
                result.add(new FileResponse(fileEntry.getPath()));
            }
        }

        return result;
    }

    private List<String> readContents(File transactionFile) throws IOException {
        List<String> result = new ArrayList<>();

        try(Stream<String> stream = Files.lines(transactionFile.toPath())) {
            stream.forEach(s -> {
                // Remove any characters that are not principal
                s = s.replaceAll("\\P{InBasic_Latin}", "");

                while (s.contains("  ") || s.contains("\t")) {
                    s = s.replace("  ", " ");
                    s = s.replace("\t", " ");
                }

                s = s.trim();

                result.add(s);
            });
        }

        return result;
    }

    private FileFormatDescription determineFileFormat(List<String> contents) {
        // The first line before the transactions indicates the structure of the file.
        for(String nextLine : contents) {
            FileFormatDescription format = new FileFormatDescription(reconcileFormatRepository, nextLine);
            if(format.getValid()) {
                return format;
            }
        }

        return new FileFormatDescription();
    }

    private List<String> getColumms(String line) {
        List<String> result = new ArrayList<>();
        int start = 0;
        boolean inQuotes = false;
        for (int current = 0; current < line.length(); current++) {
            if (line.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
            else if (line.charAt(current) == ',' && !inQuotes) {
                result.add(line.substring(start, current));
                start = current + 1;
            }
        }
        result.add(line.substring(start));

        return result;
    }

    private TransactionDTO processLine(FileFormatDescription format, String line, AccountDTO account) {
        // Split the string into columns (CSV).
        List<String> columns = getColumms(line);

        try {
            // We need to get 3 things from the line; date, description and amount.
            TransactionDTO result = new TransactionDTO();
            result.setAccount(account);
            result.setDate(format.getDate(columns));
            result.setAmount(format.getAmount(columns));
            result.setDescription(format.getDescription(columns));

            return result;
        } catch (FileFormatException ex) {
            // The line is ignored if it cannot be processes.
            LOG.warn("Line is ignored {} {}", line, ex.getMessage());
            return null;
        }
    }

    private List<TransactionDTO> processFile(FileFormatDescription format, List<String> contents, AccountDTO account) {
        LOG.info("Process file with format {}", format.toString());

        List<TransactionDTO> result = new ArrayList<>();

        if(!format.getValid()) {
            return result;
        }

        // Process the contents starting at the first line.
        int line = 0;
        for(String nextLine : contents) {
            if(line < format.getFirstLine()) {
                line++;
                continue;
            }

            // Process this line.
            TransactionDTO transaction = processLine(format, nextLine, account);
            if(transaction != null) {
                result.add(transaction);
            }
        }

        return result;
    }

    public List<TransactionDTO> getFileTransactions(FileResponse file, AccountDTO account) throws IOException {
        LOG.info("Get transactions from {} for {}", file.getFile(), account.getId());

        // Get the full filename
        File transactionFile = new File(file.getFile());

        if(!Files.exists(transactionFile.toPath())) {
            throw new FileNotFoundException("Cannot find " +  transactionFile.getName());
        }

        // Read the file into a list of strings.
        List<String> contents = readContents(transactionFile);

        // Determine the file format
        FileFormatDescription formatDescription = determineFileFormat(contents);

        // Process the file.
        List<TransactionDTO> result = processFile(formatDescription, contents, account);

        LOG.info("Processed file, found {} transactions", result.size());

        return result;
    }
}
