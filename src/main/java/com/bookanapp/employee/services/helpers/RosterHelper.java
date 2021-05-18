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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .flatMap(providerId -> this.getEmployees(employeeId, subdivisionId, divisionId, providerId)
                        .flatMap(this::getEmployeeEntities));
    }

    public Mono<ResponseEntity> getAllEmployees() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getAllEmployees)
                .flatMap(this::getEmployeeEntities);

    }

    public Mono<ResponseEntity> displayRoster(String start, String end, String offset, Long employeeId, Long subdivisionId, Long divisionId, String all, String showTimeOff) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId ->  {
                    List<LocalDate> dateRange = this.dateRange.dateRangeUTC(start, end, offset);
                    Set<Long> employeeIds = new HashSet<>();
                    List<RosterEntity> entities = new ArrayList<>();
                    boolean timeOff = showTimeOff != null;

                    if (employeeId != null)
                        employeeIds.add(employeeId);

                })
    }

    public Mono<ResponseEntity> findEmployeeByName(String term) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.commonHelper.getCurrentEmployee()
                        .flatMap(employee -> returnSearchedEmployees(providerId, term, employee.getEmployeeId()))
                        .switchIfEmpty(returnSearchedEmployees(providerId, term, null)));
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
                                List<LocalDate> interval = dateRange.dateRange(rosterForm.schedule.startDate, rosterForm.schedule.endDate, false);
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

    private Mono<String> saveNewColor(Forms.RosterForm rosterForm, long providerId){
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

    private Mono<List<Employee>> getEmployees(Long employeeId, Long subdivisionId, Long divisionId, long providerId) {
        if (employeeId != null) {
            return this.employeeService.getEmployee(employeeId)
                    .flatMap(employee -> {
                        List<Employee> employees = new ArrayList<>();
                        employees.add(employee);
                        return Mono.just(employees);
                    });
        } else if (subdivisionId != null){
            return this.employeeService.getSubdivision(subdivisionId)
                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                            .flatMap(division -> {
                                        if (division.getProviderId() == providerId ) {
                                            return this.employeeService.findEmployeesBySubdivision(subdivisionId);
                                        } else {
                                            return Mono.just(new ArrayList<>());
                                        }
                                    }
                            )
                    );

        } else if (divisionId != null){
            return this.employeeService.getDivision(divisionId)
                    .flatMap(division -> {
                                if (division.getProviderId() == providerId ) {
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

    private Mono<ResponseEntity<List<EmployeeEntity>>> getEmployeeEntities(List<Employee> employees) {
        return Flux.fromIterable(employees)
                .flatMap(employee -> {
                    var entity = new EmployeeEntity(employee.getEmployeeId(), employee.getName());
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


}