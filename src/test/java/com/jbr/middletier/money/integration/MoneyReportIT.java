package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@WebAppConfiguration
@ContextConfiguration(initializers = {MoneyReportIT.Initializer.class})
@ActiveProfiles(value="report-it")
public class MoneyReportIT extends Support {
    private static final Logger LOG = LoggerFactory.getLogger(MoneyReportIT.class);

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
        return String.format("%-" + size + "s", value == null ? "" : value);
    }

    private void logTransactionData(TransactionDataDTO transactionData) {
        LOG.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
        LOG.info("TRANSACTION DETAILS");
        LOG.info("  Open    {} {}", transactionData.getOpenDate(), transactionData.getOpenBalance().toFormattedString(12));
        LOG.info("  Today   {} {}", transactionData.getToday(), transactionData.getTodayBalance().toFormattedString(12));
        LOG.info("  Forward {} {}", getPaddedString(transactionData.getForwardDate(),10), transactionData.getForwardBalance().toFormattedString(12));
        LOG.info("");
        int row = 1;
        for(TransactionReportDTO next : transactionData.getTransactions()) {
            LOG.info("  {} {} {} {} {} {} {} {} {} {} {} {} {} {} {} |",
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
                    getPaddedString(next.getDescription(),40));
            row++;
        }
        LOG.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
    }

    @Test
    public void testFilterAll() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setReconciliationAccount("BANK");

        MvcResult result = getMockMvc().perform(get("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(59)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
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

        MvcResult result = getMockMvc().perform(get("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(18)))
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

        String error = Objects.requireNonNull(getMockMvc().perform(get("/jbr/int/money/transaction/list")
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

        MvcResult result = getMockMvc().perform(get("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(6)))
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

        MvcResult result = getMockMvc().perform(get("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(38)))
                .andExpect(jsonPath("openDate", is("2023-04-06")))
                .andExpect(jsonPath("today", is("2023-05-24")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }
}
