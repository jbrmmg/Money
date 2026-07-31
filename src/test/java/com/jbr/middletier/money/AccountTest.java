package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dto.AccountDTO;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import java.util.Objects;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class AccountTest extends Support {
    @Test
    void getAccountTest() throws Exception {
        // Get accounts (external), check that both categories are returned and in the correct order.
        getMockMvc().perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")));

        // Get accounts (internal), check that both categories are returned and in the correct order.
        getMockMvc().perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")));
    }

    @Test
    void getLogoTest() throws Exception {
        getMockMvc().perform(get("/api/v1/account/logo?id=AMEX&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=AMEX&disabled=false"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=XYXY&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=XYXY&disabled=false"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=AMEX&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=AMEX&disabled=false"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=XYXY&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/api/v1/account/logo?id=XYXY&disabled=false"))
                .andExpect(status().isOk());

        // Check the id is validated.
        try {
            getMockMvc().perform(get("/api/v1/account/logo?id=XYY&disabled=false")
                            .contentType(getContentType()))
                    .andExpect(status().isBadRequest());
        } catch (ServletException ex) {
            Assertions.assertTrue(ex.getRootCause().getMessage().contains("Id must be a four letter code"));
        }
    }

    @Test
    void crudAccountTest() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        account.setName("Testing");
        account.setColour("FFFFF");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/accounts")
                .content(this.json(account))
                .contentType(getContentType()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertTrue(error.contains("Colour must be a 6 digit hex value."));

        account.setColour("FFFFFF");
        getMockMvc().perform(post("/api/v1/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")))
                .andExpect(jsonPath("$[4].id", is("XXXX")));

        account.setImagePrefix("test2");

        getMockMvc().perform(put("/api/v1/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")))
                .andExpect(jsonPath("$[4].id", is("XXXX")));

        getMockMvc().perform(delete("/api/v1/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")));
    }

    @Test
    void addExistingTest() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Account already exists AMEX", error);
    }

    @Test
    void updateNonExistent() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(put("/api/v1/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find account with id XXXX", error);
    }

    @Test
    void deleteNonExistent() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find account with id XXXX", error);
    }
}
