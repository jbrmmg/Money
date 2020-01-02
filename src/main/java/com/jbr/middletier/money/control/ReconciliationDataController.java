package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jason on 10/04/17.
 */

@Controller
@RequestMapping("/jbr")
public class ReconciliationDataController {
    final static private Logger LOG = LoggerFactory.getLogger(ReconciliationDataController.class);

    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ReconciliationDataController(ReconciliationRepository reconciliationRepository,
                                        CategoryRepository categoryRepository) {
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
    }

    // File Processors.
    private interface IReconcileFileProcessor {
        boolean skipLine(String line);

        ReconciliationData getReconcileData(String[] columns) throws Exception;
    }

    private class AmexFileProcessor implements  IReconcileFileProcessor {
        public boolean skipLine(String line) {
            return false;
        }

        public ReconciliationData getReconcileData(String[] columns) throws Exception {
            if(columns.length < 4) {
                throw new Exception("Unexpected line");
            }

            // Column 1 = date.
            Date transactionDate = getRecocillationDateDate(columns[0],"dd/MM/yy");

            // Column 3 = amount * -1
            Double transactionAmount = Double.parseDouble(columns[2]);
            transactionAmount *= -1;

            // Column 4 = description.
            String description = columns[3].length() > 40 ? columns[3].substring(0,40) : columns[3];

            LOG.info("Got a valid record - inserting.");
            return new ReconciliationData(transactionDate, transactionAmount, "", "", description);
        }
    }

    private class JohnLewisFileProcessor implements  IReconcileFileProcessor {
        public boolean skipLine(String line) {
            return false;
        }

        public ReconciliationData getReconcileData(String[] columns) {
            if(columns.length < 3) {
                return null;
            }

            if(columns[2].equalsIgnoreCase("amount")) {
                return null;
            }

            // Column 1 = date.
            Date transactionDate = getRecocillationDateDate(columns[0],"dd-MMM-yyyy");

            if(transactionDate != null) {
                // Column 3 = amount * -1, remove £
                String amountString = columns[2];

                amountString = amountString.replace("£","");
                amountString = amountString.replace(" ","");

                double multiplier = -1;
                if(amountString.substring(0,1).equals("+"))
                {
                    multiplier = 1;
                }

                Double transactionAmount = Double.parseDouble(amountString.replace(",",""));
                transactionAmount *= multiplier;

                // Column 4 = description.
                String description = columns[1].length() > 40 ? columns[1].substring(0, 40) : columns[1];

                LOG.info("Got a valid record - inserting.");
                return new ReconciliationData(transactionDate, transactionAmount, "", "", description);
            }

            return null;
        }
    }

    private class FirstDirectFileProcessor implements  IReconcileFileProcessor {
        public boolean skipLine(String line) {
            return false;
        }

        public ReconciliationData getReconcileData(String[] columns) throws Exception {
            if(columns.length < 4) {
                return null;
            }

            if(columns[2].equalsIgnoreCase("amount")) {
                return null;
            }

            // Column 1 = date.
            Date transactionDate = getRecocillationDateDate(columns[0],"dd/MM/yyyy");

            // Column 3 = amount * -1
            Double transactionAmount = Double.parseDouble(columns[2]);

            // Column 4 = description.
            String description = columns[1].length() > 40 ? columns[1].substring(0,40) : columns[1];

            LOG.info("Got a valid record - inserting.");
            return new ReconciliationData(transactionDate, transactionAmount, "", "", description);
        }
    }

    private Date getRecocillationDateDate(String elementDate, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(elementDate));
            calendar.set(Calendar.HOUR_OF_DAY,12);
            calendar.set(Calendar.MINUTE,0);
            calendar.set(Calendar.SECOND,0);
            calendar.set(Calendar.MILLISECOND,0);

