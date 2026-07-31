package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.primary.Regular;
import com.jbr.middletier.money.data.primary.repository.RegularRepository;
import com.jbr.middletier.money.dto.RegularDTO;
import com.jbr.middletier.money.dto.mapper.RegularMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class RegularTest extends Support {
    @Autowired
    private RegularRepository regularRepository;

    @Autowired
    private RegularMapper regularMapper;

    @BeforeEach
    void cleanup() {
        // Ensure there are no regular payments.
        regularRepository.deleteAll();
    }

    private RegularDTO createTestRegular(String accountId, String categoryId, String adjustmentType, BigDecimal amount, String description, String frequency, String start) {
        RegularDTO regular = new RegularDTO();
        regular.setAccountId(accountId);
        regular.setCategoryId(categoryId);
        regular.setWeekendAdj(adjustmentType);
        regular.setAmount(amount);
        regular.setDescription(description);
        regular.setFrequency(frequency);
        regular.setStart(start);

        return regular;
    }

    @Test
    void testNoRegulars() throws Exception {
        getMockMvc().perform(get("/api/v1/transaction/regulars")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        getMockMvc().perform(get("/api/v1/transaction/regulars")
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testCreate() throws Exception {
        RegularDTO newRegular = createTestRegular("AMEX", "HSE", "FW", BigDecimal.valueOf(102.21), "Testing", "1W", "2023-06-01");

        getMockMvc().perform(post("/api/v1/transaction/regulars")
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
    void testUpdate() throws Exception {
        RegularDTO updateRegular = createTestRegular("BANK", "FDG", "BW", BigDecimal.valueOf(122.39), "Testing 2", "1M", "2023-06-01");

        Regular savedRegular = regularRepository.save(regularMapper.map(updateRegular,Regular.class));

        updateRegular.setId(savedRegular.getId());
        updateRegular.setDescription("Testing 3");

        getMockMvc().perform(put("/api/v1/transaction/regulars")
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
    void testDelete() throws Exception {
        RegularDTO deleteRegular = createTestRegular("BANK", "HSE", "BW", BigDecimal.valueOf(21.21), "Testing", "1M", "2023-06-01");

        Regular savedRegular = regularRepository.save(regularMapper.map(deleteRegular,Regular.class));

        deleteRegular.setId(savedRegular.getId());

        getMockMvc().perform(delete("/api/v1/transaction/regulars")
                        .content(this.json(deleteRegular))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testInvalidException() throws Exception {
        RegularDTO deleteRegular = createTestRegular("BANK", "HSE", "BW", BigDecimal.valueOf(21.21), "Testing", "1M", "2023-06-01");

        Regular savedRegular = regularRepository.save(regularMapper.map(deleteRegular,Regular.class));

        deleteRegular.setId(savedRegular.getId() + 1);

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/transaction/regulars")
                        .content(this.json(deleteRegular))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find regular payment with id " + deleteRegular.getId(), error);
    }

    @Test
    void testAlreadyExist() throws Exception {
        RegularDTO createRegular = createTestRegular("BANK", "HSE", "BW", BigDecimal.valueOf(21.21), "Testing", "1M", "2023-06-01");

        Regular savedRegular = regularRepository.save(regularMapper.map(createRegular,Regular.class));

        createRegular.setId(savedRegular.getId());

        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/transaction/regulars")
                        .content(this.json(createRegular))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Regular Payment already exists " + createRegular.getId(), error);
    }
}
