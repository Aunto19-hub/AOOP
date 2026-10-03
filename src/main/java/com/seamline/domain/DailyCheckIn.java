package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/** One employee's "I'm here and at my station" tap for one calendar day. */
@Entity
@Table(name = "daily_checkins", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "check_date"}))
public class DailyCheckIn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    protected DailyCheckIn() {
        // required by JPA
    }

    public DailyCheckIn(Employee employee, LocalDate checkDate) {
        this.employee = employee;
        this.checkDate = checkDate;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getCheckDate() {
        return checkDate;
    }
}
