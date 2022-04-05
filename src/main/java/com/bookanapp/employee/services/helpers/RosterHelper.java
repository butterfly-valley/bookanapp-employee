package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.RosterEntity;
import com.bookanapp.employee.services.DateRangeService;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RosterHelper {
    private final RosterService rosterService;
    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final DateRangeService dateRange;
    private final RosterHelperService rosterHelperService;

    public Mono<ResponseEntity> displaySharedEmployees(Long employeeId, Long subdivisionId, Long divisionId) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.rosterHelperService.getEmployees(employeeId, subdivisionId, divisionId, providerId, false)
                        .flatMap(employees -> this.rosterHelperService.getEmployeeEntities(employees, false)));
    }

    public Mono<ResponseEntity> getAllEmployees() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getAllEmployees)
                .flatMap(employees -> this.rosterHelperService.getEmployeeEntities(employees, false));

    }

    public Mono<ResponseEntity> displaySharedEmployeesAnonymously(Long employeeId, Long subdivisionId, Long divisionId) {
        return this.rosterHelperService.getEmployees(employeeId, subdivisionId, divisionId, 0, true)
                .flatMap(employees -> this.rosterHelperService.getEmployeeEntities(employees, true));
    }

    public Mono<ResponseEntity> displayRoster(String start, String end, String offset, Long employeeId, String subdivisionId, String divisionId, String all, String showTimeOff) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId ->  {
                    List<LocalDate> dateRange = this.dateRange.dateRangeUTC(start, end, offset);
                    boolean timeOff = showTimeOff != null;

                    Mono<List<RosterEntity>> slotEntities = null;

                    if (employeeId != null)
                        slotEntities = this.rosterHelperService.getEmployeeRoster(employeeId, dateRange, timeOff);

                    if (subdivisionId != null && !subdivisionId.equals("null")) {
                        if (!timeOff) {
                            slotEntities = this.rosterHelperService.getSubdivisionRoster(Long.parseLong(subdivisionId), dateRange, providerId);
                        } else {
                            slotEntities = this.rosterHelperService.getSubDivisionEmployees(Long.parseLong(subdivisionId), providerId, false)
                                    .flatMap(employees -> rosterHelperService.getRosterSlotEntities(dateRange, employees));

                        }
                    }
                    if (divisionId != null && !divisionId.equals("null")) {
                        slotEntities = this.employeeService.getDivision(Long.parseLong(divisionId))
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
                                        return this.employeeService.getSubdivisions(division.getDivisionId())
                                                .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                                        .flatMap(subdivision -> this.rosterHelperService.getSubDivisionEmployees(subdivision.getSubdivisionId(), providerId, false)
                                                                .flatMap(employees -> Flux.fromIterable(employees)
                                                                        .flatMap(employee -> this.rosterHelperService.getEmployeeRoster(employee.getEmployeeId(), dateRange, timeOff))
                                                                        .collectList()
                                                                )
                                                        )
                                                        .collectList()
                                                        .flatMap(lists -> {
                                                            List<RosterEntity> entities = new ArrayList<>();
                                                            lists.forEach(subLists -> subLists.forEach(entities::addAll));
                                                            return Mono.just(entities);
                                                        }));

                                    } else {
                                        return Mono.just(new ArrayList<>());
                                    }
                                });
                    }

                    if (all != null){
                        slotEntities = this.employeeService.getAllEmployees(providerId)
                                .flatMap(employees -> rosterHelperService.getRosterSlotEntities(dateRange, employees));
                    }

                    if (slotEntities != null) {
                        return Mono.just(ResponseEntity.ok(slotEntities));
                    } else {
                        return Mono.just(ResponseEntity.ok(new ArrayList<>()));
                    }
                });
    }



    public Mono<ResponseEntity> displayAnonymousRoster(String start, String end, String offset, Long employeeId, String subdivisionId, String divisionId) {
        List<LocalDate> dateRange = this.dateRange.dateRangeUTC(start, end, offset);

        Mono<List<RosterEntity>> slotEntities = null;

        if (employeeId != null)
            slotEntities = this.rosterHelperService.getAnonymousEmployeeRoster(employeeId, dateRange);

        if (subdivisionId != null && !subdivisionId.equals("null"))
            slotEntities = this.rosterHelperService.getAnonymousSubdivisionRoster(Long.parseLong(subdivisionId), dateRange);

        if (divisionId != null && !divisionId.equals("null")) {
            slotEntities = this.employeeService.getDivision(Long.parseLong(divisionId))
                    .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                            .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                    .flatMap(subdivision -> this.rosterHelperService.getSubDivisionEmployees(subdivision.getSubdivisionId(), 0, true)
                                            .flatMap(employees -> Flux.fromIterable(employees)
                                                    .flatMap(employee -> this.rosterHelperService.getAnonymousEmployeeRoster(employee.getEmployeeId(), dateRange))
                                                    .collectList()
                                            )
                                    )
                                    .collectList()
                                    .flatMap(lists -> {
                                        List<RosterEntity> entities = new ArrayList<>();
                                        lists.forEach(subLists -> subLists.forEach(entities::addAll));
                                        return Mono.just(entities);
                                    })));
        }

        return Mono.just(ResponseEntity.ok(slotEntities));

    }


    public Mono<ResponseEntity> findEmployeeByName(String term) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.commonHelper.getCurrentEmployee()
                        .flatMap(employee -> this.rosterHelperService.returnSearchedEmployees(providerId, term, employee.getEmployeeId()))
                        .switchIfEmpty(this.rosterHelperService.returnSearchedEmployees(providerId, term, null)));
    }


    public Mono<ResponseEntity> uploadRoster(Forms.RosterForm rosterForm) {
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

                                                    this.rosterHelperService.createNormalOrRangeSlots(employee, null , slots, null, date, scheduleStart, scheduleEnd,
                                                            rosterForm.color, rosterForm.note, rosterForm.publish);
                                                }
                                            }

                                        }

                                    } else {
                                        if (rosterHelperService.createPatternSlots(employee, slots, date, rosterForm.patternStart, rosterForm.patternEnd, rosterForm.color, rosterForm.note, rosterForm.publish))
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                    }


                                }

                                this.rosterHelperService.setMaternityOrSickLeave(slots, rosterForm.sickLeave, rosterForm.maternityLeave);
                                return this.rosterService.saveRosterSlots(slots)
                                        .then(this.rosterHelperService.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                                        .then(this.rosterHelperService.saveNewPattern(rosterForm, providerId))
                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                            }
                        }));

    }


    public Mono<ResponseEntity> uploadRangeRoster(Forms.RosterRangeForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(Long.parseLong(rosterForm.employeeId))
                        .flatMap(employee -> {
                            if (employee.getProviderId() == providerId) {
                                List<LocalDate> interval = rosterForm.dates;
                                List<EmployeeRosterSlot> slots = new ArrayList<>();

                                for (LocalDate date : interval) {
                                    if (this.rosterHelperService.createPatternSlots(employee, slots, date, rosterForm.patternStart, rosterForm.patternEnd, rosterForm.color, rosterForm.note, rosterForm.publish))
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                }

                                this.rosterHelperService.setMaternityOrSickLeave(slots, rosterForm.sickLeave, rosterForm.maternityLeave);

                                return this.rosterService.saveRosterSlots(slots)
                                        .then(this.rosterHelperService.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                            }
                        }));

    }

    public Mono<ResponseEntity> uploadRangeRosterDivision(Forms.SubdivisionRosterRangeForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getSubdivision(Long.parseLong(rosterForm.subdivisionId))
                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
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
                                            this.rosterHelperService.createNormalOrRangeSlots(null, subdivision , null, slots, date, scheduleStart, scheduleEnd,
                                                    rosterForm.color, rosterForm.note, rosterForm.publish);
                                        }

                                        return this.rosterService.saveSubdivisionRosterSlots(slots)
                                                .then(this.rosterHelperService.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSubdivision")));

                                    }
                                })

                        ));

    }


    public Mono<ResponseEntity> uploadSubdivisionRoster(Forms.SubdivisionRosterForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getSubdivision(rosterForm.subdivisionId)
                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
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

                                                            this.rosterHelperService.createNormalOrRangeSlots(null, subdivision , null, slots, date, scheduleStart, scheduleEnd,
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
                                                this.rosterHelperService.createNormalOrRangeSlots(null, subdivision , null, slots, date, scheduleStart, scheduleEnd,
                                                        rosterForm.color, rosterForm.note, rosterForm.publish);
                                            }


                                        }

                                        return this.rosterService.saveSubdivisionRosterSlots(slots)
                                                .then(this.rosterHelperService.saveNewColor(rosterForm.colorName, rosterForm.color, providerId))
                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                                    }
                                })));

    }



    public Mono<ResponseEntity> getRosterPatternsAndColors(){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.rosterService.getColors(providerId)
                        .flatMap(colors -> this.rosterService.getPatterns(providerId)
                                .flatMap(rosterPatterns -> {
                                    List<Object> patternsAndColors = new ArrayList<>();
                                    patternsAndColors.add(rosterPatterns);
                                    patternsAndColors.add(colors);
                                    return Mono.just(ResponseEntity.ok(patternsAndColors));
                                })
                        )
                );
    }

    public Mono<ResponseEntity> updateRosterSlot(Forms.RosterSlotForm slotForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(slotForm.slotDetails)
                        .flatMap(rosterSlotDetailsForm -> {
                            Mono<? extends RosterSlot> slotMono = null;
                            if (rosterSlotDetailsForm.employeeId != null && !rosterSlotDetailsForm.employeeId.equals("0"))
                                slotMono = this.rosterService.findSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (rosterSlotDetailsForm.subdivisionId != null && !rosterSlotDetailsForm.subdivisionId.equals("0"))
                                slotMono = this.rosterService.findSubdivisionRosterSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (slotForm == null)
                                return Mono.just("invalidSlot");

                            assert slotMono != null;
                            return slotMono
                                    .flatMap(slot -> this.rosterHelperService.returnInvalidSlotMessage(providerId, slot)
                                            .flatMap(invalid -> {
                                                if (invalid) {
                                                    return Mono.just("invalidSlot");
                                                } else {
                                                    if (slotForm.start != null) {
                                                        LocalTime startTime = LocalTime.of(slotForm.start.hour, slotForm.start.minute);
                                                        slot.setStart(startTime);
                                                    }

                                                    if (slotForm.end != null) {
                                                        LocalTime endTime = LocalTime.of(slotForm.end.hour, slotForm.end.minute);
                                                        slot.setEnd(endTime);

                                                    }

                                                    if (slotForm.note != null)
                                                        slot.setNote(slotForm.note);

                                                    if (slotForm.color != null)
                                                        slot.setColor(slotForm.color);

                                                    if (slot instanceof EmployeeRosterSlot) {
                                                        return this.rosterService.saveRosterSlot((EmployeeRosterSlot) slot)
                                                                .then(Mono.just("success"));

                                                    } else if  (slot instanceof SubdivisionRosterSlot) {
                                                        return this.rosterService.saveSubdivisionRosterSlot((SubdivisionRosterSlot) slot)
                                                                .then(Mono.just("success"));
                                                    } else {
                                                        return Mono.just("error");
                                                    }


                                                }
                                            })

                                    );

                        })
                        .collectList()
                        .flatMap(this.rosterHelperService::processMessages)

                );
    }

    public Mono<ResponseEntity> pasteRosterSlot(Forms.PasteRosterSlotForm slotForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                    Mono<? extends RosterSlot> slotMono = this.rosterService.findSlot(slotForm.slotId).switchIfEmpty(Mono.just(new EmployeeRosterSlot()))
                            .flatMap(slot -> {
                                if (slot.getSlotId() == null) {
                                    return this.rosterService.findSubdivisionRosterSlot(slotForm.slotId);
                                } else {
                                    return Mono.just(slot);
                                }
                            });
                    return slotMono
                            .flatMap(slot -> this.rosterHelperService.returnInvalidSlotMessage(providerId, slot)
                                    .flatMap(invalid -> {
                                        if (invalid) {
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                        } else {
                                            if (slotForm.employeeId != 0) {
                                                return this.employeeService.getEmployee(slotForm.employeeId)
                                                        .flatMap(employee -> {
                                                            if (employee.getProviderId() == providerId) {
                                                                List<EmployeeRosterSlot> slotList = new ArrayList<>();
                                                                for (int i = 0; i < slotForm.size; i++) {
                                                                    LocalDate date = slotForm.date.plusDays(i);
                                                                    EmployeeRosterSlot employeeRosterSlot = new EmployeeRosterSlot(date, slot.getStart(), slot.getEnd());
                                                                    employeeRosterSlot.setEmployeeId(employee.getEmployeeId());
                                                                    employeeRosterSlot.setColor(slot.getColor());
                                                                    slotList.add(employeeRosterSlot);
                                                                }
                                                                return this.rosterService.saveRosterSlots(slotList)
                                                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                                                            } else {
                                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                                            }
                                                        });


                                            } else if (slotForm.subdivisionId != 0) {
                                                return this.employeeService.getSubdivision(slotForm.subdivisionId)
                                                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                                                .flatMap(division -> {
                                                                    if (division.getProviderId() == providerId) {
                                                                        List<SubdivisionRosterSlot> slotList = new ArrayList<>();
                                                                        for (int i = 0; i < slotForm.size; i++) {
                                                                            LocalDate date = slotForm.date.plusDays(i);
                                                                            var newSlot = new SubdivisionRosterSlot(date, slot.getStart(), slot.getEnd());
                                                                            newSlot.setSubdivisionId(subdivision.getSubdivisionId());
                                                                            newSlot.setColor(slot.getColor());
                                                                            slotList.add(newSlot);
                                                                        }
                                                                        return this.rosterService.saveSubdivisionRosterSlots(slotList)
                                                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                                                                    } else {
                                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                                                    }
                                                                }));
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                            }

                                        }
                                    })
                            );

                });
    }

    public Mono<ResponseEntity> deleteRosterSlot(Forms.DeleteRosterSlotForm slotForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(slotForm.slotDetails)
                        .flatMap(rosterSlotDetailsForm -> {
                            Mono<? extends RosterSlot> slotMono = null;
                            if (rosterSlotDetailsForm.employeeId != null && !rosterSlotDetailsForm.employeeId.equals("0"))
                                slotMono = this.rosterService.findSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (rosterSlotDetailsForm.subdivisionId != null && !rosterSlotDetailsForm.subdivisionId.equals("0"))
                                slotMono = this.rosterService.findSubdivisionRosterSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (slotForm == null)
                                return Mono.just("invalidSlot");

                            assert slotMono != null;
                            return slotMono
                                    .flatMap(slot -> this.rosterHelperService.returnInvalidSlotMessage(providerId, slot)
                                            .flatMap(invalid -> {
                                                if (invalid) {
                                                    return Mono.just("invalidSlot");
                                                } else {
                                                    if (slot instanceof EmployeeRosterSlot) {
                                                        return this.rosterService.deleteSlot((EmployeeRosterSlot) slot)
                                                                .then(Mono.just("success"));

                                                    } else if  (slot instanceof SubdivisionRosterSlot) {
                                                        return this.rosterService.deleteSubdivisionSlot((SubdivisionRosterSlot) slot)
                                                                .then(Mono.just("success"));
                                                    } else {
                                                        return Mono.just("error");
                                                    }
                                                }
                                            })
                                    );

                        })
                        .collectList()
                        .flatMap(this.rosterHelperService::processMessages)

                );
    }

    public Mono<ResponseEntity> publishRoster(Forms.DeleteOrPublishRosterSlotForm deleteForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                            List<LocalDate> interval = dateRange.enhancedDateRange(deleteForm.startDate, deleteForm.endDate);

                            return this.rosterHelperService.getEmployees(deleteForm.employeeId, deleteForm.subdivisionId, deleteForm.divisionId, providerId, false)
                                    .flatMap(employees -> {
                                        if (employees.size()>0) {
                                            return this.rosterHelperService.publishOrDeleteEmployeeRosterSlots(employees, interval, deleteForm.delete, deleteForm.unpublish)
                                                    .flatMap(response -> {
                                                        if (Objects.requireNonNull(response.getBody()).message.equals("success")){
                                                            if (deleteForm.subdivisionId != null) {
                                                                return this.rosterHelperService.checkSubdivisionId(deleteForm.subdivisionId, providerId)
                                                                        .flatMap(subdivision -> {
                                                                            if (subdivision.getName() != null) {
                                                                                return this.rosterHelperService.publishOrDeleteSubdivisionRosterSlots(subdivision, interval, deleteForm.delete);
                                                                            } else {
                                                                                return Mono.just(response);
                                                                            }
                                                                        });
                                                            } else {
                                                                return Mono.just(response);
                                                            }
                                                        } else {
                                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("error")));
                                                        }
                                                    });

                                        } else {
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                                        }
                                    });

                        }

                );
    }

    public Mono<ResponseEntity> approveTimeOff(Forms.DeleteForm deleteForm, boolean deny){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(deleteForm.idsToDelete)
                        .flatMap(id -> this.rosterService.findSlot(Long.parseLong(id))
                                .flatMap(slot -> this.employeeService.getEmployee(slot.getEmployeeId())
                                        .flatMap(employee -> {
                                            if (employee.getProviderId() == providerId) {
                                                slot.setTimeOffApproved(!deny);
                                                slot.setTimeOffDenied(deny);
                                                return this.rosterService.saveRosterSlot(slot)
                                                        .then(this.rosterHelperService.sendTimeOffApprovalResponseEmail(employee, !deny));

                                            } else {
                                                return Mono.just("invalidSlot");
                                            }

                                        })
                                )
                        )
                        .collectList()
                        .flatMap(this.rosterHelperService::processMessages)
                );
    }

    public Mono<ResponseEntity> approveAbsence(String requestId, String deny){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.rosterService.getAbsenceRequest(requestId)
                        .flatMap(absenceRequest -> this.employeeService.getEmployee(absenceRequest.getEmployeeId())
                                .flatMap(employee -> {
                                    if (employee.getProviderId() == providerId) {
                                        absenceRequest.setToBeApproved(false);
                                        absenceRequest.setApproved(deny == null);
                                        return this.rosterService.saveAbsenceRequest(absenceRequest)
                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidRequest")));
                                    }
                                }))
                );
    }



}
