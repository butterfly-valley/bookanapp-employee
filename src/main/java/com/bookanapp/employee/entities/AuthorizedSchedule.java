package com.bookanapp.employee.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Objects;


@Data
@NoArgsConstructor
public class AuthorizedSchedule {

    @Id
    private long id;
    private long employeeId ;
    private long scheduleId;

    public AuthorizedSchedule(long employeeId, long scheduleId) {
        this.employeeId = employeeId;
        this.scheduleId = scheduleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizedSchedule that = (AuthorizedSchedule) o;
        return scheduleId == that.scheduleId &&
                Objects.equals(employeeId, that.employeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, scheduleId);
    }
}
