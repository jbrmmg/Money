package com.jbr.middletier.money.data.primary;

import com.jbr.middletier.money.schedule.AdjustmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;

/**
 * Created by jason on 07/03/17.
 */

@Entity
@Table(name="account")
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name="id")
    @NotNull(message = "Account ID cannot be null.")
    @Pattern(regexp="^[0-9A-Z]{4}$",message="Account id must be uppercase and/or numbers of length 4.")
    private String id;

    @Column(name="name")
    @Pattern(regexp="^[0-9A-Za-z\\s]{1,45}$",message="Account name must be alpha numeric upto 45 characters.")
    private String name;

    @Column(name="image_prefix")
    @Pattern(regexp="^[0-9a-zA-Z]{1,45}$",message="Image Prefix can only contain letters or digits up to 45 characters.")
    private String imagePrefix;

    @Column(name="colour")
    @Pattern(regexp="^[0-9a-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;

    @Column(name="closed")
    private Boolean closed;

    @Column(name="transfer_account")
    @Pattern(regexp="^[0-9A-Z]{4}$",message="Account id must be uppercase and/or numbers of length 4.")
    private String transferAccountId;

    @Column(name="transfer_day")
    private Integer transferDay;

    @Column(name="weekend_adj")
    @Size(max=2)
    private String weekendAdjust;

    public String getId() {
        return id;
    }

    public void setId(String id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getImagePrefix() {
        return imagePrefix;
    }

    public void setImagePrefix(String imagePrefix) { this.imagePrefix = imagePrefix; }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) { this.colour = colour; }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public @Pattern(regexp = "^[0-9A-Z]{4}$", message = "Account id must be uppercase and/or numbers of length 4.") String getTransferAccountId() {
        return transferAccountId;
    }

    public void setTransferAccountId(@Pattern(regexp = "^[0-9A-Z]{4}$", message = "Account id must be uppercase and/or numbers of length 4.") String transferAccountId) {
        this.transferAccountId = transferAccountId;
    }

    public Integer getTransferDay() {
        return transferDay;
    }

    public void setTransferDay(Integer transferDay) {
        this.transferDay = transferDay;
    }

    public AdjustmentType getWeekendAdj() {
        if(this.weekendAdjust == null) {
            return null;
        }

        return AdjustmentType.getAdjustmentType(this.weekendAdjust);
    }

    public void setWeekendAdj(AdjustmentType weekendAdj) {
        if(weekendAdj == null) {
            this.weekendAdjust = null;
        }

        this.weekendAdjust = weekendAdj.getTypeName();
    }
}
