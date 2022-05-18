package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeHelperService {

    final CommonHelper commonHelper;
    final EmployeeService employeeService;

    Mono<Employee> setAuthorizedRosters(Employee employee, List<Long> authorizedRosters) {
        var persistNewRosters = this.employeeService.getSubdivisionsByListOfIds(authorizedRosters)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                            if (division.getProviderId() == employee.getProviderId()) {
                                return Mono.just(new AuthorizedRoster(employee.getEmployeeId(), subdivision.getSubdivisionId()));
                            } else {
                                return Mono.empty();
                            }
                        })
                )
                .collectList()
                .flatMap(this.employeeService::saveAuthorizedRosters);


        return this.employeeService.getAuthorizedRosters(employee.getEmployeeId())
                .switchIfEmpty(persistNewRosters)
                .flatMap(this.employeeService::deleteAuthorizedRosters)
                .then(persistNewRosters)
                .then(Mono.just(employee));
    }

    Mono<Employee> setAuthorizedScheduleNames(Employee employee, List<Long> schedules) {
        //TODO connect to appointment service to retrieve authorised schedules


        return Mono.just(employee);
//        Set<String> ids = new HashSet<>();
//        schedules.forEach(schedule -> ids.add(Long.toString(schedule)));
//
//        var webclient = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl + "/employee/schedules");
//        return webclient.post()
//                .body(Mono.just(new Forms.SetOfStringsForm(ids)), Forms.SetOfStringsForm.class)
//                .retrieve()
//                .bodyToMono(String[].class)
//                .flatMap(scheduleList -> {
//                    employee.setAuthorizedScheduleNames(Arrays.asList(scheduleList));
//                    return Mono.just(employee);
//                });
    }

    Mono<Boolean> isRegistered(String username, long id) {

        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/registered/" + id +"?username=" + username);

        return client.get()
                .retrieve()
                .bodyToMono(Boolean.class);


    }


    Mono<EmployeeEntity> buildShortEmployeeEntity(Employee employee) {
        if (employee.getSubdivisionId() != null) {
            return this.employeeService.getSubdivision(employee.getSubdivisionId())
                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                            .flatMap(division -> Mono.just(EmployeeEntity.builder()
                                    .id(employee.getEmployeeId())
                                    .name(employee.getName())
                                    .avatar(employee.getAvatar())
                                    .subdivision(subdivision.getName())
                                    .division(division.getName())
                                    .subdivisionId(subdivision.getSubdivisionId())
                                    .divisionId(subdivision.getDivisionId())
                                    .jobTitle(employee.getJobTitle())
                                    .build()))
                    );
        } else {
            return Mono.just(EmployeeEntity.builder()
                    .id(employee.getEmployeeId())
                    .name(employee.getName())
                    .avatar(employee.getAvatar())
                    .jobTitle(employee.getJobTitle())
                    .build());
        }


    }


    Mono<List<Employee>> getEmployees(Long employeeId, Long subdivisionId, Long divisionId, long providerId, List<Employee> employees) {
        if (employeeId != null) {
            return this.employeeService.getEmployeeByProviderId(employeeId, providerId)
                    .switchIfEmpty(Mono.just(new Employee()))
                    .flatMap(employee -> {
                        List<Employee> emps = new ArrayList<>();

                        if (employee.getEmployeeId() != null)
                            emps.add(employee);

                        return Mono.just(emps);
                    });
        }

        if (subdivisionId != null) {
            return this.employeeService.getAllSubdivisionEmployees(providerId, subdivisionId);
        }

        if (divisionId != null) {
            return this.employeeService.getAllDivisionEmployees(providerId, divisionId);

        } else {
            return this.employeeService.findAllUnassignedEmployees(providerId);
        }

    }



    Mono<String> deleteEmployeeDetails(Employee employee) {
        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/delete/" + employee.getEmployeeId());
        return client.get()
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    if (response.equals("ok")) {
                        return this.employeeService.deleteEmployee(employee)
                                .then(Mono.just("ok"));
                    } else {
                        return Mono.error(new RuntimeException("Error deleting employee details from auth service"));
                    }
                });
    }

    Mono<? extends ResponseEntity> persistSubdivision(Forms.NewEmployeeForm form, Long providerId, Employee employee, boolean edit) {
        if (form.subdivisionId != null){
            return this.checkSubdivision(providerId, form.subdivisionId)
                    .flatMap(bool -> {
                        if (bool){
                            employee.setSubdivisionId(form.subdivisionId);
                        }
                        if (edit) {
                            return this.setSubdivision(form.subdivisionId, employee);
                        } else {
                            return this.persistEmployee(employee, false);
                        }
                    });
        } else if (form.division!=null && form.subdivision !=null && form.division.length()>1 && form.subdivision.length()>1) {
            return this.findSubdivision(providerId, form.division, form.subdivision)
                    .flatMap(subdivisionId -> {
                        employee.setSubdivisionId(subdivisionId);
                        if (edit) {
                            return this.setSubdivision(subdivisionId, employee);
                        } else {
                            return this.persistEmployee(employee, false);
                        }
                    });
        } else {
            return this.persistEmployee(employee, edit);
        }
    }

    Mono<Boolean> checkSubdivision(long providerId, long subdivisionId) {
        return this.employeeService.getSubdivision(subdivisionId)
                .switchIfEmpty(Mono.defer(() -> Mono.just(new Subdivision())))
                .flatMap(subdivision -> {
                    if (subdivision.getSubdivisionId() > 0) {
                        return this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> Mono.just(division.getProviderId() == providerId));
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    Mono<Long> findSubdivision(long providerId, String divisionName, String subdivisionName) {
        return this.employeeService.getDivisionByName(providerId, divisionName)
                .switchIfEmpty(Mono.defer(() -> this.saveDivision(providerId, divisionName)))
                .flatMap(division -> this.employeeService.getSubdivisionByName(division.getDivisionId(), subdivisionName)
                        .switchIfEmpty(Mono.defer(() -> this.saveSubdivision(division.getDivisionId(), subdivisionName)))
                        .flatMap(subdivision -> Mono.just(subdivision.getSubdivisionId())));
    }

    Mono<Division> saveDivision(long providerId, String name) {
        var division = new Division(providerId, name);
        return this.employeeService.saveDivision(division)
                .flatMap(savedDivision -> this.employeeService.getDivisionByName(providerId, name));
    }

    Mono<Subdivision> saveSubdivision(long divisionId, String name) {
        var subdivision = new Subdivision(divisionId, name);
        return this.employeeService.saveSubdivision(subdivision)
                .flatMap(savedDivision -> this.employeeService.getSubdivisionByName(divisionId, name));
    }


    Mono<String> deleteEmployeeDetails(String username, long providerId, long employeeId) {
        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/delete/" + employeeId);
        return client.get()
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> this.employeeService.getEmployeeByUsername(providerId, username)
                        .switchIfEmpty(Mono.just(new Employee()))
                        .flatMap(employee -> {
                            if (employee.getEmployeeId() != null) {
                                return this.employeeService.deleteEmployee(employee)
                                        .then(Mono.just("ok"));
                            } else {
                                return Mono.just("ok");
                            }
                        })

                );

    }

    Mono<ResponseEntity> persistEmployee(Employee emp, boolean edit) {

        if (edit) {
            return this.employeeService.saveEmployee(emp)
                    .then(this.buildEmployeeEntity(emp)
                            .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity))));
        } else {
            return this.employeeService.saveEmployee(emp)
                    .flatMap(e -> this.employeeService.getEmployeeByUsername(e.getProviderId(), e.getUsername()))
                    .flatMap(employee -> this.employeeService.saveTimeOffBalance(emp.getTimeOffBalance()))
                    .flatMap(timeOff -> {
                        List<AuthorizedSchedule> authorizedSchedules = new ArrayList<>();
                        emp.getAuthorizedSchedules().forEach(authorizedSchedule -> authorizedSchedules.add(new AuthorizedSchedule(emp.getEmployeeId(), authorizedSchedule)));
                        return this.employeeService.saveAuthorizedSchedules(authorizedSchedules)
                                .flatMap(authorizedSchedList -> {
                                    List<AuthorizedRoster> authorizedRosters = new ArrayList<>();
                                    emp.getAuthorizedRosters().forEach(authorizedRoster -> authorizedRosters.add(new AuthorizedRoster(emp.getEmployeeId(), authorizedRoster)));
                                    return this.employeeService.saveAuthorizedRosters(authorizedRosters)
                                            .then(this.buildShortEmployeeEntity(emp)
                                                    .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity))));
                                });
                    });
        }
    }

    Mono<List<Long>> setPersonalAuthorizedRosters (Long subDivisionId, long providerId) {
        if (subDivisionId != null) {

            return this.checkSubdivision(providerId, subDivisionId)
                    .flatMap(bool -> {
                        if (bool) {
                            return this.employeeService.getSubdivision(subDivisionId)
                                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                            .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                                                    .flatMap(subdivisions -> {
                                                        List<Long> authorizedRosters = new ArrayList<>();
                                                        subdivisions.forEach(sub ->  authorizedRosters.add(sub.getSubdivisionId()));
                                                        return Mono.just(authorizedRosters);
                                                    })
                                            )
                                    );

                        } else {
                            return Mono.just(new ArrayList<>());
                        }
                    });

        } else {
            return Mono.just(new ArrayList<>());
        }
    }


    Mono<ResponseEntity> setSubdivision(long subdivisionId, Employee employee) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                            subdivision.setDivision(division);
                            employee.setSubdivision(subdivision);

                            return this.persistEmployee(employee, true);
                        }));
    }

    Mono<TimeOffBalance> saveTimeOffBalance(long employeeId, EmployeeEntity.TimeOffEntity timeOffBalance) {
        TimeOffBalance balance = new TimeOffBalance(employeeId, 0,0,0,0,0,0);
        Mono<TimeOffBalance> newBalance = this.employeeService.saveTimeOffBalance(balance);

        return this.employeeService.getTimeOffBalance(employeeId)
                .switchIfEmpty(Mono.defer(() -> newBalance))
                .flatMap(timeOff -> {
                    if (timeOffBalance != null) {
                        timeOff.setVacationDays(timeOffBalance.getVacationDays());
                        timeOff.setVacationRolloverDays(timeOffBalance.getVacationRolloverDays());
                        timeOff.setComplimentaryBankHolidayDays(timeOffBalance.getComplimentaryBankHolidayDays());
                        timeOff.setComplimentaryBankHolidayRolloverDays(timeOffBalance.getComplimentaryBankHolidayRolloverDays());
                        timeOff.setCompensationDays(timeOffBalance.getCompensationDays());
                        timeOff.setCompensationRolloverDays(timeOffBalance.getCompensationRolloverDays());
                        return this.employeeService.saveTimeOffBalance(timeOff);
                    } else {
                        return Mono.just(timeOff);
                    }
                });
    }


    Mono<List<Long>> setAuthorizedRosters (Forms.NewEmployeeForm employeeForm, long providerId) {
        if (employeeForm.subdivisionIds!=null && employeeForm.subdivisionIds.size()>0){
            return Flux.fromIterable(employeeForm.subdivisionIds)
                    .flatMap(subDivisionId -> this.employeeService.getSubdivision(Long.parseLong(subDivisionId))
                            .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                    .flatMap(division -> {
                                        if (division.getProviderId() == providerId) {
                                            return Mono.just(subdivision.getSubdivisionId());
                                        } else {
                                            return Mono.empty();
                                        }
                                    }))
                    )
                    .collectList()
                    .flatMap(Mono::just);
        } else {
            return Mono.just(new ArrayList<>());
        }

    }

    Mono<List<Long>> setAuthorizedSchedules (Forms.NewEmployeeForm employeeForm, long providerId, Long employeeId, boolean deleteAll) {
        if (deleteAll) {
            return this.employeeService.getAuthorizedSchedules(employeeId)
                    .flatMap(this.employeeService::deleteAuthorizedSchedules)
                    .then(Mono.just(new ArrayList<>()));
        } else {
            if (employeeForm.availabilityIds != null && employeeForm.availabilityIds.size() > 0) {

                return Flux.fromIterable(employeeForm.availabilityIds)
                        .flatMap(id -> {
                            var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl +
                                    "/schedule/get/" + id + "?providerId=" + providerId);
                            return client.get()
                                    .retrieve()
                                    .bodyToMono(Long.class);

                        })
                        .collectList()
                        .flatMap(list -> {
                            var authorizedSchedules = list.stream()
                                    .filter(id -> id > 0)
                                    .collect(Collectors.toList());

                            if (employeeId != null) {
                                List<AuthorizedSchedule> authorizedSchedulesToSave = new ArrayList<>();
                                authorizedSchedules.forEach(authorizedSchedule -> authorizedSchedulesToSave.add(new AuthorizedSchedule(employeeId, authorizedSchedule)));
                                return this.employeeService.getAuthorizedSchedules(employeeId)
                                        .flatMap(this.employeeService::deleteAuthorizedSchedules)
                                        .then(this.employeeService.saveAuthorizedSchedules(authorizedSchedulesToSave))
                                        .then(Mono.just(authorizedSchedules));
                            } else {
                                return Mono.just(authorizedSchedules);
                            }

                        });
            } else {
                return Mono.just(new ArrayList<>());
            }
        }

    }

    Mono<ResponseEntity> getTimeOff(long id) {

        return this.employeeService.getTimeOffRequest(id)
                .flatMap(timeOffRequests ->
                        getTimeOffRequestEntities(timeOffRequests)
                );
    }

