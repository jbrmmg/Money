package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class CategoryTest extends Support {
    @Test
    public void getCategoryTest() throws Exception {
        // Get accounts (external), check that both categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/ext/money/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is("FDW")))
                .andExpect(jsonPath("$[2].id", is("FDT")))
                .andExpect(jsonPath("$[3].id", is("HSE")));

        // Get accounts (internal), check that both categories are returned and in the correct order..
        getMockMvc().perform(get("/jbr/int/money/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is("FDW")))
                .andExpect(jsonPath("$[2].id", is("FDT")))
                .andExpect(jsonPath("$[3].id", is("HSE")));
    }
}
