package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dto.CategoryDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class CategoryTest extends Support {
    @Test
    void getCategoryTest() throws Exception {
        // Get accounts (external), check that both categories are returned and in the correct order.
        getMockMvc().perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is("FDW")))
                .andExpect(jsonPath("$[2].id", is("FDT")))
                .andExpect(jsonPath("$[3].id", is("HSE")));

        // Get accounts (internal), check that both categories are returned and in the correct order.
        getMockMvc().perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is("FDW")))
                .andExpect(jsonPath("$[2].id", is("FDT")))
                .andExpect(jsonPath("$[3].id", is("HSE")));
    }

    @Test
    void crudCategoryTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("XXX");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        getMockMvc().perform(post("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("$[24].id", is("XXX")));

        category.setGroup("FDS");

        getMockMvc().perform(put("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("$[24].id", is("XXX")));

        getMockMvc().perform(delete("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)));
    }

    @Test
    void addExistingTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("WGS");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(post("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Category already exists WGS", error);
    }

    @Test
    void updateNonExistent() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("XXX");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(put("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find category with id XXX", error);
    }

    @Test
    void deleteNonExistent() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("XXX");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("Cannot find category with id XXX", error);
    }

    @Test
    void deleteSystemTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("TRF");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("You cannot delete this category as it is used by system. (TRF)", error);
    }

    @Test
    void updateSystemTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("TRF");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(put("/api/v1/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assertions.assertEquals("You cannot update this category as it is used by system. (TRF)", error);
    }
}
