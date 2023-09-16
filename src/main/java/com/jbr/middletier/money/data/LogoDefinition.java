package com.jbr.middletier.money.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="logoDefinition")
public class LogoDefinition {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column
    private Integer fontSize;

    @Column
    private Integer y;

    @Column
    private String fillColour;

    @Column
    private String borderColour;

    @Column
    private String textColour;

    @Column
    private String logoText;

    @Column
    private Boolean secondBorder;

    @Column
    private String borderTwoColour;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public String getFillColour() {
        return fillColour;
    }

    public void setFillColour(String fillColour) {
        this.fillColour = fillColour;
    }

    public String getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(String borderColour) {
        this.borderColour = borderColour;
    }

    public String getTextColour() {
        return textColour;
    }

    public void setTextColour(String textColour) {
        this.textColour = textColour;
    }

    public String getLogoText() {
        return logoText;
    }

    public void setLogoText(String logoText) {
        this.logoText = logoText;
    }

    public boolean getSecondBorder() {
        return secondBorder != null && secondBorder;
    }

    public void setSecondBorder(Boolean secondBorder) {
        this.secondBorder = secondBorder;
    }

    public String getBorderTwoColour() {
        return borderTwoColour;
    }

    public void setBorderTwoColour(String borderTwoColour) {
        this.borderTwoColour = borderTwoColour;
    }
}
