package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.entities.rest.RosterEntity;
import com.bookanapp.employee.services.DateRangeService;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RosterHelper {
    private final RosterService rosterService;
    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final DateRangeService dateRange;

    public Mono<ResponseEntity> displaySharedEmployees(Long employeeId, Long subdivisionId, Long divisionId) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.getEmployees(employeeId, subdivisionId, divisionId, providerId, false)
                        .flatMap(employees -> this.getEmployeeEntities(employees, false)));
    }

    public Mono<ResponseEntity> getAllEmployees() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getAllEmployees)
                .flatMap(employees -> this.getEmployeeEntities(employees, false));

    }

    public Mono<ResponseEntity> displaySharedEmployeesAnonymously(Long employeeId, Long subdivisionId, Long divisionId) {
        return this.getEmployees(employeeId, subdivisionId, divisionId, 0, true)
                        .flatMap(employees -> this.getEmployeeEntities(employees, true));
    }

    public Mono<ResponseEntity> displayRoster(String start, String end, String offset, Long employeeId, String subdivisionId, String divisionId, String all, String showTimeOff) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId ->  {
                    List<LocalDate> dateRange = this.dateRange.dateRangeUTC(start, end, offset);
                    boolean timeOff = showTimeOff != null;

                    Mono<List<RosterEntity>> slotEntities = null;

                    if (employeeId != null)
                        slotEntities = this.getEmployeeRoster(employeeId, dateRange, timeOff);

                    if (subdivisionId != null && !subdivisionId.equals("null")) {
                        if (!timeOff) {
                            slotEntities = this.getSubdivisionRoster(Long.parseLong(subdivisionId), dateRange, providerId);
                        } else {
                            slotEntities = this.getSubDivisionEmployees(Long.parseLong(subdivisionId), providerId, false)
                                    .flatMap(employees -> Flux.fromIterable(employees)
                                            .flatMap(employee -> this.getEmployeeRoster(employee.getEmployeeId(), dateRange, true))
                                            .collectList()
                                            .flatMap(lists -> {
                                                List<RosterEntity> entities = new ArrayList<>();
                                                lists.forEach(entities::addAll);
                                                return Mono.just(entities);
                                            }));

                        }
                    }
                    if (divisionId != null && !divisionId.equals("null")) {
                        slotEntities = this.employeeService.getDivision(Long.parseLong(divisionId))
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
                                        return this.employeeService.getSubdivisions(division.getDivisionId())
                                                .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                                        .flatMap(subdivision -> this.getSubDivisionEmployees(subdivision.getSubdivisionId(), providerId, false)
                                                                .flatMap(employees -> Flux.fromIterable(employees)
                                                                        .flatMap(employee -> this.getEmployeeRoster(employee.getEmployeeId(), dateRange, timeOff))
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
                                .flatMap(employees -> Flux.fromIterable(employees)
                                        .flatMap(employee -> this.getEmployeeRoster(employee.getEmployeeId(), dateRange, true))
                                        .collectList()
                                        .flatMap(lists -> {
                                            List<RosterEntity> entities = new ArrayList<>();
                                            lists.forEach(entities::addAll);
                                            return Mono.just(entities);
                                        }));
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
            slotEntities = this.getAnonymousEmployeeRoster(employeeId, dateRange);

        if (subdivisionId != null && !subdivisionId.equals("null"))
            slotEntities = this.getAnonymousSubdivisionRoster(Long.parseLong(subdivisionId), dateRange);

        if (divisionId != null && !divisionId.equals("null")) {
            slotEntities = this.employeeService.getDivision(Long.parseLong(divisionId))
                    .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                            .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                    .flatMap(subdivision -> this.getSubDivisionEmployees(subdivision.getSubdivisionId(), 0, true)
                                            .flatMap(employees -> Flux.fromIterable(employees)
                                                    .flatMap(employee -> this.getAnonymousEmployeeRoster(employee.getEmployeeId(), dateRange))
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
                        .flatMap(employee -> returnSearchedEmployees(providerId, term, employee.getEmployeeId()))
                        .switchIfEmpty(returnSearchedEmployees(providerId, term, null)));
    }


    private Mono<List<RosterEntity>> getEmployeeRoster(long employeeId, List<LocalDate> dateRange, boolean showTimeOff) {

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

    private Mono<List<RosterEntity>> getAnonymousEmployeeRoster(long employeeId, List<LocalDate> dateRange) {

        if (dateRange.size()<40) {
            return this.employeeService.getEmployee(employeeId)
                    .flatMap(employee -> getEmployeeRosterEntities(employeeId, dateRange, false, employee, true));

        } else {
            return Mono.just(new ArrayList<>());
        }

    }

    private Mono<? extends List<RosterEntity>> getEmployeeRosterEntities(long employeeId, List<LocalDate> dateRange, boolean showTimeOff, Employee employee, boolean anonymous) {
        return Flux.fromIterable(dateRange)
                .flatMap(date -> this.rosterService.getRosterSlotsByDate(employeeId, date)
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
                        })
                )
                .collectList()
                .flatMap(lists -> {
                    List<RosterEntity> entities = new ArrayList<>();
                    lists.forEach(entities::addAll);
                    return Mono.just(entities);
                });
    }

    private Mono<List<RosterEntity>> getSubdivisionRoster(long subdivisionId, List<LocalDate> dateRange, Long providerId) {
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

    private Mono<List<RosterEntity>> getAnonymousSubdivisionRoster(long subdivisionId, List<LocalDate> dateRange) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> this.getSubdivisionRosterEntities(dateRange, subdivisionId)));

    }

    private Mono<List<RosterEntity>> getSubdivisionRosterEntities(List<LocalDate> dateRange, long subdivisionId) {
        dateRange.removeIf(e -> e.isBefore(LocalDate.now().minusDays(90L)));
        return Flux.fromIterable(dateRange)
                .flatMap(date -> this.rosterService.findSubdivisionRosterSlotsByDate(subdivisionId, date)
                        .flatMap(dateSlots -> {
                            List<RosterEntity> entities = new ArrayList<>();
                            dateSlots.forEach(
                                    slot -> entities.add(new RosterEntity(slot))
                            );
                            return Mono.just(entities);
                        })
                )
                .collectList()
                .flatMap(lists -> {
                    List<RosterEntity> entities = new ArrayList<>();
                    lists.forEach(entities::addAll);
                    return Mono.just(entities);
                });
    }

    private Mono<ResponseEntity> returnSearchedEmployees(long providerId, String term, Long employeeId) {

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

    public Mono<ResponseEntity> uploadRoster(Forms.RosterForm rosterForm) {
        return this.commonHelper.fetchCurrentProvider()
                .flatMap(provider -> this.employeeService.getEmployee(Long.parseLong(rosterForm.employeeId))
                        .flatMap(employee -> {
                            if (employee.getProviderId() == provider.getId()) {
                                List<LocalDate> interval = dateRange.dateRange(rosterForm.schedule.startDate, rosterForm.schedule.endDate);
                                List<EmployeeRosterSlot> slots = new ArrayList<>();

                                String start;
                                String end;

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

                                                    start = dateRange.addZeroBeforeInt(daySchedule.start.hour) + ":" + dateRange.addZeroBeforeInt(daySchedule.start.minute);
                                                    end =  dateRange.addZeroBeforeInt(daySchedule.end.hour) + ":" + dateRange.addZeroBeforeInt(daySchedule.end.minute);


                                                    LocalTime scheduleStart = LocalTime.of(daySchedule.start.hour, daySchedule.start.minute);
                                                    LocalTime scheduleEnd = LocalTime.of(daySchedule.end.hour, daySchedule.end.minute);

                                                    if (scheduleEnd.isBefore(scheduleStart))
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                                                    if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                                    } else {
                                                        initialEndTime = scheduleEnd;

                                                    }

                                                    createRosterSlots(rosterForm, provider, employee, slots, null, null, start, end, date, scheduleStart, scheduleEnd);
                                                }
                                            }

                                        }

                                    } else {
                                        LocalTime initialEndTime = null;

                                        start = dateRange.addZeroBeforeInt(rosterForm.patternStart.hour) + ":" + dateRange.addZeroBeforeInt(rosterForm.patternStart.minute);
                                        end = dateRange.addZeroBeforeInt(rosterForm.patternEnd.hour) + ":" + dateRange.addZeroBeforeInt(rosterForm.patternEnd.minute);


                                        LocalTime scheduleStart = LocalTime.parse(start, DateTimeFormatter.ofPattern("H:mm"));
                                        LocalTime scheduleEnd = LocalTime.parse(end, DateTimeFormatter.ofPattern("H:mm"));

                                        if (scheduleEnd.isBefore(scheduleStart))
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                                        if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                        } else {
                                            initialEndTime = scheduleEnd;

                                        }

                                        createRosterSlots(rosterForm, provider, employee, slots, null, null, start, end, date, scheduleStart, scheduleEnd);
                                    }


                                }

                                return this.rosterService.saveRosterSlots(slots)
                                        .then(this.saveNewColor(rosterForm, provider.getId()))
                                        .then(this.saveNewPattern(rosterForm, provider.getId()))
                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                            }
                        }));

    }

    public Mono<ResponseEntity> uploadSubdivisionRoster(Forms.SubdivisionRosterForm rosterForm) {
        return this.commonHelper.fetchCurrentProvider()
                .flatMap(provider -> this.employeeService.getSubdivision(rosterForm.subdivisionId)
                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> {
                                    if (division.getProviderId() == provider.getId()) {
                                        List<LocalDate> interval = dateRange.dateRange(rosterForm.schedule.startDate, rosterForm.schedule.endDate);
                                        List<SubdivisionRosterSlot> slots = new ArrayList<>();

                                        String start;
                                        String end;

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

                                                            start = dateRange.addZeroBeforeInt(daySchedule.start.hour) + ":" + dateRange.addZeroBeforeInt(daySchedule.start.minute);
                                                            end =  dateRange.addZeroBeforeInt(daySchedule.end.hour) + ":" + dateRange.addZeroBeforeInt(daySchedule.end.minute);


                                                            LocalTime scheduleStart = LocalTime.of(daySchedule.start.hour, daySchedule.start.minute);
                                                            LocalTime scheduleEnd = LocalTime.of(daySchedule.end.hour, daySchedule.end.minute);

                                                            if (scheduleEnd.isBefore(scheduleStart))
                                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                                                            if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                                            } else {
                                                                initialEndTime = scheduleEnd;

                                                            }

                                                            createSubdivisionRosterSlots(rosterForm, provider, null, null, slots, subdivision, start, end, date, scheduleStart, scheduleEnd);
                                                        }
                                                    }

                                                }

                                            } else {
                                                LocalTime initialEndTime = null;

                                                start = dateRange.addZeroBeforeInt(rosterForm.patternStart.hour) + ":" + dateRange.addZeroBeforeInt(rosterForm.patternStart.minute);
                                                end = dateRange.addZeroBeforeInt(rosterForm.patternEnd.hour) + ":" + dateRange.addZeroBeforeInt(rosterForm.patternEnd.minute);


                                                LocalTime scheduleStart = LocalTime.parse(start, DateTimeFormatter.ofPattern("H:mm"));
                                                LocalTime scheduleEnd = LocalTime.parse(end, DateTimeFormatter.ofPattern("H:mm"));

                                                if (scheduleEnd.isBefore(scheduleStart))
                                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));

                                                if (initialEndTime != null && scheduleStart.isBefore(initialEndTime)) {
                                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidHour")));
                                                } else {
                                                    initialEndTime = scheduleEnd;

                                                }

                                                createSubdivisionRosterSlots(rosterForm, provider, null, null, slots, subdivision, start, end, date, scheduleStart, scheduleEnd);
                                            }


                                        }

                                        return this.rosterService.saveSubdivisionRosterSlots(slots)
                                                .then(this.saveNewColor(rosterForm, provider.getId()))
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
                                    .flatMap(slot -> this.returnInvalidSlotMessage(providerId, slot)
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
                        .flatMap(this::processMessages)

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
                            .flatMap(slot -> this.returnInvalidSlotMessage(providerId, slot)
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
                                    .flatMap(slot -> this.returnInvalidSlotMessage(providerId, slot)
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
                        .flatMap(this::processMessages)

                );
    }

    public Mono<ResponseEntity> publishRoster(Forms.DeleteOrPublishRosterSlotForm deleteForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                            List<LocalDate> interval = dateRange.enhancedDateRange(deleteForm.startDate, deleteForm.endDate);

                            return this.getEmployees(deleteForm.employeeId, deleteForm.subdivisionId, deleteForm.divisionId, providerId, false)
                                    .flatMap(employees -> {
                                        if (employees.size()>0) {
                                            return this.publishOrDeleteEmployeeRosterSlots(employees, interval, deleteForm.delete)
                                                    .flatMap(response -> {
                                                        if (Objects.requireNonNull(response.getBody()).message.equals("success")){
                                                            if (deleteForm.subdivisionId != null) {
                                                                return this.checkSubdivisionId(deleteForm.subdivisionId, providerId)
                                                                        .flatMap(subdivision -> {
                                                                            if (subdivision.getName() != null) {
                                                                                return this.publishOrDeleteSubdivisionRosterSlots(subdivision, interval, deleteForm.delete);
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
                                                        .then(this.sendTimeOffApprovalResponseEmail(employee, !deny));

                                            } else {
                                                return Mono.just("invalidSlot");
                                            }

                                        })
                                )
                        )
                        .collectList()
                        .flatMap(this::processMessages)
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

    private Mono<String> sendTimeOffApprovalResponseEmail(Employee employee, boolean approved) {
        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.providerServiceUrl + "/provider/get/" + employee.getProviderId());

        return client.get()
                .retrieve()
                .bodyToMono(Provider.class)
                .flatMap(provider -> {
                    String url = commonHelper.notificationServiceUrl + "/email/employee/timeoff/approve";
                    if (!approved)
                        url = url + "?deny=true";
                    var emailClient = this.commonHelper.buildAPIAccessWebClient(url);
                    var form = new Forms.TimeOffRequestNotificationForm(provider, employee, employee.getUsername());
                    return emailClient.post()
                            .body(Mono.just(form), Forms.TimeOffRequestNotificationForm.class)
                            .retrieve()
                            .bodyToMono(String.class)
                            .flatMap(message -> Mono.just("success"))
                            .onErrorResume(e -> Mono.just("success"));
                });

    }


    private Mono<ResponseEntity<Forms.GenericResponse>> publishOrDeleteEmployeeRosterSlots(List<Employee> employees, List<LocalDate> range, boolean delete) {
        return Flux.fromIterable(employees)
                .flatMap(employee -> Flux.fromIterable(range)
                        .flatMap(date -> this.rosterService.getRosterSlotsByDate(employee.getEmployeeId(), date)
                                .flatMap(slots -> {
                                    if (delete) {
                                        return this.rosterService.deleteSlots(slots)
                                                .then(Mono.just("success"));
                                    } else {
                                        slots.forEach(slot -> slot.setPublished(true));
                                        return this.rosterService.saveRosterSlots(slots)
                                                .then(Mono.just("success"));
                                    }
                                })

                        )
                        .collectList())
                .collectList()
                .flatMap(lists -> {
                    List<String> messages = new ArrayList<>();
                    lists.forEach(messages::addAll);
                    return this.processMessages(messages);
                });
    }

    private Mono<ResponseEntity<Forms.GenericResponse>> publishOrDeleteSubdivisionRosterSlots(Subdivision subdivision, List<LocalDate> range, boolean delete) {
        return Flux.fromIterable(range)
                .flatMap(date -> this.rosterService.findSubdivisionRosterSlotsByDate(subdivision.getSubdivisionId(), date)
                        .flatMap(slots -> {
                            if (delete) {
                                return this.rosterService.deleteSubdivisionSlots(slots)
                                        .then(Mono.just("success"));
                            } else {
                                slots.forEach(slot -> slot.setPublished(true));
                                return this.rosterService.saveSubdivisionRosterSlots(slots)
                                        .then(Mono.just("success"));
                            }
                        })

                )
                .collectList()
                .flatMap(this::processMessages);
    }


    private Mono<ResponseEntity<Forms.GenericResponse>> processMessages(List<String> list){
        if (list.contains("error")) {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("error")));
        } else if (list.contains("invalidSlot")) {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
        } else {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success")));
        }
    }

    private Mono<Boolean> returnInvalidSlotMessage(long providerId, RosterSlot slot) {
        if (slot instanceof EmployeeRosterSlot)
            return this.employeeService.getEmployee(((EmployeeRosterSlot) slot).getEmployeeId())
                    .flatMap(employee -> Mono.just(employee.getProviderId() != providerId));

        if (slot instanceof SubdivisionRosterSlot)
            return this.employeeService.getSubdivision(((SubdivisionRosterSlot) slot).getSubdivisionId())
                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                            .flatMap(division -> Mono.just(division.getProviderId() != providerId)));

        return Mono.just(true);

    }

    private Mono<List<Employee>> getSubDivisionEmployees(long subdivisionId, long providerId, boolean anonymous) {
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

    private Mono<ResponseEntity> getSearchedEmployeeEntities(List<Employee> employees, Long employeeId) {
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

    private Mono<Employee> loadEmployee(long employeeId) {
        return this.employeeService.getEmployee(employeeId)
                .flatMap(employee -> {
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
                );


    }


    private Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {

        return  Mono.just(EmployeeEntity.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .registerDate(employee.getRegisterDate().toString())
                .username(employee.getUsername())
                .avatar(employee.getAvatar())
                .subdivision(employee.getSubdivision() != null ? employee.getSubdivision().getName() : null)
                .division(employee.getSubdivision() != null ? employee.getSubdivision().getDivision().getName() : null)
                .subdivisionId(employee.getSubdivisionId())
                .divisionId(employee.getSubdivision() != null && employee.getSubdivision().getDivision() != null ? employee.getSubdivision().getDivision().getDivisionId() : null)
                .jobTitle(employee.getJobTitle())
                .bankAccount(employee.getBankAccount())
                .taxPayerId(employee.getTaxPayerId())
                .personalEmail(employee.getPersonalEmail())
                .build());


    }

    /**
     * Creates roster slots for either employee or subdivision
     * @param rosterForm UI form
     * @param subdivision subdivision
     * @param slots list of roster slots
     * @param subdivisionSlots list of subdivision slots
     * @param date date for slots
     * @param durationInMinutes duration for each slot
     * @param hours start and end of slot
     */
    private void createSlots(Forms.RosterSuperForm rosterForm, Employee employee, Subdivision subdivision, List<EmployeeRosterSlot> slots, List<SubdivisionRosterSlot> subdivisionSlots, LocalDate date, long durationInMinutes, List<LocalTime> hours) {
        hours.forEach(x -> {
            if (date.isAfter(LocalDate.now().minusDays(1L))) {
                LocalDateTime key = LocalDateTime.of(date, x);
                if (key.isAfter(LocalDateTime.now())) {
                    RosterSlot slot;
                    if (employee != null) {
                        slot = new EmployeeRosterSlot(date, key.toLocalTime(), key.plusMinutes(durationInMinutes).toLocalTime());
                        ((EmployeeRosterSlot) slot).setEmployeeId(employee.getEmployeeId());
                    } else {
                        slot = new SubdivisionRosterSlot(date, key.toLocalTime(), key.plusMinutes(durationInMinutes).toLocalTime());
                        ((SubdivisionRosterSlot) slot).setSubdivisionId(subdivision.getSubdivisionId());
                    }
                    if (rosterForm.getColor() != null)
                        slot.setColor(rosterForm.getColor());

                    if (rosterForm.getNote() != null)
                        slot.setNote(rosterForm.getNote());

                    if (rosterForm.isPublish())
                        slot.setPublished(true);

                    if (employee != null) {
                        slots.add((EmployeeRosterSlot) slot);
                    } else {
                        subdivisionSlots.add((SubdivisionRosterSlot) slot);
                    }

                }
            }
        });
    }

    private void createRosterSlots(Forms.RosterForm rosterForm, Provider provider, Employee employee, List<EmployeeRosterSlot> slots, List<SubdivisionRosterSlot> subdivisionSlots, Subdivision subdivision, String start, String end, LocalDate date, LocalTime scheduleStart, LocalTime scheduleEnd) {
        long durationInMinutes = scheduleStart.until(scheduleEnd, ChronoUnit.MINUTES);
        String duration = LocalTime.MIN.plus(Duration.ofMinutes(durationInMinutes)).toString();
        List<LocalTime> hours = dateRange.addHour(start, end, duration, "1", provider);
        this.createSlots(rosterForm, employee, subdivision, slots, subdivisionSlots, date, durationInMinutes, hours);

    }

    private void createSubdivisionRosterSlots(Forms.SubdivisionRosterForm rosterForm, Provider provider, Employee employee, List<EmployeeRosterSlot> slots, List<SubdivisionRosterSlot> subdivisionSlots, Subdivision subdivision, String start, String end, LocalDate date, LocalTime scheduleStart, LocalTime scheduleEnd) {
        long durationInMinutes = scheduleStart.until(scheduleEnd, ChronoUnit.MINUTES);
        String duration = LocalTime.MIN.plus(Duration.ofMinutes(durationInMinutes)).toString();
        List<LocalTime> hours = dateRange.addHour(start, end, duration, "1", provider);
        this.createSlots(rosterForm, employee, subdivision, slots, subdivisionSlots, date, durationInMinutes, hours);

    }

    private Mono<String> saveNewPattern(Forms.RosterForm rosterForm, long providerId){
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

    private Mono<String> saveNewColor(Forms.RosterSuperForm rosterForm, long providerId){
        if (rosterForm.colorName != null) {
            RosterSlotColor rosterSlotColor = new RosterSlotColor(providerId, rosterForm.colorName, rosterForm.color);
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

    private Mono<List<Employee>> getEmployees(Long employeeId, Long subdivisionId, Long divisionId, long providerId, boolean anonymous) {
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

    private Mono<ResponseEntity<List<EmployeeEntity>>> getEmployeeEntities(List<Employee> employees, boolean anonymous) {
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

    private Mono<Subdivision> checkSubdivisionId(long subdivisionId, long providerId) {
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

    private String getInitials(String name) {
        String[] words = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for(String word : words) {
            builder.append(word.charAt(0));
        }
        return builder.toString();
    }

}
