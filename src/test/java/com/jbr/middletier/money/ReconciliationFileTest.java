package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.manager.ReconcileFileLine;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class ReconciliationFileTest {
    @Autowired
    ReconciliationFileManager reconciliationFileManager;

    @Test
    void testFilesAvailable() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        Assertions.assertEquals(8, files.size());
    }

    private void testReconciliationFile(ReconciliationFileDTO file, int count, double sumIn, double sumOut, LocalDate earliest, LocalDate latest) {
        Assertions.assertEquals(count,file.getTransactionCount());
        Assertions.assertEquals(sumIn, file.getCreditSum().doubleValue(), 0.001);
        Assertions.assertEquals(sumOut, file.getDebitSum().doubleValue(), 0.001);
        Assertions.assertEquals(earliest, file.getEarliestTransaction());
        Assertions.assertEquals(latest, file.getLatestTransaction());
    }

    @Test
    void testAmexFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO amexFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("amex.csv")) {
                amexFile = next;
            }
        }
        Assertions.assertNotNull(amexFile);
        testReconciliationFile(amexFile,15,102.39,-235.03, LocalDate.of(2022, Month.OCTOBER,5), LocalDate.of(2022, Month.OCTOBER,11));
    }

    @Test
    void testFirstDirectFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO fdFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("first")) {
                fdFile = next;
            }
        }
        Assertions.assertNotNull(fdFile);
        testReconciliationFile(fdFile,18,7079, -8083.52, LocalDate.of(2022, Month.SEPTEMBER,12), LocalDate.of(2022, Month.OCTOBER,11));
    }

    @Test
    void testJlpFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO jlpFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("jlp.")) {
                jlpFile = next;
            }
        }
        Assertions.assertNotNull(jlpFile);
        testReconciliationFile(jlpFile,19,10.02,-7120.36, LocalDate.of(2019, Month.SEPTEMBER,20), LocalDate.of(2019, Month.OCTOBER,14));
    }

    @Test
    void testJlp2File() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO jlpFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("jlp2")) {
                jlpFile = next;
            }
        }
        Assertions.assertNotNull(jlpFile);
        testReconciliationFile(jlpFile,14,42.48,-699.28, LocalDate.of(2022, Month.OCTOBER,7), LocalDate.of(2022, Month.OCTOBER,10));
    }

    @Test
    void testNationwideFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO nationwideFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("nwde")) {
                nationwideFile = next;
            }
        }
        Assertions.assertNotNull(nationwideFile);
        testReconciliationFile(nationwideFile,48,102.39,-1235.90, LocalDate.of(2022, Month.JULY,29), LocalDate.of(2022, Month.AUGUST,25));
    }

    @Test
    void testBarclaysFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO barclaycardFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("barc.")) {
                barclaycardFile = next;
            }
        }
        Assertions.assertNotNull(barclaycardFile);
        testReconciliationFile(barclaycardFile,57,466.17,-1608.64, LocalDate.of(2023, Month.JANUARY,5), LocalDate.of(2023, Month.DECEMBER,3));
    }

    @Test
    void testBarclaysFile2() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO barclaycardFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("barc2")) {
                barclaycardFile = next;
            }
        }
        Assertions.assertNotNull(barclaycardFile);
        testReconciliationFile(barclaycardFile,12,2,-297.34, LocalDate.of(2023, Month.SEPTEMBER,3), LocalDate.of(2023, Month.SEPTEMBER,13));
    }

    private String getElementsForAssert(List<String> columns) {
        return String.join("-", columns);
    }

    @Test
    void testRegexInLine() {
        ReconcileFileLine line = new ReconcileFileLine(1,"x,y,z");

        Assertions.assertEquals("x-y-z",getElementsForAssert(line.getColumns()));
    }

    @Test
    void testRegexInLine2() {
        ReconcileFileLine line = new ReconcileFileLine(1,"\"x,x\",y,z");

        Assertions.assertEquals("\"x,x\"-y-z",getElementsForAssert(line.getColumns()));
    }

    @Test
    void testRegexInLine3() {
        ReconcileFileLine line = new ReconcileFileLine(1,"x,x,y,z,d");

        Assertions.assertEquals("x-x-y-z-d",getElementsForAssert(line.getColumns()));
    }
}