            return calendar.getTime();
        } catch (Exception ignored) {
        }

        return null;
    }

    private Date getReconcilationDataDate(String elementDate) {
        String[] dateFormats = new String[] {"dd-MM-yyyy","dd-MMM-yyyy","yyyy-MM-dd","dd/MM/yyyy", "dd/MMM/yyyy"};

        for(String nextDateFormat : dateFormats) {
            Date nextDate = getRecocillationDateDate(elementDate, nextDateFormat);

            if(nextDate != null) {
                return nextDate;
            }
        }

        return null;
    }

    private Double getReconcilationDataAmount(String elementAmount) {
        try {
            // Attempt to parse the value.
            return Double.parseDouble(elementAmount);
        } catch (Exception ignored) {
        }

        return null;
    }

    private void addReconcilationDataRecord(String record) {
        try {
            // Process the elements (CSV)
            String[] elements = record.split("\t|,");

            // Minimum of 2 (date and amount).
            if(elements.length < 2) {
                throw new Exception("Too few elements.");
            }

            // Process the elements.
            Date transactionDate = null;
            Double transactionAmount = null;
            String categoryId = "";
            String categoryColour = "";
            String description = "";

            for(String nextElement : elements) {
                // If we don't have a date, try to get one.
                if(transactionDate == null) {
                    transactionDate = getReconcilationDataDate(nextElement);

                    if(transactionDate != null) {
                        // Check, if the year is less than 100
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(transactionDate);
                        int year = calendar.get(Calendar.YEAR);
                        if(year < 100) {
                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                            int month = calendar.get(Calendar.MONTH);
                            calendar.set(year + 2000, month, day);
                            transactionDate = calendar.getTime();
                        }

                        continue;
                    }
                }

                if(transactionAmount == null) {
                    transactionAmount = getReconcilationDataAmount(nextElement);

                    if(transactionAmount != null) {
                        continue;
                    }
                }

                // Is it a category id?
                if(categoryId.equals("")) {
                    if (nextElement.length() == 3) {
                        Optional<Category> category = categoryRepository.findById(nextElement);

                        if (category.isPresent()) {
                            categoryId = nextElement;
                            categoryColour = category.get().getColour();
                            continue;
                        }
                    }
                }

                // Otherwise use it as the description.
                if(nextElement.length() > description.length()) {
                    if(nextElement.length() > 40) {
                        description = nextElement.substring(0, 39);
                    } else {
                        description = nextElement;
                    }
                }
            }

            // If we had a value date and amount.
            if((transactionDate != null) && (transactionAmount != null)) {
                LOG.info("Got a valid record - inserting.");
                ReconciliationData newReconciliationData = new ReconciliationData(transactionDate, transactionAmount, categoryId, categoryColour, description);

                reconciliationRepository.save(newReconciliationData);
            }
        } catch (Exception ex) {
            LOG.info("Failed to process record - " + record);
            LOG.info("Error - " + ex.getMessage());
        }
    }

    private void addReconcilationData(String data) {
        // Each line is a record - split by CR/LF
        String[] records = data.split("\n");

        LOG.info("Records - " + records.length);

        // Insert data into the table.
        for(String nextRecord : records) {
            addReconcilationDataRecord(nextRecord);
        }
    }

    private String[] splitDataLine(String line) {
        String[] intermediate = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        String[] result = new String[intermediate.length];

        for(int i = 0; i < intermediate.length; i++) {
            result[i] = intermediate[i].replace("\"","");
        }

        return result;
    }


    private void loadReconcileFile(File recFile, IReconcileFileProcessor lineProcessor) throws Exception {
        // Clear existing data.
        LOG.info("Clear the reconciliation data.");
        reconciliationRepository.deleteAll();

        // Load the AMEX file.
        LOG.info("About to process an AMEX file - " + recFile.getPath());

        // AMEX File is a CSV
        // Date (dd/mm/yy), Reference, Amount *-1, Description, additional
        BufferedReader reader = new BufferedReader(new FileReader(recFile.getPath()));
        String line;
        while((line = reader.readLine()) != null) {
            // Clean the line.
            while(line.contains("  ") || line.contains("\t")) {
                line = line.replace("  ", " ");
                line = line.replace("\t", " ");
            }

            line = line.trim();

            // Get the reconciliation data.
            if(!lineProcessor.skipLine(line)) {
                ReconciliationData recLine = lineProcessor.getReconcileData(splitDataLine(line));

                if(recLine != null) {
                    reconciliationRepository.save(recLine);
                }
            }
        }
        reader.close();
    }

    private void loadFile(LoadFileRequest loadFileRequest) throws Exception {
        // Load the file.
        File recFile = new File(loadFileRequest.getPath());

        if(!recFile.exists()) {
            throw new Exception("Cannot find file " + loadFileRequest.getPath());
        }

        // Process the file of the specified type.
        switch(loadFileRequest.getType()) {
            case "AMEX":
                loadReconcileFile(recFile, new AmexFileProcessor() );
                break;
            case "JOHNLEWIS":
                loadReconcileFile(recFile, new JohnLewisFileProcessor() );
                break;
            case "FIRSTDIRECT":
                loadReconcileFile(recFile, new FirstDirectFileProcessor() );
                break;

            default:
                throw new Exception("Unexpected file type " + loadFileRequest.getType());
        }
    }

    @RequestMapping(path="/ext/money/reconciliation/add", method= RequestMethod.POST)
    public @ResponseBody
    void  reconcileDataExt( @RequestBody String reconciliationData) {
        LOG.info("Adding Reconcilation Data (ext) - " + reconciliationData.length());
        addReconcilationData(reconciliationData);
    }

    @RequestMapping(path="/int/money/reconciliation/add", method= RequestMethod.POST)
    public @ResponseBody
    void  reconcileDataInt( @RequestBody String reconciliationData) {
        LOG.info("Adding Reconcilation Data - " + reconciliationData.length());
        addReconcilationData(reconciliationData);
    }

    @RequestMapping(path="int/money/reconciliation/load", method= RequestMethod.POST)
    public @ResponseBody
    void reconcileDataLoadInt(@RequestBody LoadFileRequest loadFileRequest) throws Exception {
        LOG.info("Request to load file - " + loadFileRequest.getPath() + " " + loadFileRequest.getType());
        loadFile(loadFileRequest);
    }

    @RequestMapping(path="int/money/reconciliation/files", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<FileResponse> getListOfFiles() {
        LOG.info("Request to get list of files");

        final File folder = new File("/home/jason/Downloads");

        List<FileResponse> result = new ArrayList<>();

        for(final File fileEntry : folder.listFiles()) {
            if(!fileEntry.isDirectory()) {
                if(fileEntry.getPath().endsWith(".csv")) {
                    result.add(new FileResponse(fileEntry.getPath()));
                }
            }
        }

        return result;
    }
}
