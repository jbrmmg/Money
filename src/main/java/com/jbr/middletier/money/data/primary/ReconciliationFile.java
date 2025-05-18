package com.jbr.middletier.money.data.primary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name="reconciliation_file")
public class ReconciliationFile implements Serializable {
    @Id
    @Size(max=100)
    @Column(name="name")
    @NotNull(message = "File name cannot be null.")
    private String name;

    @JoinColumn(name="account_id")
    @ManyToOne
    private Account account;

    @Column(name="last_modified")
    private LocalDateTime lastModified;

    @Column(name="size")
    private Long size;

    @Column(name="error")
    private String error;

    @Column(name="loaded")
    private Boolean loaded;
}
