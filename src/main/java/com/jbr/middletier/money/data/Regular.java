package com.jbr.middletier.money.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Entity
@Table(name="Regular")
public class Regular {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @Column(name="account")
    @NotNull
    @Size(max=4)
    private String account;

    @Column(name="amount")
    @NotNull
    private double amount;

    @Column(name="category")
    @NotNull
    @Size(max=3)
    private String category;

    @Column(name="frequency")
    @NotNull
    @Size(max=2)
    private String frequency;

    @Column(name="weekend_adj")
    @Size(max=2)
    private String weekendAdj;

    @Column(name="start")
    @NotNull
    private Date start;

    @Column(name="last_created")
    private Date lastCreated;

    @Column(name="description")
    @Size(max=40)
    private String description;

    final static private Logger LOG = LoggerFactory.getLogger(Regular.class);
    private static SimpleDateFormat loggingSDF = new SimpleDateFormat("dd-MM-yyyy");

    private Date removeTime(Date dateTime) {
        // Force the date to be midday.
        Calendar cal = Calendar.getInstance(); // locale-specific
        cal.setTime(dateTime);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public class CannotDetermineNextDateException extends Exception {
        public CannotDetermineNextDateException(String message)  {
            super(message);
        }
    }

    private Date addFrequency ( Date fromDate ) throws CannotDetermineNextDateException {
        String fromDateString = "<not set>";

        try {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(removeTime(fromDate));

            // Frequency is a number plus a letter, eg. 20D = 20 days.
            String unit = this.frequency.substring(frequency.length() - 1);

            int count = Integer.parseInt(this.frequency.replace(unit, ""));
            if (count < 0) {
                LOG.info("Invalid count, defaulted to 1");
                count = 1;
            }

            switch (unit) {
                case "D":
                    LOG.info("Day x " + count);
                    calendar.add(Calendar.DATE, count);
                    break;

                case "W":
                    LOG.info("Week x " + count);
                    calendar.add(Calendar.DATE, count * 7);
                    break;

                case "M":
                    LOG.info("Month x " + count);
                    calendar.add(Calendar.MONTH, count);
                    break;

                case "Y":
                    LOG.info("Year x " + count);
                    calendar.add(Calendar.YEAR, count);
                    break;

                default:
                    throw new CannotDetermineNextDateException("Unexpected frequency unit - " + unit );
            }

            LOG.info("Add Frequency Date: " + loggingSDF.format(calendar.getTime()));

            return calendar.getTime();
        } catch (CannotDetermineNextDateException ex) {
            throw ex;
        } catch( Exception ex) {
            throw new CannotDetermineNextDateException("Unexpected exception while adding frequency - " + fromDateString + " " + this.frequency);
        }
    }

    public Date getStart() {
        return removeTime(this.start);
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getLastDate() {
        if(this.lastCreated == null) {
            return null;
        }

        return removeTime(this.lastCreated);
    }

    public void setLastDate(Date lastDate) {
        this.lastCreated = lastDate;
    }

    public boolean isNextDateToday(Date useToday) throws CannotDetermineNextDateException {
        // Is the next date today?
        Date today = removeTime(useToday);
        if(today.equals(getNextDate(today))) {
            return true;
        }

        return false;
    }

    private boolean isDateTodayOrFuture(Date today, Date fromDate) {
        // Is it today?
        if(today.equals(fromDate)) {
            return true;
        }

        if(today.before(fromDate)) {
            return true;
        }

        return false;
    }

    private Date internalNextDate(Date today, Date fromDate) throws CannotDetermineNextDateException {
        while(!isDateTodayOrFuture(today, fromDate)) {
            fromDate = addFrequency(fromDate);
        }

        return fromDate;
    }

    public  Date getNextDate(Date today) throws CannotDetermineNextDateException {
        today = removeTime(today);

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

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
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

    public String getWeekendAdj() {
        return this.weekendAdj;
    }

    public void setWeekendAdj(String weekendAdj) {
        this.weekendAdj = weekendAdj;
    }
}
