package com.bookanapp.employee.entities.rest;

import com.bookanapp.employee.entities.AbsenceRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class TimeRequestEntity {

    public TimeRequestEntity(AbsenceRequest request) {
        this.id = request.getId();
        this.approved = request.isApproved();
        this.toBeApproved = request.isToBeApproved();
        this.overtime = request.isOvertime();
        this.comments = request.getComments();
        this.start = LocalDateTime.of(request.getDate(), request.getStart());
        this.end = LocalDateTime.of(request.getDate(), request.getEnd());

    }

    private String id;
    private boolean approved;
    private boolean toBeApproved;
    private boolean overtime;
    private String comments;
    private LocalDateTime start;
    private LocalDateTime end;
    private List<String> attachments;

}
