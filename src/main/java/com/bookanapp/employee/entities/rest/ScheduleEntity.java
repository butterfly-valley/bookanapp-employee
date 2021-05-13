package com.bookanapp.employee.entities.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ScheduleEntity {
    private String scheduleId;
    private String scheduleName;
    private boolean visible;
    private boolean main;
    private boolean mandatoryPhone;
    private String scheduleCategory;
    private boolean smsReminder;
    private String minimumNotice;
    private String avatar;
    private List<ServiceEvent> serviceList = new ArrayList<>();
    private boolean notif;
    private String notifEmail;
    private LocalDate start;
    private LocalDate end;
    private boolean service;
    private String serviceSchedule;
    private boolean freeSchedule;
    private boolean noDuration;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ServiceEvent {

    private String service;
    private String duration;

}

