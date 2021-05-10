package com.bookanapp.employee.entities;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SubdivisionRosterSlot extends RosterSlot {
    private long subdivisionId;

    public SubdivisionRosterSlot(LocalDate date, LocalTime start, LocalTime end) {
        super(date, start, end);
    }

    public SubdivisionRosterSlot() {
        super();
    }

}
