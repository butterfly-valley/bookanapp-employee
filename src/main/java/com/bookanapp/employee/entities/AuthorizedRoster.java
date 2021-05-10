package com.bookanapp.employee.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.Objects;


@Data
@NoArgsConstructor
public class AuthorizedRoster {

//    @Id
//    private long id;
    private long employeeId ;
    private long rosterId;

    public AuthorizedRoster(long employeeId, long scheduleId) {
        this.employeeId = employeeId;
        this.rosterId = rosterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizedRoster that = (AuthorizedRoster) o;
        return rosterId == that.rosterId &&
                Objects.equals(employeeId, that.employeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, rosterId);
    }
}
