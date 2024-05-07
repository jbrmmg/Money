package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.control.AccountController;
import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
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

    private void logTransactionData(TransactionDataDTO transactionData) {
        LOG.info("-------------------------------------------------------------------------------------------------------------------");
        LOG.info("TRANSACTION DETAILS");
        LOG.info("  Open    {} {}", transactionData.getOpenDate(), transactionData.getOpenBalance());
        LOG.info("  Today   {} {}", transactionData.getToday(), transactionData.getTodayBalance());
        LOG.info("  Forward {} {}", transactionData.getForwardDate(), transactionData.getForwardBalance());
        LOG.info("");
        for(TransactionReportDTO next : transactionData.getTransactions()) {
            LOG.info("  {} {} {} {}",
                    next.getId() == null ? "" : next.getId(),
                    next.getDate(),
                    next.getAmount().toString(),
                    next.getCategory());
        }
        LOG.info("-------------------------------------------------------------------------------------------------------------------");
    }

    @Test
    public void testFilter1() throws Exception {
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setReconciliationAccount("BANK");

        MvcResult result = getMockMvc().perform(get("/jbr/int/money/transaction/list")
                        .content(this.json(filter))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(59)))
                .andExpect(jsonPath("$.openDate", is("2023-04-06")))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        TransactionDataDTO transactionData = objectMapper.readValue(result.getResponse().getContentAsString(),TransactionDataDTO.class);
        logTransactionData(transactionData);
    }
}
