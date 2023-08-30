package com.jbr.middletier.money.data;

import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import com.jbr.middletier.money.schedule.AdjustmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name="Regular")
public class Regular {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name="account")
    @NotNull
    @ManyToOne
    private Account account;

    @Column(name="amount")
    @NotNull
    private double amount;

    @JoinColumn(name="category")
    @NotNull
    @ManyToOne
    private Category category;

    @Column(name="frequency")
    @NotNull
    @Size(max=2)
    private String frequency;

    @Column(name="weekend_adj")
    @Size(max=2)
    private String weekendAdj;

    @Column(name="start")
    @NotNull
    private LocalDate start;

    @Column(name="last_created")
    private LocalDate lastCreated;

    @Column(name="description")
    @Size(max=40)
    private String description;

    private static final Logger LOG = LoggerFactory.getLogger(Regular.class);

    private LocalDate addFrequency ( LocalDate fromDate ) throws CannotDetermineNextDateException {
        try {
            // Frequency is a number plus a letter, e.g. 20D = 20 days.
            String unit = this.frequency.substring(frequency.length() - 1);

            int count = Integer.parseInt(this.frequency.replace(unit, ""));
            if (count < 0) {
                LOG.info("Invalid count, defaulted to 1");
                count = 1;
            }

            return switch (unit) {
                case "D" -> {
                    LOG.info("Day x {}", count);
                    yield fromDate.plusDays(count);
                }
                case "W" -> {
                    LOG.info("Week x {}", count);
                    yield fromDate.plusWeeks(count);
                }
                case "M" -> {
                    LOG.info("Month x {}", count);
                    yield fromDate.plusMonths(count);
                }
                case "Y" -> {
                    LOG.info("Year x {}", count);
                    yield fromDate.plusYears(count);
                }
                default -> throw new CannotDetermineNextDateException("Unexpected frequency unit - {}" + unit);
            };
        } catch (CannotDetermineNextDateException ex) {
            throw ex;
        } catch( Exception ex) {
            throw new CannotDetermineNextDateException("Unexpected exception while adding frequency - " + fromDate + " " + this.frequency);
        }
    }

    public LocalDate getStart() {
        return this.start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getLastDate() {
        if(this.lastCreated == null) {
            return null;
        }

        return this.lastCreated;
    }

    public void setLastDate(LocalDate lastDate) {
        this.lastCreated = lastDate;
    }

    public boolean isNextDateToday(LocalDate useToday) throws CannotDetermineNextDateException {
        // Is the next date today?
        return useToday.equals(getNextDate(useToday));
    }

    private boolean isDateTodayOrFuture(LocalDate today, LocalDate fromDate) {
        // Is it today?
        if(today.equals(fromDate)) {
            return true;
        }

        return today.isBefore(fromDate);
    }

    private LocalDate internalNextDate(LocalDate today, LocalDate fromDate) throws CannotDetermineNextDateException {
        while(!isDateTodayOrFuture(today, fromDate)) {
            fromDate = addFrequency(fromDate);
        }

        return fromDate;
    }

    public LocalDate getNextDate(LocalDate today) throws CannotDetermineNextDateException {
        // If the last date is not set then use the start date, otherwise use the next date (add 1 frequency to the last date)
        if(this.getLastDate() == null) {
            return internalNextDate(today, getStart());
        } else {
            return internalNextDate(today, addFrequency(getLastDate()));
        }
    }

    public String getFrequency() {
        return this.frequency;
    }

    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {this.id = id;}

    public Account getAccount() {
        return this.account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Category getCategory() {
        return this.category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public AdjustmentType getWeekendAdj() {
        return AdjustmentType.getAdjustmentType(this.weekendAdj);
    }

    public void setWeekendAdj(AdjustmentType weekendAdj) {
        this.weekendAdj = weekendAdj.getTypeName();
    }
}
