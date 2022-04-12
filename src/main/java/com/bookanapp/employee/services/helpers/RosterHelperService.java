package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.DateRangeService;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class RosterHelperService {
    private final RosterService rosterService;
    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final DateRangeService dateRange;
    private final KafkaTemplate<String, Forms.TimeOffRequestNotificationForm> timeOffRequestNotificationTemplate;


    Mono<List<RosterEntity>> getRosterSlotEntities(List<LocalDate> dateRange, List<Employee> employees) {
        return Flux.fromIterable(employees)
                .flatMap(employee -> this.getEmployeeRoster(employee.getEmployeeId(), dateRange, true))
                .collectList()
                .flatMap(lists -> {
                    List<RosterEntity> entities = new ArrayList<>();
                    lists.forEach(entities::addAll);
                    return Mono.just(entities);
                });
    }




    Mono<List<RosterEntity>> getEmployeeRoster(long employeeId, List<LocalDate> dateRange, boolean showTimeOff) {

        if (dateRange.size()<40) {
            return this.commonHelper.getCurrentProviderId()
                    .flatMap(providerId -> this.employeeService.getEmployee(employeeId)
                            .flatMap(employee -> {
                                if (employee.getProviderId() == providerId) {
                                    return getEmployeeRosterEntities(employeeId, dateRange, showTimeOff, employee, false);
                                } else {
                                    return Mono.just(new ArrayList<>());
                                }
                            })
                    );
        } else {
            return Mono.just(new ArrayList<>());
        }

    }

    Mono<List<RosterEntity>> getAnonymousEmployeeRoster(long employeeId, List<LocalDate> dateRange) {

        if (dateRange.size()<40) {
            return this.employeeService.getEmployee(employeeId)
                    .flatMap(employee -> getEmployeeRosterEntities(employeeId, dateRange, false, employee, true));

        } else {
            return Mono.just(new ArrayList<>());
        }

    }

    Mono<? extends List<RosterEntity>> getEmployeeRosterEntities(long employeeId, List<LocalDate> dateRange, boolean showTimeOff, Employee employee, boolean anonymous) {
        return  this.rosterService.getRosterSlotsInInterval(employeeId, dateRange)
                .flatMap(dateSlots -> {
                    List<RosterEntity> entities = new ArrayList<>();
                    dateSlots.forEach(
                            slot -> {
                                if (!showTimeOff) {
                                    if (!anonymous) {
                                        entities.add(new RosterEntity(slot));
                                    } else {
                                        entities.add(new RosterEntity(slot, true, employee));
                                    }

                                } else {
                                    if (slot.isTimeOff())
                                        entities.add(new RosterEntity(slot, "", employee));
                                }
                            }
                    );
                    return Mono.just(entities);
                });
    }

    Mono<List<RosterEntity>> getSubdivisionRoster(long subdivisionId, List<LocalDate> dateRange, Long providerId) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
                                        return this.getSubdivisionRosterEntities(dateRange, subdivisionId);
                                    } else {
                                        return Mono.just(new ArrayList<>());
                                    }
                                }

                        ));

    }

    Mono<List<RosterEntity>> getAnonymousSubdivisionRoster(long subdivisionId, List<LocalDate> dateRange) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> this.getSubdivisionRosterEntities(dateRange, subdivisionId)));

    }

    Mono<List<RosterEntity>> getSubdivisionRosterEntities(List<LocalDate> dateRange, long subdivisionId) {
        return this.rosterService.findSubdivisionRosterSlotsByDateInRange(subdivisionId, dateRange)
                .flatMap(slots -> {
                    List<RosterEntity> entities = new ArrayList<>();
                    slots.forEach(
                            slot -> entities.add(new RosterEntity(slot))
                    );
                    return Mono.just(entities);
                });
    }

    Mono<ResponseEntity> returnSearchedEmployees(long providerId, String term, Long employeeId) {

        return this.employeeService.searchAllEmployeesByName(providerId, term)
                .flatMap(employees -> {
                    if (employees.size()<1) {
                        return this.employeeService.findAllDivisionEmployeesByName(providerId, term)
                                .flatMap(divisionEmployees -> {
                                    if (divisionEmployees.size()>0) {
                                        return this.getSearchedEmployeeEntities(divisionEmployees, employeeId);
                                    } else {
                                        return this.employeeService.findAllSubdivisionEmployeesByName(providerId, term)
                                                .flatMap(subdivisionEmployees -> this.getSearchedEmployeeEntities(subdivisionEmployees, employeeId));
                                    }
                                });
                    } else {
                        return this.getSearchedEmployeeEntities(employees, employeeId);
                    }


                });
    }

    Mono<ResponseEntity> uploadRoster(Forms.RosterForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(Long.parseLong(rosterForm.employeeId))
                        .flatMap(employee -> {
                            if (employee.getProviderId() == providerId) {
                                List<LocalDate> interval = dateRange.dateRange(rosterForm.schedule.startDate, rosterForm.schedule.endDate);
                                List<EmployeeRosterSlot> slots = new ArrayList<>();

                                if (rosterForm.schedule.days.day.size()>0) {
                                } else {
                                    // calculate pattern
                                    if (rosterForm.pattern != null && rosterForm.pattern.length()>2) {
                                        interval = dateRange.rosterPatternDateRange(interval, rosterForm.pattern);
                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                    }
                                }

                                for (LocalDate date : interval) {
                                    if (rosterForm.schedule.days.day.size() > 0) {
                                        for (Forms.RosterDay weekday : rosterForm.schedule.days.day) {
                                            if (date.isAfter(LocalDate.now().minusDays(1)) && date.getDayOfWeek().toString().equals(weekday.weekday)) {
                                                LocalTime initialEndTime = null;
                                                for (Forms.RosterDay.RosterDaySchedule daySchedule : weekday.schedule) {
                                                    LocalTime scheduleStart = LocalTime.of(daySchedule.start.hour, daySchedule.start.minute);
                                                    LocalTime scheduleEnd = LocalTime.of(daySchedule.end.hour, daySchedule.end.minute);

                                                    if (scheduleEnd.isBefore(scheduleStart))
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                                                    if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                                    } else {
                                                        initialEndTime = scheduleEnd;

                                                    }

                                                    this.createNormalOrRangeSlots(employee, null , slots, null, date, scheduleStart, scheduleEnd,
                                                            rosterForm.color, rosterForm.note, rosterForm.publish);
                                                }
                                            }

                                        }

                                    } else {
                                        if (createPatternSlots(employee, slots, date, rosterForm.patternStart, rosterForm.patternEnd, rosterForm.color, rosterForm.note, rosterForm.publish))
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                    }


                                }

                                setMaternityOrSickLeave(slots, rosterForm.sickLeave, rosterForm.maternityLeave);
                                return this.rosterService.saveRosterSlots(slots)
                                        .then(this.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                                        .then(this.saveNewPattern(rosterForm, providerId))
                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                            }
                        }));

    }


    void setMaternityOrSickLeave(List<EmployeeRosterSlot> slots, boolean sickLeave, boolean maternityLeave) {
        slots.forEach(slot -> {
            slot.setSickLeave(sickLeave);
            slot.setMaternityLeave(maternityLeave);
        });
    }



