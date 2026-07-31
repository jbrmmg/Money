package com.jbr.middletier.money.integration;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.Support;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.TransactionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.MySQLContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@WebAppConfiguration
@ContextConfiguration(initializers = {MoneyIT.Initializer.class})
@ActiveProfiles(value="it")
@Testcontainers
class MoneyIT extends Support  {
    @Autowired
    private TransactionRepository transactionRepository;

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

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
    }


    @Test
    void testBadTransaction() throws Exception {
        List<TransactionDTO> transactions = new ArrayList<>();

        TransactionDTO transaction = new TransactionDTO();
        transaction.setAmount(BigDecimal.valueOf(10));
        transactions.add(transaction);

        transaction = new TransactionDTO();
        transaction.setAmount(BigDecimal.valueOf(10));
        transactions.add(transaction);

        transaction = new TransactionDTO();
        transaction.setAmount(BigDecimal.valueOf(10));
        transactions.add(transaction);

        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/transaction")
                        .content(this.json(transactions))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("List size must be 1 or 2", error);
    }
}
