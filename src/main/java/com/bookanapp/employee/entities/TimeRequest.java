package com.bookanapp.employee.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeRequest implements Persistable<String> {

    @Id
    private String id;
    private long employeeId;
//    private LocalDate date;
    private String dates;
    private LocalTime start;
    private LocalTime end;
    private boolean approved;
    private boolean toBeApproved = true;
    private boolean overtime;
    private String comments;
    private long minutes;

    @Transient
    private boolean newRequest;

    @Override
    @Transient
    public boolean isNew() {
        return this.newRequest || id == null;
    }
}
