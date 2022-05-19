package com.bookanapp.employee.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDate;
import java.time.LocalTime;



@Data
@Table("roster_slot")
@AllArgsConstructor
public class EmployeeRosterSlot extends RosterSlot {
    private long employeeId;
    private boolean timeOff = false;
    private boolean halfDayOff = false;
    private boolean timeOffApproved = false;
    private boolean timeOffDenied = false;
    private int balanceType;
    private boolean sickLeave = false;
    private boolean maternityLeave = false;

    public EmployeeRosterSlot(LocalDate date, LocalTime start, LocalTime end) {
        super(date, start, end);
    }

    public EmployeeRosterSlot() {
        super();
    }


    public enum TimeOffBalanceType{
        VACS,
        VACSROLLOVER,
        BANK,
        BANKROLLOVER,
        COMP,
        COMPROLLOVER;

    }




}
