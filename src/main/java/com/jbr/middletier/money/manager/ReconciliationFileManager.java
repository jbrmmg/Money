package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.ReconcileFormat;
import com.jbr.middletier.money.data.ReconciliationFile;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.ReconciliationFileRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.dataaccess.ReconcileFormatRepository;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.reconciliation.FileFormatDescription;
import com.jbr.middletier.money.reconciliation.FileFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@Controller
public class ReconciliationFileManager implements FileChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationFileManager.class);

    private final ApplicationProperties applicationProperties;
    private final ReconcileFormatRepository reconcileFormatRepository;
    private final TransactionMapper transactionMapper;
    private final ReconciliationFileRepository reconciliationFileRepository;

    public ReconciliationFileManager(ApplicationProperties applicationProperties,
                                     ReconcileFormatRepository reconcileFormatRepository,
                                     TransactionMapper transactionMapper,
                                     ReconciliationFileRepository reconciliationFileRepository) {
        this.applicationProperties = applicationProperties;
        this.reconcileFormatRepository = reconcileFormatRepository;
        this.transactionMapper = transactionMapper;
        this.reconciliationFileRepository = reconciliationFileRepository;
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
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(dateFormat)
                    .toFormatter(Locale.ENGLISH);
            LocalDate ignored = LocalDate.parse(dateValue,formatter);
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

        // Cannot be determined on the title line, does it match formats that have no title?
        if(!contents.isEmpty()) {
            for (ReconcileFormat next : reconcileFormatRepository.findByHeaderLineIsNull()) {
                ReconcileFileLine firstLine = contents.get(0);

                if(validFormattedDate(next.getDateFormat(),firstLine.getColumns().get(next.getDateColumn()))) {
                    return new FileFormatDescription(next);
                }
            }
        }

        return new FileFormatDescription();
    }

    private Transaction processLine(FileFormatDescription format, ReconcileFileLine line) {
        try {
            LOG.info("Process Line");
            for(String next : line.getColumns()) {
                LOG.info("Next {}",next);
            }
            LOG.info("Format {}", format.getValid());
            // We need to get 3 things from the line; date, description and amount.
            Transaction result = new Transaction();
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
            Transaction lineTransaction = processLine(format, nextLine);
            if(lineTransaction != null) {
                result.add(this.transactionMapper.map(lineTransaction, TransactionDTO.class));
            }
        }

        return result;
    }

    public static class TransactionFileDetails {
        List<TransactionDTO> transactions;
        boolean OK;
        String error;
        String accountId;

        public TransactionFileDetails() {
            this.transactions = new ArrayList<>();
            this.OK = false;
            this.error = "Unitialised";
            this.accountId = null;
        }

        public List<TransactionDTO> getTransactions() {
            return this.transactions;
        }

        public void addTransaction(TransactionDTO transaction) {
            this.transactions.add(transaction);
        }

        public void setOK(boolean OK) {
            this.OK = OK;
        }

        public boolean isOK() {
            return this.OK;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getError() {
            return this.error;
        }

        public void setAccountId(String account) {
            this.accountId = account;
        }

        public String getAccountId() {
            return this.accountId;
        }
    }

    public TransactionFileDetails getFileTransactionDetails(ReconciliationFileDTO file) throws IOException {
        LOG.info("Get transactions from {}", file.getFilename());

        TransactionFileDetails result = new TransactionFileDetails();

        // Get the full filename
        File transactionFile = new File(file.getFilename());

        if(!Files.exists(transactionFile.toPath())) {
            throw new FileNotFoundException("Cannot find " +  transactionFile.getName());
        }

        // Read the file into a list of strings.
        List<ReconcileFileLine> contents = readContents(transactionFile);

        // Determine the file format
        FileFormatDescription formatDescription = determineFileFormat(contents);
        if(!formatDescription.getValid()) {
            result.setError("Cannot determine format");
            return result;
        }

        // Set the account (if available)
        if(formatDescription.getAccountId() != null) {
            Account account = this.transactionMapper.map(formatDescription.getAccountId(),Account.class);
            result.setAccountId(account.getId());
        }

        // Process the file.
        List<TransactionDTO> transactions = processFile(formatDescription, contents);
        LOG.info("Processed file, found {} transactions", transactions.size());

        if(!transactions.isEmpty()) {
            for (TransactionDTO next : transactions) {
                result.addTransaction(next);
            }
            result.setOK(true);
        } else {
            result.setError("No transactions found");
        }

        return result;
    }

    @Deprecated
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

    public void fileUpdated(File update) {
        Optional<ReconciliationFile> dbFile = Optional.empty();

        try {
            LOG.info("Update file: " + update.toString());

            ReconciliationFileDTO fileInformation = new ReconciliationFileDTO();
            fileInformation.setFilename(new File(this.applicationProperties.getReconcileFileLocation(), update.getName()).toString());

            TransactionFileDetails details = this.getFileTransactionDetails(fileInformation);

            // Does this already exist?
            dbFile = this.reconciliationFileRepository.findById(update.getName());
            if(dbFile.isEmpty()) {
                dbFile = Optional.of(new ReconciliationFile());
                dbFile.get().setName(update.getName());
            }

            dbFile.get().setAccount(null);
            if(details.getAccountId() != null) {
                dbFile.get().setAccount(transactionMapper.map(details.getAccountId(), Account.class));
            }

            BasicFileAttributes attr = Files.readAttributes(update.toPath(), BasicFileAttributes.class);
            dbFile.get().setLastModified(LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault()));
            dbFile.get().setSize(attr.size());

            // Is this a valid file?
            if(details.isOK()) {
                // TODO load data into the file.
                dbFile.get().setError(null);
            } else {
                // TODO flag as a problem.
                dbFile.get().setError(details.error);
            }
        } catch (IOException error) {
            dbFile.ifPresent(reconciliationFile -> reconciliationFile.setError("Exception processing file."));
            LOG.warn("Failed to get details of " + update);
        }

        // Save the data.
        dbFile.ifPresent(this.reconciliationFileRepository::save);
    }

    public void clearFileData() {
        // TODO clear all the file data stored.

        this.reconciliationFileRepository.deleteAll();
    }

    public void fileDeleted(File deleted) {
        //TODO - remove data from database.
        Optional<ReconciliationFile> dbFile = this.reconciliationFileRepository.findById(deleted.getName());
        if(dbFile.isPresent()) {
            this.reconciliationFileRepository.delete(dbFile.get());
        }

        LOG.info("Deleted file: " + deleted.toString());
    }

    @Override
    public void onChange(Set<ChangedFiles> changeSet) {
        for(ChangedFiles next : changeSet) {
            for(ChangedFile nextFile : next.getFiles()) {
                // Is this a csv file?
                if(!nextFile.getFile().getName().toLowerCase().endsWith(".csv")) {
                    LOG.info(nextFile + " ignoring file as not a csv");
                    continue;
                }

                // What is the change?
                switch (nextFile.getType()) {
                    case ADD:
                    case MODIFY:
                        fileUpdated(nextFile.getFile());
                        break;
                    case DELETE:
                        fileDeleted(nextFile.getFile());
                        break;
                }
            }
        }
    }
}
