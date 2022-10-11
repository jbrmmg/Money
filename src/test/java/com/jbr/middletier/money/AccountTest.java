package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dto.AccountDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import java.util.Objects;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class AccountTest extends Support {
    @Test
    public void getAccountTest() throws Exception {
        // Get accounts (external), check that both categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/ext/money/accounts/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")));

        // Get accounts (internal), check that both categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/int/money/accounts/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")));
    }

    @Test
    public void getLogoTest() throws Exception {
        getMockMvc().perform(get("/jbr/int/money/account/logo?id=AMEX&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/int/money/account/logo?id=AMEX&disabled=false"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/int/money/account/logo?id=XYXY&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/int/money/account/logo?id=XYXY&disabled=false"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/ext/money/account/logo?id=AMEX&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/ext/money/account/logo?id=AMEX&disabled=false"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/ext/money/account/logo?id=XYXY&disabled=true"))
                .andExpect(status().isOk());
        getMockMvc().perform(get("/jbr/ext/money/account/logo?id=XYXY&disabled=false"))
                .andExpect(status().isOk());
    }

    @Test
    public void crudAccountTest() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        getMockMvc().perform(post("/jbr/int/money/accounts")
                .content(this.json(account))
                .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")))
                .andExpect(jsonPath("$[4].id", is("XXXX")));

        account.setImagePrefix("test2");

        getMockMvc().perform(put("/jbr/int/money/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")))
                .andExpect(jsonPath("$[4].id", is("XXXX")));

        getMockMvc().perform(delete("/jbr/int/money/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("AMEX")))
                .andExpect(jsonPath("$[1].id", is("BANK")))
                .andExpect(jsonPath("$[2].id", is("JLPC")))
                .andExpect(jsonPath("$[3].id", is("NWDE")));
    }

    @Test
    public void addExistingTest() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Account already exists AMEX", error);
    }

    @Test
    public void updateNonExistent() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(put("/jbr/int/money/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find account with id XXXX", error);
    }

    @Test
    public void deleteNonExistent() throws Exception {
        AccountDTO account = new AccountDTO();
        account.setId("XXXX");
        account.setName("Testing");
        account.setColour("FCFCFC");
        account.setImagePrefix("test");

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/accounts")
                        .content(this.json(account))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find account with id XXXX", error);
    }
}