//
//    private Mono<ResponseEntity> setSubdivision(long subdivisionId, Employee employee) {
//        return this.employeeService.getSubdivision(subdivisionId)
//                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
//                        .flatMap(division -> {
//                            subdivision.setDivision(division);
//                            employee.setSubdivision(subdivision);
//
//                            return this.persistEmployee(employee, true);
//                        }));
//    }




    Mono<ResponseEntity> filterEmployeeByName(List<Employee> employees) {
        return Flux.fromIterable(employees)
                .flatMap(this::buildShortEmployeeEntity)
                .collectList()
                .flatMap(employeeEntities -> Mono.just(ResponseEntity.ok(employeeEntities)));
    }

    Mono<ResponseEntity> filteredEmployees(List<Employee> employees, Integer page, Integer employeesPerPage) {


        var map = new Forms.EmployeeMap();
        map.setTotal(employees.size());

        PagedListHolder<Employee> employeePagedListHolder=new PagedListHolder<>(employees);

        if (page==null){
            page=1;
        }

        int goToPage=page-1;

        if (goToPage<=employeePagedListHolder.getPageCount()&&goToPage>=0){
            employeePagedListHolder.setPage(goToPage);
        }

        employeePagedListHolder.setPageSize(employeesPerPage);

        return Flux.fromIterable(employeePagedListHolder.getPageList())
                .flatMap(this::buildShortEmployeeEntity)
                .collectList()
                .flatMap(entities -> {
                    map.setEntities(entities);
                    return Mono.just(ResponseEntity.ok(map));
                });


    }


    Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {

        List<Long> schedules = new ArrayList<>();

        if (employee.getAuthorizedSchedules() != null) {
            schedules.addAll(employee.getAuthorizedSchedules());
        }

        if (employee.getAuthorities().contains("SUBPROVIDER_FULL")) {
            employee.getAuthorities().clear();
            employee.getAuthorities().add("SUBPROVIDER_FULL");
        }

        return  Mono.just(EmployeeEntity.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .authorities(employee.getAuthorities())
                .authorizedSchedules(schedules)
                .authorizedScheduleNames(employee.getAuthorizedScheduleNames())
                .registerDate(employee.getRegisterDate().toString())
                .username(employee.getUsername())
                .avatar(employee.getAvatar())
                .subdivision(employee.getSubdivision() != null ? employee.getSubdivision().getName() : null)
                .division(employee.getSubdivision() != null ? employee.getSubdivision().getDivision().getName() : null)
                .subdivisionId(employee.getSubdivisionId())
                .divisionId(employee.getSubdivision() != null && employee.getSubdivision().getDivision() != null ? employee.getSubdivision().getDivision().getDivisionId() : null)
                .jobTitle(employee.getJobTitle())
                .authorizedRosters(employee.getAuthorizedRosters())
                .timeOffBalance(employee.getTimeOffBalance() != null ? new EmployeeEntity.TimeOffEntity(employee.getTimeOffBalance()) : null)
                .homeAddress(employee.getAddress())
                .phones(employee.getPhones())
                .family(employee.getFamily())
                .bankAccount(employee.getBankAccount())
                .taxPayerId(employee.getTaxPayerId())
                .personalEmail(employee.getPersonalEmail())
                .build());


    }

    Mono<ResponseEntity<List<TimeRequestEntity>>> getTimeOffRequestEntities(List<TimeRequest> timeOffRequests) {
        return Flux.fromIterable(timeOffRequests)
                .flatMap(timeOffRequest -> {
                    var entity = new TimeRequestEntity(timeOffRequest);
                    var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl + "/upload/timeoff/attachments/" + timeOffRequest.getId());

                    return client.get()
                            .retrieve()
                            .bodyToMono(String[].class)
                            .flatMap(response -> {
                                entity.setAttachments(Arrays.asList(response));
                                return Mono.just(entity);

                            });

                })
                .collectList()
                .flatMap(entities -> {
                    var sortedEntities = entities.stream()
                            .sorted(Comparator.comparing(TimeRequestEntity::getStart))
                            .collect(Collectors.toList());

                    Collections.reverse(sortedEntities);
                    return Mono.just(ResponseEntity.ok(sortedEntities));
                });
    }



}
