package com.bookanapp.employee.entities;

import lombok.Data;


import java.time.LocalDate;
import java.time.LocalTime;



@Data
public class EmployeeRosterSlot extends RosterSlot {

    public EmployeeRosterSlot(LocalDate date, LocalTime start, LocalTime end) {
        super(date, start, end);
    }

    public EmployeeRosterSlot() {
        super();
    }

    private long employeeId;

    private boolean timeOff = false;
    private boolean halfDayOff = false;
    private boolean timeOffApproved = false;
    private boolean timeOffDenied = false;
    private TimeOffBalanceType balanceType;

    public enum TimeOffBalanceType{
        VACS,
        VACSROLLOVER,
        BANK,
        BANKROLLOVER,
        COMP,
        COMPROLLOVER
    }




}
