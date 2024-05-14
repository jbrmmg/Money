package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dto.ReconciliationFileDTO;
import com.jbr.middletier.money.manager.ReconcileFileLine;
import com.jbr.middletier.money.manager.ReconciliationFileManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
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
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        Assert.assertEquals(8, files.size());
    }

    private void testReconciliationFile(ReconciliationFileDTO file, int count, double sumIn, double sumOut, LocalDate earliest, LocalDate latest) {
        Assert.assertEquals(count,file.getTransactionCount());
        Assert.assertEquals(sumIn, file.getCreditSum(), 0.001);
        Assert.assertEquals(sumOut, file.getDebitSum(), 0.001);
        Assert.assertEquals(earliest, file.getEarliestTransaction());
        Assert.assertEquals(latest, file.getLatestTransaction());
    }

    @Test
    public void testAmexFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO amexFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("amex.csv")) {
                amexFile = next;
            }
        }
        Assert.assertNotNull(amexFile);
        testReconciliationFile(amexFile,15,102.39,-235.03, LocalDate.of(2022,10,5), LocalDate.of(2022,10,11));
    }

    @Test
    public void testFirstDirectFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO fdFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("first")) {
                fdFile = next;
            }
        }
        Assert.assertNotNull(fdFile);
        testReconciliationFile(fdFile,18,7079, -8083.52, LocalDate.of(2022,9,12), LocalDate.of(2022,10,11));
    }

    @Test
    public void testJlpFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO jlpFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("jlp.")) {
                jlpFile = next;
            }
        }
        Assert.assertNotNull(jlpFile);
        testReconciliationFile(jlpFile,19,10.02,-7120.36, LocalDate.of(2019,9,20), LocalDate.of(2019,10,14));
    }

    @Test
    public void testJlp2File() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO jlpFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("jlp2")) {
                jlpFile = next;
            }
        }
        Assert.assertNotNull(jlpFile);
        testReconciliationFile(jlpFile,14,42.48,-699.28, LocalDate.of(2022,10,7), LocalDate.of(2022,10,10));
    }

    @Test
    public void testNationwideFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO nationwideFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("nwde")) {
                nationwideFile = next;
            }
        }
        Assert.assertNotNull(nationwideFile);
        testReconciliationFile(nationwideFile,48,102.39,-1235.90, LocalDate.of(2022,7,29), LocalDate.of(2022,8,25));
    }

    @Test
    public void testBarclaysFile() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO barclaycardFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("barc.")) {
                barclaycardFile = next;
            }
        }
        Assert.assertNotNull(barclaycardFile);
        testReconciliationFile(barclaycardFile,57,466.17,-1608.64, LocalDate.of(2023,1,5), LocalDate.of(2023,12,3));
    }

    @Test
    public void testBarclaysFile2() {
        List<ReconciliationFileDTO> files = reconciliationFileManager.getFiles();
        ReconciliationFileDTO barclaycardFile = null;
        for(ReconciliationFileDTO next : files) {
            if(next.getFilename().toLowerCase().contains("barc2")) {
                barclaycardFile = next;
            }
        }
        Assert.assertNotNull(barclaycardFile);
        testReconciliationFile(barclaycardFile,12,2,-297.34, LocalDate.of(2023,9,3), LocalDate.of(2023,9,13));
    }

    private String getElementsForAssert(List<String> columns) {
        return String.join("-", columns);
    }

    @Test
    public void testRegexInLine() {
        ReconcileFileLine line = new ReconcileFileLine(1,"x,y,z");

        Assert.assertEquals("x-y-z",getElementsForAssert(line.getColumns()));
    }

    @Test
    public void testRegexInLine2() {
        ReconcileFileLine line = new ReconcileFileLine(1,"\"x,x\",y,z");

        Assert.assertEquals("\"x,x\"-y-z",getElementsForAssert(line.getColumns()));
    }

    @Test
    public void testRegexInLine3() {
        ReconcileFileLine line = new ReconcileFileLine(1,"x,x,y,z,d");

        Assert.assertEquals("x-x-y-z-d",getElementsForAssert(line.getColumns()));
    }
}
