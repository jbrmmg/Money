package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.FileResponse;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class ReconciliationFileTest {
    @Autowired
    ReconciliationFileManager reconciliationFileManager;

    @Test
    public void testFilesAvailable() {
        List<FileResponse> files = reconciliationFileManager.getFiles();
        Assert.assertEquals(5, files.size());
    }

    private void testReconcilationFile(FileResponse file, int count, double sumIn, double sumOut, LocalDate earliest, LocalDate latest) throws IOException {
        AccountDTO account = new AccountDTO();
        account.setId("ACNT");
        List<TransactionDTO> transactions = reconciliationFileManager.getFileTransactions(file,account);

        Assert.assertEquals(count, transactions.size());

        double totalIn = 0.0;
        double totalOut = 0.0;
        LocalDate earliestDate = transactions.get(0).getDate();
        LocalDate latestDate = transactions.get(0).getDate();
        for(TransactionDTO next : transactions) {
            if(next.getDate().isBefore(earliestDate)) {
                earliestDate = next.getDate();
            }
            if(next.getDate().isAfter((latestDate))) {
                latestDate = next.getDate();
            }
            if(next.getAmount() > 0) {
                totalIn += next.getAmount();
            } else {
                totalOut += next.getAmount();
            }
        }
        Assert.assertEquals(sumIn, totalIn, 0.001);
        Assert.assertEquals(sumOut, totalOut, 0.001);
        Assert.assertEquals(earliest, earliestDate);
        Assert.assertEquals(latest, latestDate);
    }

    @Test
    public void testAmexFile() throws IOException {
        List<FileResponse> files = reconciliationFileManager.getFiles();
        FileResponse amexFile = null;
        for(FileResponse next : files) {
            if(next.getFile().toLowerCase().contains("amex")) {
                amexFile = next;
            }
        }
        Assert.assertNotNull(amexFile);
        testReconcilationFile(amexFile,15,102.39,-235.03, LocalDate.of(2022,10,5), LocalDate.of(2022,10,11));
    }

    @Test
    public void testFirstDirectFile() throws IOException {
        List<FileResponse> files = reconciliationFileManager.getFiles();
        FileResponse fdFile = null;
        for(FileResponse next : files) {
            if(next.getFile().toLowerCase().contains("first")) {
                fdFile = next;
            }
        }
        Assert.assertNotNull(fdFile);
        testReconcilationFile(fdFile,18,7079, -8083.52, LocalDate.of(2022,9,12), LocalDate.of(2022,10,11));
    }

    @Test
    public void testJlpFile() throws IOException {
        List<FileResponse> files = reconciliationFileManager.getFiles();
        FileResponse jlpFile = null;
        for(FileResponse next : files) {
            if(next.getFile().toLowerCase().contains("jlp.")) {
                jlpFile = next;
            }
        }
        Assert.assertNotNull(jlpFile);
        testReconcilationFile(jlpFile,19,10.02,-7120.36, LocalDate.of(2019,9,20), LocalDate.of(2019,10,14));
    }

    @Test
    public void testJlp2File() throws IOException {
        List<FileResponse> files = reconciliationFileManager.getFiles();
        FileResponse jlpFile = null;
        for(FileResponse next : files) {
            if(next.getFile().toLowerCase().contains("jlp2")) {
                jlpFile = next;
            }
        }
        Assert.assertNotNull(jlpFile);
        testReconcilationFile(jlpFile,14,42.48,-699.28, LocalDate.of(2022,10,7), LocalDate.of(2022,10,10));
    }

    @Test
    public void testNationwideFile() throws IOException {
        List<FileResponse> files = reconciliationFileManager.getFiles();
        FileResponse nationwideFile = null;
        for(FileResponse next : files) {
            if(next.getFile().toLowerCase().contains("nwde")) {
                nationwideFile = next;
            }
        }
        Assert.assertNotNull(nationwideFile);
        testReconcilationFile(nationwideFile,48,102.39,-1235.90, LocalDate.of(2022,7,29), LocalDate.of(2022,8,25));
    }
}
