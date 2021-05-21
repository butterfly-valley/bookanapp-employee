package com.bookanapp.employee.entities;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RosterSlot {

    public RosterSlot(LocalDate date, LocalTime start, LocalTime end) {
        this.date = date;
        this.start = start;
        this.end = end;
    }
    @Id
    private Long slotId;

    private LocalDate date;
    private LocalTime start;
    private LocalTime end;
    private String note;
    private String color;
    private boolean published;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RosterSlot slot = (RosterSlot) o;
        return slotId == slot.slotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId);
    }
}
