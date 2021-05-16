package com.bookanapp.employee.entities;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDate;
import java.time.LocalTime;



@Data
@Table("roster_slot")
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
    private int balanceType;

    public enum TimeOffBalanceType{
        VACS(0),
        VACSROLLOVER(1),
        BANK(2),
        BANKROLLOVER(3),
        COMP(4),
        COMPROLLOVER(5);

        private final int type;

        private TimeOffBalanceType(int type) {
            this.type = type;
        }

        public int getHierarchy() {
            return type;
        }
    }




}
