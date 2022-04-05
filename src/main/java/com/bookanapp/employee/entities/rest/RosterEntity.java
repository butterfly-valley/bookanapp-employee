package com.bookanapp.employee.entities.rest;

import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.EmployeeRosterSlot;
import com.bookanapp.employee.entities.SubdivisionRosterSlot;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RosterEntity {
    private long employeeId;
    private long subdivisionId;
    private long slotId;
    private LocalDateTime start;
    private LocalDateTime end;
    private String name;
    private String note;
    private String slotColor;
    private boolean published;
    private boolean approved;
    private boolean timeOff;
    private boolean halfDay;
    private boolean denied;
    private boolean sickLeave;
    private boolean maternityLeave;

    public RosterEntity(EmployeeRosterSlot slot) {
        this.employeeId = slot.getEmployeeId();
        this.slotId = slot.getSlotId();
        this.start = LocalDateTime.of(slot.getDate(), slot.getStart());
        this.end = LocalDateTime.of(slot.getDate(), slot.getEnd());
        this.slotColor = slot.getColor();
        this.note = slot.getNote();
        this.approved = slot.isTimeOffApproved();
        this.timeOff = slot.isTimeOff();
        this.halfDay = slot.isHalfDayOff();
        this.published = slot.isPublished();
        this.denied = slot.isTimeOffDenied();
        this.sickLeave = slot.isSickLeave();
        this.maternityLeave = slot.isMaternityLeave();
    }

    public RosterEntity(EmployeeRosterSlot slot, String name, Employee employee) {
        this.employeeId = slot.getEmployeeId();
        this.slotId = slot.getSlotId();
        this.start = LocalDateTime.of(slot.getDate(), slot.getStart());
        this.end = LocalDateTime.of(slot.getDate(), slot.getEnd());
        this.slotColor = slot.getColor();
        this.note = slot.getNote();
        this.approved = slot.isTimeOffApproved();
        this.timeOff = slot.isTimeOff();
        this.halfDay = slot.isHalfDayOff();
        this.published = slot.isPublished();
        this.denied = slot.isTimeOffDenied();
        this.name = employee.getName();
        this.sickLeave = slot.isSickLeave();
        this.maternityLeave = slot.isMaternityLeave();
    }



    public RosterEntity(SubdivisionRosterSlot slot) {
        this.subdivisionId = slot.getSubdivisionId();
        this.slotId = slot.getSlotId();
        this.start = LocalDateTime.of(slot.getDate(), slot.getStart());
        this.end = LocalDateTime.of(slot.getDate(), slot.getEnd());
        this.slotColor = slot.getColor();
        this.note = slot.getNote();
        this.published = slot.isPublished();
    }

    public RosterEntity(EmployeeRosterSlot slot, boolean anonymous, Employee employee) {
        this.employeeId = getEmployeeId();
        this.slotId = slot.getSlotId();
        this.start = LocalDateTime.of(slot.getDate(), slot.getStart());
        this.end = LocalDateTime.of(slot.getDate(), slot.getEnd());
        this.name = getInitials(employee.getName());
        this.slotColor = slot.getColor();
        this.note = slot.getNote();
        this.published = slot.isPublished();
        this.timeOff = slot.isTimeOff();
        this.approved = slot.isTimeOffApproved();
        this.denied = slot.isTimeOffDenied();
        this.sickLeave = slot.isSickLeave();
        this.maternityLeave = slot.isMaternityLeave();
    }

    private String getInitials(String name) {
        String[] words = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for(String word : words) {
            builder.append(word.charAt(0));
        }
        return builder.toString();
    }
}