//    Mono<String> sendTimeOffApprovalResponseEmail(Employee employee, boolean approved) {
//        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.providerServiceUrl + "/provider/get/" + employee.getProviderId());
//
//        timeOffRequestNotificationTemplate.send("", )
//
//
//
//        return client.get()
//                .retrieve()
//                .bodyToMono(Provider.class)
//                .flatMap(provider -> {
//                    String url = commonHelper.notificationServiceUrl + "/email/employee/timeoff/approve";
//                    if (!approved)
//                        url = url + "?deny=true";
//                    var emailClient = this.commonHelper.buildAPIAccessWebClient(url);
//                    var form = new Forms.TimeOffRequestNotificationForm(provider, employee, employee.getUsername());
//                    return emailClient.post()
//                            .body(Mono.just(form), Forms.TimeOffRequestNotificationForm.class)
//                            .retrieve()
//                            .bodyToMono(String.class)
//                            .flatMap(message -> Mono.just("success"))
//                            .onErrorResume(e -> Mono.just("success"));
//                });
//
//    }


    Mono<ResponseEntity<Forms.GenericResponse>> publishOrDeleteEmployeeRosterSlots(List<Employee> employees, List<LocalDate> range, boolean delete, boolean unpublish) {
        return Flux.fromIterable(employees)
                .flatMap(employee ->  this.rosterService.getRosterSlotsInInterval(employee.getEmployeeId(), range)
                        .flatMap(slots -> {
                            if (delete) {
                                return this.rosterService.deleteSlots(slots)
                                        .then(Mono.just("success"));
                            } else {
                                slots.forEach(slot -> slot.setPublished(!unpublish));
                                return this.rosterService.saveRosterSlots(slots)
                                        .then(Mono.just("success"));
                            }
                        })

                )
                .collectList()
                .flatMap(this::processMessages);
    }

    Mono<ResponseEntity<Forms.GenericResponse>> publishOrDeleteSubdivisionRosterSlots(Subdivision subdivision, List<LocalDate> range, boolean delete) {
        return  this.rosterService.findSubdivisionRosterSlotsByDateInRange(subdivision.getSubdivisionId(), range)
                .flatMap(slots -> {
                    if (delete) {
                        return this.rosterService.deleteSubdivisionSlots(slots)
                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                    } else {
                        slots.forEach(slot -> slot.setPublished(true));
                        return this.rosterService.saveSubdivisionRosterSlots(slots)
                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                    }
                });
    }


    Mono<ResponseEntity<Forms.GenericResponse>> processMessages(List<String> list){
        if (list.contains("error")) {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("error")));
        } else if (list.contains("invalidSlot")) {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
        } else {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success")));
        }
    }

    Mono<Boolean> validateSlot(long providerId, RosterSlot slot) {
        if (slot instanceof EmployeeRosterSlot)
            return this.employeeService.getEmployee(((EmployeeRosterSlot) slot).getEmployeeId())
                    .flatMap(employee -> Mono.just(employee.getProviderId() == providerId)
                            .flatMap(valid -> {
                                if (!valid) {
                                    return Mono.just(false);
                                } else {
                                    if (employee.getSubdivisionId()!=null) {
                                        return this.employeeService.getSubdivision(employee.getSubdivisionId())
                                                .flatMap(this::checkRosterAuthority);
                                    } else {
                                        return Mono.just(true);
                                    }
                                }
                            })

                    );


        if (slot instanceof SubdivisionRosterSlot)
            return this.employeeService.getSubdivision(((SubdivisionRosterSlot) slot).getSubdivisionId())
                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                            .flatMap(division -> Mono.just(division.getProviderId() == providerId))
                            .flatMap(valid -> {
                                if (!valid) {
                                    return Mono.just(false);
                                } else {
                                   return this.checkRosterAuthority(subdivision);
                                }
                            })
                    );

        return Mono.just(true);

    }

    Mono<List<Employee>> getSubDivisionEmployees(long subdivisionId, long providerId, boolean anonymous) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                                    if (division.getProviderId() == providerId || anonymous) {
                                        return this.employeeService.findEmployeesBySubdivision(subdivisionId);
                                    } else {
                                        return Mono.just(new ArrayList<>());
                                    }
                                }
                        )
                );
    }

    Mono<ResponseEntity> getSearchedEmployeeEntities(List<Employee> employees, Long employeeId) {
        if (employeeId != null)
            employees = employees.stream().filter(employee -> !employee.getEmployeeId().equals(employeeId)).collect(Collectors.toList());

        return Flux.fromIterable(employees)
                .flatMap(employee -> this.loadEmployee(employee.getEmployeeId())
                        .flatMap(this::buildEmployeeEntity))
                .collectList()
                .flatMap(list -> {
                    var sortedEntities = list.stream().sorted(Comparator.comparing(EmployeeEntity::getName))
                            .collect(Collectors.toList());
                    return Mono.just(ResponseEntity.ok(sortedEntities));
                });
    }

    Mono<Employee> loadEmployee(long employeeId) {
        return this.employeeService.getEmployee(employeeId)
                .flatMap(this::setEmployeeSubdivision);
    }

    Mono<? extends Employee> setEmployeeSubdivision(Employee employee) {
            if (employee.getSubdivisionId() != null) {
                return this.employeeService.getSubdivision(employee.getSubdivisionId())
                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> {
                                    subdivision.setDivision(division);
                                    employee.setSubdivision(subdivision);
                                    return Mono.just(employee);
                                }))
                        .switchIfEmpty(Mono.defer(() -> Mono.just(employee)));
            } else {
                return Mono.just(employee);
            }
    }


    Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {
        return  Mono.just(EmployeeEntity.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .username(employee.getUsername())
                .avatar(employee.getAvatar())
                .subdivision(employee.getSubdivision() != null ? employee.getSubdivision().getName() : null)
                .division(employee.getSubdivision() != null ? employee.getSubdivision().getDivision().getName() : null)
                .subdivisionId(employee.getSubdivisionId())
                .divisionId(employee.getSubdivision() != null && employee.getSubdivision().getDivision() != null ? employee.getSubdivision().getDivision().getDivisionId() : null)
                .jobTitle(employee.getJobTitle())
                .build());
    }

    /**
     * Creates roster slots for either employee or subdivision
     * @param subdivision subdivision
     * @param slots list of roster slots
     * @param subdivisionSlots list of subdivision slots
     * @param date date for slots
     */
    void createNormalOrRangeSlots(Employee employee, Subdivision subdivision, List<EmployeeRosterSlot> slots, List<SubdivisionRosterSlot> subdivisionSlots, LocalDate date,
                                          LocalTime start, LocalTime end, String color, String note, boolean publish) {
        RosterSlot slot;
        if (employee != null) {
            slot = new EmployeeRosterSlot(date, start, end);
            ((EmployeeRosterSlot) slot).setEmployeeId(employee.getEmployeeId());
        } else {
            slot = new SubdivisionRosterSlot(date, start, end);
            ((SubdivisionRosterSlot) slot).setSubdivisionId(subdivision.getSubdivisionId());
        }
        if (color != null)
            slot.setColor(color);

        if (note != null)
            slot.setNote(note);

        if (publish)
            slot.setPublished(true);

        if (employee != null) {
            slots.add((EmployeeRosterSlot) slot);
        } else {
            subdivisionSlots.add((SubdivisionRosterSlot) slot);
        }
    }

    boolean createPatternSlots(Employee employee, List<EmployeeRosterSlot> slots, LocalDate date, Forms.RosterDay.RosterDaySchedule.RosterDayScheduleHour patternStart,
                                       Forms.RosterDay.RosterDaySchedule.RosterDayScheduleHour patternEnd, String color, String note, boolean publish) {
        LocalTime initialEndTime = null;

        LocalTime scheduleStart = LocalTime.of(patternStart.hour, patternStart.minute);
        LocalTime scheduleEnd =  LocalTime.of(patternEnd.hour, patternEnd.minute);

        if (scheduleEnd.isBefore(scheduleStart))
            return true;

        if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
            return true;
        } else {
            initialEndTime = scheduleEnd;

        }
        this.createNormalOrRangeSlots(employee, null , slots, null, date, scheduleStart, scheduleEnd,
                color, note, publish);
        return false;
    }


    void createSubdivisionRosterSlots(Forms.SubdivisionRosterForm rosterForm, Provider provider, Employee employee, List<EmployeeRosterSlot> slots, List<SubdivisionRosterSlot> subdivisionSlots, Subdivision subdivision, String start, String end, LocalDate date, LocalTime scheduleStart, LocalTime scheduleEnd) {
        long durationInMinutes = scheduleStart.until(scheduleEnd, ChronoUnit.MINUTES);
        String duration = LocalTime.MIN.plus(Duration.ofMinutes(durationInMinutes)).toString();
        List<LocalTime> hours = dateRange.addHour(start, end, duration, "1", provider);
//        this.createNormalOrRangeSlots(employee, subdivision, slots, subdivisionSlots, date, durationInMinutes, hours, rosterForm.color, rosterForm.note, rosterForm.publish);

    }

    Mono<String> saveNewPattern(Forms.RosterForm rosterForm, long providerId){
        if (rosterForm.patternName != null && rosterForm.patternName.length()>1) {

            String start;
            String end;

            start = dateRange.addZeroBeforeInt(rosterForm.patternStart.hour) + ":" + dateRange.addZeroBeforeInt(rosterForm.patternStart.minute);
            end = dateRange.addZeroBeforeInt(rosterForm.patternEnd.hour) + ":" + dateRange.addZeroBeforeInt(rosterForm.patternEnd.minute);


            LocalTime scheduleStart = LocalTime.parse(start, DateTimeFormatter.ofPattern("H:mm"));
            LocalTime scheduleEnd = LocalTime.parse(end, DateTimeFormatter.ofPattern("H:mm"));

            RosterPattern pattern = new RosterPattern(providerId, rosterForm.patternName, rosterForm.pattern, scheduleStart, scheduleEnd);

            return this.rosterService.getPatterns(providerId)
                    .flatMap(rosterPatterns -> {
                        if (rosterPatterns.contains(pattern)) {
                            return Mono.just("ok");
                        } else {
                            return this.rosterService.savePattern(pattern)
                                    .then(Mono.just("ok"));
                        }
                    });

        } else {
            return Mono.just("ok");
        }
    }

    Mono<String> saveNewColor(String colorName, String color, long providerId){
        if (colorName != null) {
            RosterSlotColor rosterSlotColor = new RosterSlotColor(providerId, colorName, color);
            return this.rosterService.getColors(providerId)
                    .flatMap(rosterSlotColors -> {
                        if (rosterSlotColors.contains(rosterSlotColor)) {
                            return Mono.just("ok");
                        } else {
                            return this.rosterService.saveColor(rosterSlotColor)
                                    .then(Mono.just("ok"));
                        }
                    });
        } else {
            return Mono.just("ok");
        }

    }

    Mono<List<Employee>> getEmployees(Long employeeId, Long subdivisionId, Long divisionId, long providerId, boolean anonymous) {
        if (employeeId != null && employeeId != 0) {
            return this.employeeService.getEmployee(employeeId)
                    .flatMap(employee -> {
                        List<Employee> employees = new ArrayList<>();
                        employees.add(employee);
                        return Mono.just(employees);
                    });
        } else if (subdivisionId != null && subdivisionId != 0){
            return getSubDivisionEmployees(subdivisionId, providerId, anonymous);

        } else if (divisionId != null && divisionId != 0){
            return this.employeeService.getDivision(divisionId)
                    .flatMap(division -> {
                                if (division.getProviderId() == providerId || anonymous) {
                                    return this.employeeService.getSubdivisions(divisionId)
                                            .flatMap(this.employeeService::findAllSubdivisionEmployees);
                                } else {
                                    return Mono.just(new ArrayList<>());
                                }
                            }
                    );
        } else {
            return this.employeeService.getAllEmployees(providerId);
        }
    }

    Mono<ResponseEntity<List<EmployeeEntity>>> getEmployeeEntities(List<Employee> employees, boolean anonymous) {
        return Flux.fromIterable(employees.stream().sorted(Comparator.comparing(Employee::getName)).collect(Collectors.toList()))
                .flatMap(employee -> {
                    var entity = new EmployeeEntity(employee.getEmployeeId(), !anonymous ? employee.getName() : this.getInitials(employee.getName()));
                    if (employee.getSubdivisionId() != null) {
                        return this.employeeService.getSubdivision(employee.getSubdivisionId())
                                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                        .flatMap(division -> {
                                            entity.setSubdivisionId(subdivision.getSubdivisionId());
                                            entity.setSubdivision(subdivision.getName());
                                            entity.setDivisionId(division.getDivisionId());
                                            entity.setDivision(division.getName());
                                            return Mono.just(entity);
                                        }));
                    } else {
                        return Mono.just(entity);
                    }
                })
                .collectList()
                .flatMap(list -> Mono.just(ResponseEntity.ok(list)));
    }

    Mono<Subdivision> checkSubdivisionId(long subdivisionId, long providerId) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                            if (division.getProviderId() == providerId) {
                                return Mono.just(subdivision);
                            } else {
                                return Mono.just(new Subdivision());
                            }
                        }));
    }

    Mono<Boolean> checkRosterAuthority(Subdivision subdivision) {
        return this.commonHelper.getCurrentUser()
                .flatMap(userDetails -> {
                    if (userDetails instanceof ProviderDetails) {
                        return Mono.just(true);
                    } else if (userDetails instanceof EmployeeDetails) {
                        return this.employeeService.getAuthorizedRosters(((EmployeeDetails) userDetails).getId())
                                .switchIfEmpty(Mono.just(new ArrayList<>()))
                                .flatMap(rosterList -> {
                                    if (rosterList.size()==0) {
                                        return Mono.just(true);
                                    } else {
                                        var idList = rosterList.stream()
                                                .map(AuthorizedRoster::getRosterId)
                                                .collect(Collectors.toList());
                                        return Mono.just(idList.contains(subdivision.getSubdivisionId()));
                                    }
                                });

                    } else {
                        return Mono.just(false);
                    }
                });
    }

    Mono<ResponseEntity> uploadRange(Employee employee, long providerId, Forms.RosterRangeForm rosterForm) {
        if (employee.getProviderId() == providerId) {
            List<LocalDate> interval = rosterForm.dates;
            List<EmployeeRosterSlot> slots = new ArrayList<>();

            for (LocalDate date : interval) {
                if (this.createPatternSlots(employee, slots, date, rosterForm.patternStart, rosterForm.patternEnd, rosterForm.color, rosterForm.note, rosterForm.publish))
                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
            }

            this.setMaternityOrSickLeave(slots, rosterForm.sickLeave, rosterForm.maternityLeave);

            return this.rosterService.saveRosterSlots(slots)
                    .then(this.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                    .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

        } else {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

        }
    }

    Mono<ResponseEntity> uploadRangeDivision(Subdivision subdivision, Forms.SubdivisionRosterRangeForm rosterForm, long providerId) {
        List<LocalDate> interval = rosterForm.dates;
        List<SubdivisionRosterSlot> slots = new ArrayList<>();

        for (LocalDate date : interval) {
            LocalTime initialEndTime = null;
            LocalTime scheduleStart = LocalTime.of(rosterForm.patternStart.hour, rosterForm.patternStart.minute);
            LocalTime scheduleEnd =  LocalTime.of(rosterForm.patternEnd.hour, rosterForm.patternEnd.minute);

            if (scheduleEnd.isBefore(scheduleStart))
                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

            if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
            } else {
                initialEndTime = scheduleEnd;

            }
            this.createNormalOrRangeSlots(null, subdivision , null, slots, date, scheduleStart, scheduleEnd,
                    rosterForm.color, rosterForm.note, rosterForm.publish);
        }

        return this.rosterService.saveSubdivisionRosterSlots(slots)
                .then(this.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

    }

    Mono<ResponseEntity> uploadSubdivisionRoster(Subdivision subdivision, Forms.SubdivisionRosterForm rosterForm, long providerId) {

            List<LocalDate> interval = dateRange.dateRange(rosterForm.schedule.startDate, rosterForm.schedule.endDate);
            List<SubdivisionRosterSlot> slots = new ArrayList<>();

            if (rosterForm.schedule.days.day.size()>0) {

            } else {
                // calculate pattern
                if (rosterForm.pattern != null && rosterForm.pattern.length()>2) {
                    interval = dateRange.rosterPatternDateRange(interval, rosterForm.pattern);

                } else {
                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                }

            }



            for (LocalDate date : interval) {
                if (rosterForm.schedule.days.day.size() > 0) {
                    for (Forms.RosterDay weekday : rosterForm.schedule.days.day) {
                        if (date.isAfter(LocalDate.now().minusDays(1)) && date.getDayOfWeek().toString().equals(weekday.weekday)) {
                            LocalTime initialEndTime = null;
                            for (Forms.RosterDay.RosterDaySchedule daySchedule : weekday.schedule) {

                                LocalTime scheduleStart = LocalTime.of(daySchedule.start.hour, daySchedule.start.minute);
                                LocalTime scheduleEnd = LocalTime.of(daySchedule.end.hour, daySchedule.end.minute);

                                if (scheduleEnd.isBefore(scheduleStart))
                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                                if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                } else {
                                    initialEndTime = scheduleEnd;

                                }

                                this.createNormalOrRangeSlots(null, subdivision , null, slots, date, scheduleStart, scheduleEnd,
                                        rosterForm.color, rosterForm.note, rosterForm.publish);
                            }
                        }

                    }

                } else {
                    LocalTime initialEndTime = null;

                    LocalTime scheduleStart = LocalTime.of(rosterForm.patternStart.hour, rosterForm.patternStart.minute);
                    LocalTime scheduleEnd =  LocalTime.of(rosterForm.patternEnd.hour, rosterForm.patternEnd.minute);

                    if (scheduleEnd.isBefore(scheduleStart))
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                    if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                    } else {
                        initialEndTime = scheduleEnd;

                    }
                    this.createNormalOrRangeSlots(null, subdivision , null, slots, date, scheduleStart, scheduleEnd,
                            rosterForm.color, rosterForm.note, rosterForm.publish);
                }


            }

            return this.rosterService.saveSubdivisionRosterSlots(slots)
                    .then(this.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                    .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));


    }



    String getInitials(String name) {
        String[] words = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for(String word : words) {
            builder.append(word.charAt(0));
        }
        return builder.toString();
    }

}
