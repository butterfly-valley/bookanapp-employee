package com.bookanapp.employee.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeOffRequest {

    @Id
    private long id;
    private long employeeId;

    private LocalDate date;
    private LocalTime start;
    private LocalTime end;

    private boolean approved;
    private boolean toBeApproved = true;
    private boolean overtime;
    private String comments;
}
