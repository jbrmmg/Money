package com.jbr.middletier.money.data.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;

/**
 * Created by jason on 04/03/17.
 */
@Setter
@Getter
@Entity
@Table(name="category")
public class Category implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @NotNull
    @Size(max=3)
    @Column(name="id")
    private String id;

    @Size(max=45)
    @Column(name="name")
    private String name;

    @Size(max=1000000)
    @Column(name="sort")
    private Long sort;

    @NotNull
    @Column(name="restricted")
    private Boolean restricted;

    @Size(max=6)
    @Column(name="colour")
    private String colour;

    @Size(max=6)
    @Column(name="expense")
    private Boolean expense;

    @Size(max=45)
    @Column(name="groupid")
    private String group;

    @NotNull
    @Column(name="system_use")
    private Boolean systemUse;

}
