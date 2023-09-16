package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.dto.CategoryDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class CategoryTest extends Support {
    @Test
    public void getCategoryTest() throws Exception {
        // Get accounts (external), check that both categories are returned and in the correct order.
        getMockMvc().perform(get("/jbr/ext/money/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is("FDW")))
                .andExpect(jsonPath("$[2].id", is("FDT")))
                .andExpect(jsonPath("$[3].id", is("HSE")));

        // Get accounts (internal), check that both categories are returned and in the correct order.
        getMockMvc().perform(get("/jbr/int/money/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("FDG")))
                .andExpect(jsonPath("$[1].id", is("FDW")))
                .andExpect(jsonPath("$[2].id", is("FDT")))
                .andExpect(jsonPath("$[3].id", is("HSE")));
    }

    @Test
    public void crudCategoryTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("XXX");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        getMockMvc().perform(post("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("$[24].id", is("XXX")));

        category.setGroup("FDS");

        getMockMvc().perform(put("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("$[24].id", is("XXX")));

        getMockMvc().perform(delete("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)));
    }

    @Test
    public void addExistingTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("WGS");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(post("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Category already exists WGS", error);
    }

    @Test
    public void updateNonExistent() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("XXX");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(put("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find category with id XXX", error);
    }

    @Test
    public void deleteNonExistent() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("XXX");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("Cannot find category with id XXX", error);
    }

    @Test
    public void deleteSystemTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("TRF");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(delete("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("You cannot delete this category as it is used by system. (TRF)", error);
    }

    @Test
    public void updateSystemTest() throws Exception {
        CategoryDTO category = new CategoryDTO();
        category.setId("TRF");
        category.setColour("FCFCFC");
        category.setExpense(true);
        category.setGroup("FRD");
        category.setRestricted(false);
        category.setSort(9999L);
        category.setSystemUse(false);

        String error = Objects.requireNonNull(getMockMvc().perform(put("/jbr/int/money/categories")
                        .content(this.json(category))
                        .contentType(getContentType()))
                .andExpect(status().isForbidden())
                .andReturn().getResolvedException()).getMessage();
        Assert.assertEquals("You cannot update this category as it is used by system. (TRF)", error);
    }
}
