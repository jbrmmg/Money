package com.jbr.middletier.money.reporting;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.apache.batik.transcoder.TranscoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReportGenerator {
    final static private Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final ResourceLoader resourceLoader;

    @Autowired
    public ReportGenerator(TransactionRepository transactionRepository,
                           StatementRepository statementRepository,
                           ResourceLoader resourceLoader ) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.resourceLoader = resourceLoader;
    }

    class CategoryPercentage implements Comparable<CategoryPercentage> {
        public Category category;
        public double amount;
        public double percentage;

        @Override
        public int compareTo(CategoryPercentage anotherPercentage) {
//            return this.category.getGroup().compareTo(anotherPercentage.category.getGroup());
            return Double.compare(this.percentage,anotherPercentage.percentage);
        }
    }

    private Map<String,CategoryPercentage> getCategoryPercentages(List<Transaction> transactions) {
        Map<String,CategoryPercentage> result = new HashMap<>();

        double totalAmount = 0;
        for(Transaction nextTransaction: transactions) {
            if(nextTransaction.getCategory().getExpense()) {
                CategoryPercentage associatedCategory = result.get(nextTransaction.getCategory().getId());

                if (associatedCategory == null) {
                    associatedCategory = new CategoryPercentage();
                    associatedCategory.category = nextTransaction.getCategory();
                    associatedCategory.amount = 0;
                    associatedCategory.percentage = 0;
                    result.put(nextTransaction.getCategory().getId(), associatedCategory);
                }

                associatedCategory.amount += nextTransaction.getAmount();
                totalAmount += nextTransaction.getAmount();
            }
        }

        for(CategoryPercentage nextCategory: result.values()) {
            nextCategory.percentage = nextCategory.amount / totalAmount * 100;
        }

        return result.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey,
                                Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private String getPieChartSlice(int x, int y, int radius, double percentage, String colour) {
        return String.format("<circle r=\"%d\" cx=\"%d\" cy=\"%d\" fill=\"none\" stroke=\"#%s\" stroke-width=\"%d\" stroke-dasharray=\"%f %f\" transform=\"rotate(-90) translate(%d)\"></circle>\n",
                radius,
                x,
                y,
                colour,
                2 * radius,
                2 * radius * Math.PI * percentage / 100.0,
                2 * radius * Math.PI,
                -4 * radius);
    }

    private String textAtRingAndAngle(int xCentre, int yCentre, int ring, double angle, String text) {
        // Convert the ring and angle to co-ordinates.
        double x = xCentre + Math.sin(Math.toRadians((angle + 180) * -1)) * ring;
        double y = yCentre + Math.cos(Math.toRadians((angle + 180) * -1)) * ring;


        return "<text x=\"" + x + "\" y=\"" + y + "\" font-size=\"120px\">" + text + "</text>\n";
    }

    private void createPieChart(List<Transaction> transactions) throws IOException, TranscoderException {
        String pieChartFile = "/home/jason/Working/pie.svg";
        PrintWriter pie = new PrintWriter(pieChartFile);

        pie.write("<svg viewBox=\"0 0 10000 10000\" xmlns=\"http://www.w3.org/2000/svg\"\n>");
        pie.write("<circle r=\"5000\" cx=\"5000\" cy=\"5000\" fill=\"white\"></circle>\n");
        pie.write("\n");

        // Circumference = 2 * r * pi = 10000 * pi = 31416

        double percent = 100;
        Map<String,CategoryPercentage> categoryPercentageMap = getCategoryPercentages(transactions);
        for(String nextCategoryId: categoryPercentageMap.keySet()) {
            CategoryPercentage nextCategoryPercentage = categoryPercentageMap.get(nextCategoryId);
            pie.write(getPieChartSlice(5000,5000, 2500, percent, nextCategoryPercentage.category.getColour()));

            percent -= nextCategoryPercentage.percentage;
            if(percent < 0) {
                percent = 0;
            }
        }

        percent = 100;
        int ring = 4000;
        for(String nextCategoryId: categoryPercentageMap.keySet()) {
            CategoryPercentage nextCategoryPercentage = categoryPercentageMap.get(nextCategoryId);
            double halfWay = (100 - (percent - nextCategoryPercentage.percentage / 2)) * 3.6;
            pie.write(textAtRingAndAngle(5000, 5000, ring, halfWay * -1.0, nextCategoryPercentage.category.getName() + " (" + Integer.toString((int)halfWay) + ")" ));

            percent -= nextCategoryPercentage.percentage;

            ring -= 200;
            if(ring < 1001) {
                ring = 4000;
            }
        }

        pie.write("<circle r=\"5000\" cx=\"5000\" cy=\"5000\" stroke=\"black\" stroke-width=\"20\" fill=\"none\"></circle>\n");
        pie.write("</svg>");

        pie.close();

        String svg_URI_input = Paths.get("/home/jason/Working/pie.svg").toUri().toURL().toString();
        TranscoderInput input_svg_image = new TranscoderInput(svg_URI_input);
        //Step-2: Define OutputStream to PNG Image and attach to TranscoderOutput
        OutputStream png_ostream = new FileOutputStream("/home/jason/Working/pie.png");
        TranscoderOutput output_png_image = new TranscoderOutput(png_ostream);
        // Step-3: Create PNGTranscoder and define hints if required
        PNGTranscoder my_converter = new PNGTranscoder();
        // Step-4: Convert and Write output
        my_converter.transcode(input_svg_image, output_png_image);
        // Step 5- close / flush Output Stream
        png_ostream.flush();
        png_ostream.close();
    }

    public void generateReport(int year, int month) throws IOException, DocumentException, TranscoderException {
        LOG.info("Generate report");

        // Get all the transactions for the specified statement.
        List<Transaction> transactionList = transactionRepository.findByStatementIdYearAndStatementIdMonth(year,month);

        String htmlFilename = "/home/jason/Working/test.html";
        String pdfFilename = "/home/jason/Working/test.pdf";

        File htmlFile = new File(htmlFilename);
        PrintWriter writer2 = new PrintWriter(htmlFile);

        // Get the email template.
        Resource resource = resourceLoader.getResource("classpath:html/report.html");
        InputStream is = resource.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);

        String template = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        createPieChart(transactionList);
        template = template.replace("<!-- PIE -->", "<img src=\"/home/jason/Working/pie.png\"/>");

        writer2.println(template);
        writer2.close();

        // Generate a pie chart of spending for that year.

        // Generate a pie chart of spending per month.

        // Generate a month on month chart.

        // Generate a list of transactions.

        // Generate a PDF?
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document,
                new FileOutputStream(pdfFilename));
        document.open();
        XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                new FileInputStream(htmlFilename));
        document.close();
    }
}
