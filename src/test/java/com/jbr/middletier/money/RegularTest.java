package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.dataaccess.RegularRepository;
import com.jbr.middletier.money.dto.RegularDTO;
import com.jbr.middletier.money.dto.mapper.RegularMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class RegularTest extends Support {
    @Autowired
    private RegularRepository regularRepository;

    @Autowired
    private RegularMapper regularMapper;

    @Before
    public void cleanup() {
        // Ensure there are no regular payments.
        regularRepository.deleteAll();
    }

    private RegularDTO createTestRegular(String accountId, String categoryId, String adjustmentType, double amount, String description, String frequency) {
        RegularDTO regular = new RegularDTO();
        regular.setAccountId(accountId);
        regular.setCategoryId(categoryId);
        regular.setWeekendAdj(adjustmentType);
        regular.setAmount(amount);
        regular.setDescription(description);
        regular.setFrequency(frequency);

        return regular;
    }

    @Test
    public void testNoRegulars() throws Exception {
        getMockMvc().perform(get("/jbr/ext/money/transaction/regulars")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        getMockMvc().perform(get("/jbr/int/money/transaction/regulars")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testCreate() throws Exception {
        RegularDTO newRegular = createTestRegular("AMEX", "HSE", "FW", 102.21, "Testing", "1W");

        getMockMvc().perform(post("/jbr/int/money/transaction/regulars")
                        .content(this.json(newRegular))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount",is(102.21)))
                .andExpect(jsonPath("$[0].description",is("Testing")))
                .andExpect(jsonPath("$[0].accountId",is("AMEX")))
                .andExpect(jsonPath("$[0].categoryId",is("HSE")))
                .andExpect(jsonPath("$[0].weekendAdj",is("FW")))
                .andExpect(jsonPath("$[0].frequency",is("1W")));
    }

    @Test
    public void testUpdate() throws Exception {
        RegularDTO updateRegular = createTestRegular("BANK", "FDG", "BW", 122.39, "Testing 2", "1M");

        Regular savedRegular = regularRepository.save(regularMapper.map(updateRegular,Regular.class));

        updateRegular.setId(savedRegular.getId());
        updateRegular.setDescription("Testing 3");

        getMockMvc().perform(put("/jbr/int/money/transaction/regulars")
                        .content(this.json(updateRegular))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount",is(122.39)))
                .andExpect(jsonPath("$[0].description",is("Testing 3")))
                .andExpect(jsonPath("$[0].accountId",is("BANK")))
                .andExpect(jsonPath("$[0].categoryId",is("FDG")))
                .andExpect(jsonPath("$[0].weekendAdj",is("BW")))
                .andExpect(jsonPath("$[0].frequency",is("1M")));
    }

    @Test
    public void testDelete() throws Exception {
        RegularDTO deleteRegular = createTestRegular("BANK", "HSE", "BW", 21.21, "Testing", "1M");

        Regular savedRegular = regularRepository.save(regularMapper.map(deleteRegular,Regular.class));

        deleteRegular.setId(savedRegular.getId());

        getMockMvc().perform(delete("/jbr/int/money/transaction/regulars")
                        .content(this.json(deleteRegular))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testInvalidException() throws Exception {
        RegularDTO deleteRegular = createTestRegular("BANK", "HSE", "BW", 21.21, "Testing", "1M");

        Regular savedRegular = regularRepository.save(regularMapper.map(deleteRegular,Regular.class));

        deleteRegular.setId(savedRegular.getId() + 1);

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/transaction/regulars")
                        .content(this.json(deleteRegular))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find regular payment with id " + deleteRegular.getId(), error);
    }

    @Test
    public void testAlreadyExist() throws Exception {
        RegularDTO createRegular = createTestRegular("BANK", "HSE", "BW", 21.21, "Testing", "1M");

        Regular savedRegular = regularRepository.save(regularMapper.map(createRegular,Regular.class));

        createRegular.setId(savedRegular.getId());

        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/transaction/regulars")
                        .content(this.json(createRegular))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Regular Payment already exists " + createRegular.getId(), error);
    }
}
