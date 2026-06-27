package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.ReconciliationManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.util.FinancialAmount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@WebAppConfiguration
@ContextConfiguration(initializers = {MoneyReportIT.Initializer.class})
@ActiveProfiles(value="report-it")
@Testcontainers
public class MoneyReportIT extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(MoneyReportIT.class);

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private AccountTransactionManager accountTransactionManager;

    @Autowired
    private ReconciliationManager reconciliationManager;

    @Autowired
    private StatementManager statementManager;

    @SuppressWarnings("rawtypes")
    @Container
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

    private String getFinancialAmountString(FinancialAmount financialAmount) {
        if(financialAmount == null) {
            return " ".repeat(12);
        }

        return financialAmount.toFormattedString(12);
    }

    private void logTransactionData(List<TransactionReportDTO> transactionData) {
        // Get the 3 balances if they are present.
        StringBuilder builder = new StringBuilder();

        builder.append("\n");
        builder.append("Transaction Details\n");
        builder.append("-".repeat(153));
        builder.append("\n|Row|Id |Trn Id|Date      |Balance        |Amount       |Cat|Acc |Opp Id|Open Bal.      |Year|Mn|L|Rec|Prd|Description                             |Act |\n");
        int row = 1;
        for(TransactionReportDTO next : transactionData) {
            builder.append("|");
            builder.append(getNumberString(row, 3));
            builder.append("|");
            builder.append(getNumberString(next.getId(),3));
            builder.append("|");
            builder.append(getNumberString(next.getTransactionId(),6));
            builder.append("|");
            builder.append(next.getDate());
            builder.append("|");
            builder.append(getFinancialAmountString(next.getBalance()));
            builder.append("|");
            if(next.getAmount() != null) {
                builder.append(next.getAmount().toFormattedString(10));
            } else {
                builder.append("             ");
            }
            builder.append("|");
            if(next.getCategory() != null) {
                builder.append(next.getCategory().getId());
            } else {
                builder.append("   ");
            }
            builder.append("|");
            if(next.getAccount() != null) {
                builder.append(next.getAccount().getId());
            } else {
                builder.append("    ");
            }
            builder.append("|");
            builder.append(getNumberString(next.getOppositeId(),6));
            builder.append("|");
            if(next.getStatement() != null) {
                builder.append(next.getStatement().getOpenBalance().toFormattedString(12));
                builder.append("|");
                builder.append(getPaddedString(next.getStatement().getYear().toString(),4));
                builder.append("|");
                builder.append(getPaddedString(next.getStatement().getMonth().toString(),2));
                builder.append("|");
                builder.append(next.getStatement().getLocked() ? "L" : " ");
                builder.append("|");
            } else {
                builder.append("               |    |  | |");
            }
            if(next.getFromReconciliation() != null && next.getFromReconciliation()) {
                builder.append(" R ");
            } else {
                builder.append("   ");
            }
            builder.append("|");
            if(next.getPredicted() != null && next.getPredicted()) {
                builder.append(" P ");
            } else {
                builder.append("   ");
            }
            builder.append("|");
            if(next.getType() == TransactionReportTypeDTO.TRANSACTION) {
                builder.append(getPaddedString(next.getDescription(), 40));
            } else {
                if(next.getType() == TransactionReportTypeDTO.OPEN_BALANCE) {
                    builder.append("Open Balance                            ");
                } else if(next.getType() == TransactionReportTypeDTO.TODAY_BALANCE) {
                    builder.append("Today Balance                           ");
                } else {
                    builder.append("Future Balance                          ");
                }
            }
            builder.append("|");
            if(next.getActionUpdate() != null && next.getActionUpdate()) {
                builder.append("U");
            } else {
                builder.append(" ");
            }
            if(next.getActionReconcile() != null && next.getActionReconcile()) {
                builder.append("R");
            } else {
                builder.append(" ");
            }
            if(next.getActionUnreconcile() != null && next.getActionUnreconcile()) {
                builder.append("u");
            } else {
                builder.append(" ");
            }
            if(next.getActionDelete() != null && next.getActionDelete()) {
                builder.append("D");
            } else {
                builder.append(" ");
            }
            builder.append("|");
            builder.append("\n");
            row++;
        }
        builder.append("-".repeat(153));
        builder.append("\n");

        LOG.info(builder.toString());
    }

    @BeforeEach
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

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(66)))
                .andExpect(jsonPath("[0].balance.value", is(1029.0)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[59].balance.value", is(-2148.15)))
                .andExpect(jsonPath("[59].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterDescription() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setDescription("sainsburys");

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("[0].date", is("2023-04-27")))
                .andExpect(jsonPath("[2].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterFromRec() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(true);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(22)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[21].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterPredicted() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(true);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("[1].date", is("2023-05-24")))
                .andExpect(jsonPath("[1].type", is("TODAY_BALANCE")))
                .andExpect(jsonPath("[6].date", is("2023-06-22")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterStandard() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(37)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[36].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterNotLocked() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setLocked(false);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(34)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[33].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterNotLockedBank() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setLocked(false);
        filter.setAccounts(List.of(account));

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(33)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[32].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
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

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("[0].date", is("2023-04-11")))
                .andExpect(jsonPath("[10].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterAccount() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("BANK");

        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setAccounts(List.of(account));

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(5)))
//                .andExpect(jsonPath("[0].date", is("2023-04-22")))
//                .andExpect(jsonPath("[4].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterDate() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setDateRange(new DateRangeDTO("2023-04-20",null));

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("[0].date", is("2023-04-20")))
                .andExpect(jsonPath("[17].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterValue() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setValueRange(new ValueRangeDTO(-20,15));

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[2].date", is("2023-04-10")))
                .andExpect(jsonPath("[24].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFilterValueLimit() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setValueRange(new ValueRangeDTO(-20,15));
        filter.setMaxPageSize(1);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("[0].date", is("2023-04-22")))
                .andExpect(jsonPath("[1].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testNull() throws Exception {
        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content("{}")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(66)))
                .andExpect(jsonPath("[0].balance.value", is(1029.0)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[59].date", is("2023-05-24")))
                .andExpect(jsonPath("[65].balance.value", is(-1490.05)))
                .andExpect(jsonPath("[65].date", is("2023-06-22")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void testFromString() throws Exception {
        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content("{\"accounts\":[],\"categories\":[],\"predicted\":false,\"locked\":false,\"fromReconciled\":false}")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(34)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[33].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
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

        Assertions.assertEquals(1,filter.getTransactionSorts().size());

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("[0].date", is("2023-05-24")))
                .andExpect(jsonPath("[6].date", is("2023-06-22")))
                .andExpect(jsonPath("[0].description", is("Octopus")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
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

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("[0].date", is("2023-05-24")))
                .andExpect(jsonPath("[7].date", is("2023-06-22")))
                .andExpect(jsonPath("[1].description", is("Wages")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
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

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("[0].date", is("2023-05-24")))
                .andExpect(jsonPath("[7].date", is("2023-06-22")))
                .andExpect(jsonPath("[1].description", is("Disney Plus")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
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

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("[0].date", is("2023-06-01")))
                .andExpect(jsonPath("[7].date", is("2023-06-22")))
                .andExpect(jsonPath("[0].description", is("Council Tax")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
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

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("[0].date", is("2023-05-24")))
                .andExpect(jsonPath("[7].date", is("2023-06-22")))
                .andExpect(jsonPath("[4].description", is("Netflix")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void checkAddAmendAndRemoveReported() throws Exception {
        // Add a new transaction.
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setDate("2023-05-18");
        transaction.setAmount(BigDecimal.valueOf(-13.92));
        transaction.setDescription("CheckAddReported-Test");
        List<TransactionDTO> transactions = new ArrayList<>();
        transactions.add(transaction);
        transactions = accountTransactionManager.createTransaction(transactions);

        Assertions.assertEquals(1,transactions.size());
        int transactionId = transactions.get(0).getId();

        // Check the report.
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setValueRange(new ValueRangeDTO(-20,15));

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(26)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[2].date", is("2023-04-10")))
                .andExpect(jsonPath("[24].date", is("2023-05-18")))
                .andExpect(jsonPath("[24].amount.value", is(-13.92)))
                .andExpect(jsonPath("[25].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Amend and recheck, find the reported transaction.
        TransactionReportDTO reported = null;
        for(TransactionReportDTO next : transactionData) {
            if(next.getTransactionId() != null && next.getTransactionId().equals(transactionId)) {
                reported = next;
                break;
            }
        }

        if(reported != null) {
            reported.setAmount(new FinancialAmount(BigDecimal.valueOf(-13.56)));
            List<TransactionReportDTO> updates = new ArrayList<>();
            updates.add(reported);
            accountTransactionManager.updateTransactions(updates);
        }

        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[24].amount.value", is(-13.56)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Remove the transaction.
        accountTransactionManager.deleteTransactions(transactions);

        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("[24].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void checkReconcileAndUnreconcileUpdates() throws Exception {
        // Check the report.
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(false);
        filter.setPredicted(false);
        filter.setValueRange(new ValueRangeDTO(-20,15));

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[1].date", is("2023-04-08")))
                .andExpect(jsonPath("[1].amount.value", is(-8.99)))
                .andExpect(jsonPath("[1].category.id", is("FDG")))
                .andExpect(jsonPath("[1].actionReconcile", is(true)))
                .andExpect(jsonPath("[24].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Find the transaction to reconcile.
        List<Integer> ids = new ArrayList<>();
        ids.add(transactionData.get(1).getTransactionId());

        // Reconcile the transaction.
        ReconcileTransactionDTO reconcileTransaction = new ReconcileTransactionDTO();
        reconcileTransaction.setTransactions(ids);
        reconcileTransaction.setReconcile(true);
        reconciliationManager.reconcile(reconcileTransaction);

        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("[1].date", is("2023-04-08")))
                .andExpect(jsonPath("[1].amount.value", is(-8.99)))
                .andExpect(jsonPath("[1].category.id", is("FDG")))
                .andExpect(jsonPath("[1].statement.month", is(4)))
                .andExpect(jsonPath("[1].statement.year", is(2023)))
                .andExpect(jsonPath("[24].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Unreconcile and check the data goes back.
        reconcileTransaction.setReconcile(false);
        reconciliationManager.reconcile(reconcileTransaction);

        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("[0].date", is("2023-04-06")))
                .andExpect(jsonPath("[1].date", is("2023-04-08")))
                .andExpect(jsonPath("[1].amount.value", is(-8.99)))
                .andExpect(jsonPath("[1].category.id", is("FDG")))
                .andExpect(jsonPath("[1].actionReconcile", is(true)))
                .andExpect(jsonPath("[24].date", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void checkLockStatement() throws Exception {
        // Check the report.
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setPredicted(false);

        AccountDTO account = new AccountDTO();
        account.setId("BANK");

        List<AccountDTO> accounts = new ArrayList<>();
        accounts.add(account);

        filter.setAccounts(accounts);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("[20].statement.month", is(5)))
                .andExpect(jsonPath("[21].statement.month", is(5)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Lock the statement.
        StatementIdDTO statementId = new StatementIdDTO();
        statementId.setAccountId("BANK");
        statementId.setYear(2023);
        statementId.setMonth(5);
        Iterable<StatementDTO> newStatement = statementManager.statementLock(statementId,accountTransactionManager);

        // Check everything is ok.
        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(29)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Rollback the statement
        for(StatementDTO next: newStatement){
            if(!next.getLocked()) {
                statementManager.deleteStatement(next, accountTransactionManager);
            }
        }

        // Check everything is ok.
        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void createTransactionFromRecFile() throws Exception {
        // Check the report.
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setPredicted(false);

        AccountDTO account = new AccountDTO();
        account.setId("BANK");

        List<AccountDTO> accounts = new ArrayList<>();
        accounts.add(account);

        filter.setAccounts(accounts);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("[26].date", is("2023-05-24")))
                .andExpect(jsonPath("[13].description", is("NEWDAY LTD")))
                .andExpect(jsonPath("[13].actionReconcile").doesNotExist())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Find the transaction that we are going to create.
        for(TransactionReportDTO next: transactionData){
            if(next.getDescription() != null && next.getDescription().equalsIgnoreCase("newday ltd")) {
                // Update this trandaction category.
                CategoryDTO category = new CategoryDTO();
                category.setId("HSE");
                next.setCategory(category);

                List<TransactionReportDTO> updateTransactions = new ArrayList<>();
                updateTransactions.add(next);
                accountTransactionManager.updateTransactions(updateTransactions);
            }
        }

        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("[26].date", is("2023-05-24")))
                .andExpect(jsonPath("[13].actionReconcile", is(true)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // Delete the transaction
        TransactionDTO transaction = new TransactionDTO();
        for(TransactionReportDTO next: transactionData) {
            if (next.getDescription() != null && next.getDescription().equalsIgnoreCase("newday ltd")) {
                transaction.setId(next.getTransactionId());
            }
        }

        accountTransactionManager.deleteTransactions(Collections.singletonList(transaction));

        result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("[26].date", is("2023-05-24")))
                .andExpect(jsonPath("[16].actionReconcile").doesNotExist())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        objectMapper = new ObjectMapper();
        transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);
    }

    @Test
    public void checkRecIssue() throws Exception {
        // Add a new transaction.
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccountId("BANK");
        transaction.setCategoryId("HSE");
        transaction.setDate("2023-04-21");
        transaction.setAmount(BigDecimal.valueOf(-4245.70));
        transaction.setDescription("Check Issue-Test");
        List<TransactionDTO> transactions = new ArrayList<>();
        transactions.add(transaction);
        transactions = accountTransactionManager.createTransaction(transactions);

        List<Integer> ids = new ArrayList<>();
        ids.add(transactions.get(0).getId());

        // Reconcile the transaction.
        ReconcileTransactionDTO reconcileTransaction = new ReconcileTransactionDTO();
        reconcileTransaction.setTransactions(ids);
        reconcileTransaction.setReconcile(true);
        reconciliationManager.reconcile(reconcileTransaction);

        // Check the report.
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setPredicted(false);

        AccountDTO account = new AccountDTO();
        account.setId("BANK");

        List<AccountDTO> accounts = new ArrayList<>();
        accounts.add(account);

        filter.setAccounts(accounts);

        MvcResult result = getMockMvc().perform(post("/api/v1/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TransactionReportDTO> transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<TransactionReportDTO>>(){});
        logTransactionData(transactionData);

        // There should be 21 record that are 'from reconciliation'
        int recCount = 0;
        for(TransactionReportDTO next: transactionData) {
            if(next.getFromReconciliation() != null && next.getFromReconciliation()) {
                recCount++;
            }
        }
        Assertions.assertEquals(21, recCount);

        reconcileTransaction.setReconcile(false);
        reconciliationManager.reconcile(reconcileTransaction);

        // Delete the transaction
        TransactionDTO transactionForDelete = new TransactionDTO();
        for(TransactionReportDTO next: transactionData) {
            if (next.getDescription() != null && next.getDescription().equalsIgnoreCase("Check Issue-Test")) {
                transactionForDelete.setId(next.getTransactionId());
            }
        }

        accountTransactionManager.deleteTransactions(Collections.singletonList(transactionForDelete));
    }
}
