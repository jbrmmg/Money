package com.jbr.middletier.money.data;

import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name="reconciliation_file")
public class ReconciliationFile implements Serializable {
    @Id
    @Size(max=100)
    @Column(name="name")
    @NotNull(message = "File name cannot be null.")
    private String name;

    @JoinColumn(name="account_id")
    @ManyToOne(optional = true)
    private Account account;

    @Column(name="last_modified")
    private LocalDateTime lastModified;

    @Column(name="size")
    private Long size;

    @Column(name="error")
    private String error;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
