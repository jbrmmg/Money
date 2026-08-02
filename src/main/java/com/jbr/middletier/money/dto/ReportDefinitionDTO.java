package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportDefinitionDTO {
    @NotBlank(message = "type cannot be blank")
    private String type;

    @NotNull(message = "year cannot be null")
    private Integer year;

    private Integer month;

    private String to;

    public ReportDefinitionDTO() {}

    public ReportDefinitionDTO(String type, Integer year, Integer month) {
        this.type = type;
        this.year = year;
        this.month = month;
    }
}
