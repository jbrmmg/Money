package com.jbr.middletier.money.data.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Entity
@Table(name="logo_definition")
public class LogoDefinition {
    @Getter
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Getter
    @Column(name="font_size")
    private Integer fontSize;

    @Getter
    @Column
    private Integer y;

    @Getter
    @Column(name="fill_colour")
    private String fillColour;

    @Getter
    @Column(name="border_colour")
    private String borderColour;

    @Getter
    @Column(name="text_colour")
    private String textColour;

    @Getter
    @Column(name="logo_text")
    private String logoText;

    @Column(name="second_border")
    private Boolean secondBorder;

    @Getter
    @Column(name="border_two_colour")
    private String borderTwoColour;

    public boolean getSecondBorder() {
        return secondBorder != null && secondBorder;
    }
}
