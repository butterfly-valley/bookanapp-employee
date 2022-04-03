package com.bookanapp.employee.entities.rest;

import com.bookanapp.employee.entities.TimeRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
public class TimeRequestEntity {

    public TimeRequestEntity(TimeRequest request) {
        this.id = request.getId();
        this.approved = request.isApproved();
        this.toBeApproved = request.isToBeApproved();
        this.overtime = request.isOvertime();
        this.comments = request.getComments();
        this.start = request.getStart();
        this.end = request.getEnd();

    }

    private String id;
    private boolean approved;
    private boolean toBeApproved;
    private boolean overtime;
    private String comments;
    private LocalTime start;
    private LocalTime end;
    private List<String> attachments;

}
