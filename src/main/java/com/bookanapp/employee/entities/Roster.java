package com.bookanapp.employee.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Roster {

    public Roster(long employeeId, DayOfWeek weekday, LocalDate start, LocalDate end, List<EmployeeRosterSlot> slots) {
        this.employeeId = employeeId;
        this.weekday = weekday;
        this.start = start;
        this.end = end;
        this.slots = slots;
    }

    @Id
    private long weekdayId;

    private long employeeId;

    private DayOfWeek weekday;

    private LocalDate start;
    private LocalDate end;


    @Transient
    private List<EmployeeRosterSlot> slots;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Roster that = (Roster) o;
        return Objects.equals(employeeId, that.employeeId) && weekday == that.weekday;
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, weekday);
    }

}
