package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.util.FinancialAmount;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {MoneyReportIT.Initializer.class})
@ActiveProfiles(value="report-it")
public class MoneyReportIT extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(MoneyReportIT.class);

    @Autowired
    private StatementRepository statementRepository;

    @SuppressWarnings("rawtypes")
    @ClassRule
    public static MySQLContainer mysqlContainer = new MySQLContainer("mysql:8.0.28")
            .withDatabaseName("integration-tests-db")
            .withUsername("sa")
            .withPassword("sa");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mysqlContainer.getUsername(),
                    "spring.datasource.password=" + mysqlContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    private String getNumberString(Integer number, int size) {
        return getPaddedString(number == null ? "" : number.toString(),size);
    }

    private String getPaddedString(String value, int size) {
        String format = String.format("%%-%ds",size);
        return String.format(format, value == null ? "" : value);
    }

    private String getFinancialAmountString(FinancialAmount financialAmount, int size) {
        if(financialAmount == null) {
            return " ".repeat(size);
        }

        return financialAmount.toFormattedString(size);
    }

    private String spacing(int size) {
        return " ".repeat(size);
    }

    private String outputFlag(Boolean flag, String value) {
        return Boolean.TRUE.equals(flag) ? value : " ";
    }

    private void logTransactionData(TransactionDataDTO transactionData) {
        LOG.info("-".repeat(160));
        LOG.info("TRANSACTION DETAILS{}|",spacing(140));
        LOG.info("  Open    {} {} {}|", transactionData.getOpenDate(), getFinancialAmountString(transactionData.getOpenBalance(),12),spacing(125));
        LOG.info("  Today   {} {} {}|", transactionData.getToday(), getFinancialAmountString(transactionData.getTodayBalance(),12),spacing(125));
        LOG.info("  Forward {} {} {}|", getPaddedString(transactionData.getForwardDate(),10), getFinancialAmountString(transactionData.getForwardBalance(),12),spacing(125));
        LOG.info("{}|", spacing(159));
        int row = 1;
        for(TransactionReportDTO next : transactionData.getTransactions()) {
            LOG.info("  {} {} {} {} {} {} {} {} {} {} {} {} {} {} {} {}|",
                    "Row",
                    "Id    ",
                    "Date      ",
                    "Balance        ",
                    "Amount       ",
                    "Cat",
                    "Acc ",
                    "Op. Id",
                    "Open Balance   ",
                    "Year",
                    "Mn",
                    "Locked",
                    "Rec",
                    "Predict",
                    "Description                             ",
                    "Act. ");
            LOG.info("  {} {} {} {} {} {} {} {} {} {} {} {} {} {} {} {}{}{}{}{}|",
                    getNumberString(row,3),
                    getNumberString(next.getId(),6),
                    next.getDate(),
                    next.getBalance().toFormattedString(12),
                    next.getAmount().toFormattedString(10),
                    getPaddedString(next.getCategory() == null ? "" : next.getCategory().getId(),3),
                    getPaddedString(next.getAccount() == null ? "" : next.getAccount().getId(),4),
                    getNumberString(next.getOppositeId(),6),
                    getPaddedString(next.getStatement() == null ? "" : next.getStatement().getOpenBalance().toFormattedString(12),15),
                    getPaddedString(next.getStatement() == null ? "" : next.getStatement().getYear().toString(),4),
                    getPaddedString(next.getStatement() == null ? "" : next.getStatement().getMonth().toString(),2),
                    getPaddedString(next.getStatement() == null ? "" : next.getStatement().getLocked() ? "locked" : "",6),
                    getPaddedString(next.getFromReconciliation() ? "Rec" : "",3),
                    getPaddedString(next.getPredicted() ? "Predict" : "",7),
                    getPaddedString(next.getDescription(),40),
                    outputFlag(next.getActionDelete(),"d"),
                    outputFlag(next.getActionReconcile(),"r"),
                    outputFlag(next.getActionUnreconcile(),"u"),
                    outputFlag(next.getActionUpdate(),"U"),
                    outputFlag(next.getActionUpdateCategory(),"c"));
            row++;
        }
        LOG.info("-".repeat(160));
    }

    @Before
    public void cleanUp() {
        // Remove the default statements.
        for(Statement statement : statementRepository.findAll()) {
            if(statement.getId().getYear().equals(2010) && statement.getId().getMonth().equals(1)) {
                statementRepository.delete(statement);
            }
        }
    }

    @Test
    public void testFilterAll() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setReconciliationAccount("BANK");

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(59)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterDescription() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setReconciliationAccount("BANK");
        filter.setDescription("sainsburys");

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("openDate", is("2023-04-27")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterFromRec() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setReconciliationAccount("BANK");
        filter.setFromReconciled(true);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(18)))
