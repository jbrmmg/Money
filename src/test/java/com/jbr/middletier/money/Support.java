package com.jbr.middletier.money;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

public class Support {
    private MockMvc mockMvc;

    @SuppressWarnings("rawtypes")
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    public void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() {
        // Set up the mock web context.
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        // The AllTransaction table needs to be a view.
    }

    protected void deleteDirectoryContents(Path path) throws IOException {
        if(!Files.exists(path))
            return;

        //noinspection ResultOfMethodCallIgnored,resource
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    protected MediaType getContentType() {
        return new MediaType(MediaType.APPLICATION_JSON.getType(),
                MediaType.APPLICATION_JSON.getSubtype(),
                StandardCharsets.UTF_8);
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        //noinspection unchecked
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    public MockMvc getMockMvc() {
        return this.mockMvc;
    }

    public void reinstateStatements(StatementRepository statementRepository, AccountRepository accountRepository) {
        statementRepository.deleteAll();
        for(Account nextAccount : accountRepository.findAll()) {
            StatementId nextStatementId = new StatementId();
            nextStatementId.setAccount(nextAccount);
            nextStatementId.setYear(2010);
            nextStatementId.setMonth(1);
            Statement nextStatement = new Statement();
            nextStatement.setId(nextStatementId);
            nextStatement.setOpenBalance(0);
            nextStatement.setLocked(false);

            statementRepository.save(nextStatement);
        }
    }
}