//                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterFromRecError() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(true);

        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        LOG.info("Error {}",error);
        Assert.assertTrue(error.contains("Account ID not specified, reconciliation transactions required."));
    }

    @Test
    public void testFilterPredicted() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-05-24")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterStandard() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(38)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterNotLocked() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setLocked(false);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(35)))
                .andExpect(jsonPath("openBalance.value", is(630.16)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterNotLockedBank() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("BANK");
        account.setClosed(false);
        account.setColour("FF0000");
        account.setName("Bank");

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setLocked(false);
        filter.setAccounts(List.of(account));

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(3)))
                .andExpect(jsonPath("openBalance.value", is(620.16)))
                .andExpect(jsonPath("openDate", is("2023-05-01")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterCategory() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");
        category.setName("House");

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setCategories(List.of(category));

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(11)))
                .andExpect(jsonPath("openBalance.value", is(0.0)))
                .andExpect(jsonPath("openDate", is("2023-04-11")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterDate() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setDateRange(new DateRangeDTO("2023-04-20",null));

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(19)))
                .andExpect(jsonPath("openBalance.value", is(0.0)))
                .andExpect(jsonPath("openDate", is("2023-04-20")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterValue() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setValueRange(new ValueRangeDTO(-20,15));

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(23)))
                .andExpect(jsonPath("openBalance.value", is(0.0)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testNull() throws Exception {
        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content("{\"reconciliationAccount\": \"BANK\"}")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(59)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testFromString() throws Exception {
        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content("{\"accounts\":[],\"categories\":[],\"reconciliationAccount\":\"BANK\",\"predicated\":false,\"locked\":false,\"fromReconciled\":false}")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(41)))
                .andExpect(jsonPath("openBalance.value", is(630.16)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testSorting() throws Exception {
        List<TransactionSortDTO> transactionSortList = new ArrayList<>();
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.DATE,TransactionSortType.ASCENDING));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);
        filter.setTransactionSorts(transactionSortList);

        Assert.assertEquals(1,filter.getTransactionSorts().size());

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-05-24")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andExpect(jsonPath("$.transactions[0].description", is("Octopus")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testSortingAmount() throws Exception {
        List<TransactionSortDTO> transactionSortList = new ArrayList<>();
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.AMOUNT,TransactionSortType.DESCENDING));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);
        filter.setTransactionSorts(transactionSortList);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-05-24")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andExpect(jsonPath("$.transactions[0].description", is("Wages")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testSortingAccount() throws Exception {
        List<TransactionSortDTO> transactionSortList = new ArrayList<>();
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.ACCOUNT,TransactionSortType.ASCENDING));
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.AMOUNT,TransactionSortType.DESCENDING));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);
        filter.setTransactionSorts(transactionSortList);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-05-24")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andExpect(jsonPath("$.transactions[0].description", is("Disney Plus")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testSortingCategory() throws Exception {
        List<TransactionSortDTO> transactionSortList = new ArrayList<>();
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.CATEGORY,TransactionSortType.ASCENDING));
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.AMOUNT,TransactionSortType.DESCENDING));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);
        filter.setTransactionSorts(transactionSortList);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-05-24")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andExpect(jsonPath("$.transactions[0].description", is("Council Tax")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }

    @Test
    public void testSortingDescription() throws Exception {
        List<TransactionSortDTO> transactionSortList = new ArrayList<>();
        transactionSortList.add(new TransactionSortDTO(TransactionSortField.DESCRIPTION,TransactionSortType.DESCENDING));

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);
        filter.setTransactionSorts(transactionSortList);

        MvcResult result = getMockMvc().perform(post("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
                .andExpect(jsonPath("openBalance.value", is(1039.0)))
                .andExpect(jsonPath("openDate", is("2023-05-24")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andExpect(jsonPath("$.transactions[3].description", is("Netflix")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }
}
