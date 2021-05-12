package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeHelper {

    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;

    public Mono<ResponseEntity> currentEmployees(Integer page, Integer employeesPerPage, String employeeId, String subdivisionId, String divisionId){

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getAllEmployees(providerId)
                        .flatMap(employees -> this.commonHelper.getCurrentUser()
                                .flatMap(currentUser -> {
                                    if (currentUser instanceof EmployeeDetails) {
                                        long loggedUserId = ((EmployeeDetails) currentUser).getId();

                                        return this.getEmployees(employeeId, subdivisionId, divisionId, providerId, employees.stream()
                                                .filter(employee -> employee.getEmployeeId() != loggedUserId)
                                                .collect(Collectors.toList()))
                                                .flatMap(filtered -> this.filteredEmployees(filtered, page, employeesPerPage));

                                    } else {
                                        return this.getEmployees(employeeId, subdivisionId, divisionId, providerId, employees)
                                                .flatMap(filtered -> this.filteredEmployees(filtered, page, employeesPerPage));
                                    }
                                })
                        ));

    }

    public Mono<ResponseEntity> findEmployeeByName(String term) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getAllEmployeesByName(providerId, term))
                .flatMap(employees -> this.commonHelper.getCurrentUser()
                        .flatMap(currentUser -> {
                            if (currentUser instanceof EmployeeDetails) {
                                long loggedUserId = ((EmployeeDetails) currentUser).getId();


                                return filterEmployeeByName(employees.stream()
                                        .filter(employee -> employee.getEmployeeId() != loggedUserId)
                                        .collect(Collectors.toList()));


                            } else {
                                return this.filterEmployeeByName(employees);
                            }
                        }));


    }

    public Mono<ResponseEntity> showEmployee(long id) {

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> this.commonHelper.getCurrentUser()
                                .flatMap(userDetails -> {
                                            if (employee.getProviderId() == providerId) {
                                                if (userDetails instanceof EmployeeDetails) {
                                                    long loggedUserId = ((EmployeeDetails) userDetails).getId();

                                                    if (loggedUserId == employee.getEmployeeId()) {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                                    } else {
                                                        return this.loadEmployee(employee.getEmployeeId())
                                                                .flatMap(this::buildEmployeeEntity)
                                                                .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                                    }


                                                } else {
                                                    return this.loadEmployee(employee.getEmployeeId())
                                                            .flatMap(this::buildEmployeeEntity)
                                                            .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                                }

                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        }
                                )
                        )
                );

    }

    public Mono<ResponseEntity> createNewEmployee(Forms.NewEmployeeForm form) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                    var authClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/provider/authorities/" + providerId);

                    return authClient.get()
                            .retrieve()
                            .bodyToMono(ProviderAuthority[].class)
                            .flatMap(providerAuthorityArray -> {
                                        var providerAuthorities = Arrays.asList(providerAuthorityArray);

                                        return this.employeeService.getAllEmployees(providerId)
                                                .flatMap(employees -> {
                                                    List<EmployeeAuthority> authorities = new ArrayList<>();
                                                    boolean limitExceeded = false;

                                                    for (ProviderAuthority userAuthority : providerAuthorities) {
                                                        if (userAuthority.getAuthority().contains("ENTERPRISE"))
                                                            authorities.add(new EmployeeAuthority("ROLE_ENTERPRISE"));
                                                        if (userAuthority.getAuthority().contains("BUSINESS")) {
                                                            if (employees.size() > 19) {
                                                                limitExceeded = true;
                                                            } else {
                                                                authorities.add(new EmployeeAuthority("ROLE_BUSINESS"));
                                                            }
                                                        }
                                                        if (userAuthority.getAuthority().contains("ROLE_PRO")) {

                                                            if (employees.size() > 9) {
                                                                limitExceeded = true;
                                                            } else {
                                                                authorities.add(new EmployeeAuthority("ROLE_PRO"));
                                                            }

                                                        }
                                                    }

                                                    if (limitExceeded)
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("limitExceeded")));

                                                    form.authorizations.forEach(auth -> authorities.add(new EmployeeAuthority(auth)));
                                                    authorities.add(new EmployeeAuthority("SUBPROVIDER_ROSTER_VIEW"));

                                                    var employeeDetailsForm = new Forms.EmployeeRegistrationForm(form.email, form.password, providerId, authorities);

                                                    var registerClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/signup");

                                                    return registerClient.post()
                                                            .body(Mono.just(employeeDetailsForm), Forms.EmployeeRegistrationForm.class)
                                                            .retrieve()
                                                            .bodyToMono(String.class)
                                                            .flatMap(response -> {
                                                                if (response.equals("alreadyRegistered"))
                                                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("existingUser")));

                                                                var employeeId = Long.parseLong(response);

                                                                return this.setAuthorizedSchedules(form, providerId, null, false)
                                                                        .flatMap(authorizedSchedules -> this.setAuthorizedRosters(form, providerId)
                                                                                .flatMap(authorizedRosters -> this.setPersonalAuthorizedRosters(form.subdivisionId, providerId)
                                                                                        .flatMap(personalRosters -> {
                                                                                                    authorizedRosters.addAll(personalRosters);

                                                                                                    var timeOffBalance = new TimeOffBalance(employeeId, 0,0,0,0,0,0);
                                                                                                    var employee = Employee.builder()
                                                                                                            .employeeId(employeeId)
                                                                                                            .providerId(providerId)
                                                                                                            .name(form.name)
                                                                                                            .username(form.email)
                                                                                                            .jobTitle(form.jobTitle)
                                                                                                            .personalEmail(form.personalEmail)
                                                                                                            .timeOffBalance(timeOffBalance)
                                                                                                            .authorizedRosters(authorizedRosters)
                                                                                                            .authorizedSchedules(authorizedSchedules)
                                                                                                            .authorities(authorities.stream().map(EmployeeAuthority::getAuthority).collect(Collectors.toList()))
                                                                                                            .build();

                                                                                                    return persistSubdivision(form, providerId, employee, false);
                                                                                                }
                                                                                        )
                                                                                )
                                                                        )
                                                                        .cast(ResponseEntity.class)
                                                                        .onErrorResume(e -> {
                                                                            log.error("Error registering new employee, error: " + e.getMessage());
                                                                            return this.deleteEmployeeDetails(form.email, providerId, employeeId)
                                                                                    .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("newUserError"))));
                                                                        });

                                                            });


                                                });


                                    }
                            );


                });
    }

    public Mono<ResponseEntity> editEmployee(long id, Forms.NewEmployeeForm form) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> {

                                    if (employee.getProviderId() == providerId) {
                                        var authClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/provider/authorities/" + providerId);

                                        return authClient.get()
                                                .retrieve()
                                                .bodyToMono(ProviderAuthority[].class)
                                                .flatMap(providerAuthorityArray -> {
                                                    List<EmployeeAuthority> authorities = new ArrayList<>();

                                                    for (ProviderAuthority userAuthority : providerAuthorityArray) {
                                                        if (userAuthority.getAuthority().contains("ENTERPRISE"))
                                                            authorities.add(new EmployeeAuthority("ROLE_ENTERPRISE"));
                                                        if (userAuthority.getAuthority().contains("BUSINESS"))
                                                            authorities.add(new EmployeeAuthority("ROLE_BUSINESS"));
                                                        if (userAuthority.getAuthority().contains("ROLE_PRO"))
                                                            authorities.add(new EmployeeAuthority("ROLE_PRO"));
                                                    }


                                                    form.authorizations.forEach(auth -> authorities.add(new EmployeeAuthority(auth)));
                                                    authorities.add(new EmployeeAuthority("SUBPROVIDER_ROSTER_VIEW"));

                                                    List<String> auths = new ArrayList<>();
                                                    authorities.forEach(auth -> auths.add(auth.getAuthority()));

                                                    var authorityForm = new Forms.ListOfStringsForm(auths);

                                                    var updateAuthoritiesClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/authorities/update/" + id);

                                                    employee.setPersonalEmail(form.personalEmail);
                                                    employee.setName(form.name);
                                                    employee.setTaxPayerId(form.taxId);
                                                    employee.setAuthorities(auths);
                                                    return updateAuthoritiesClient.post()
                                                            .body(Mono.just(authorityForm), Forms.ListOfStringsForm.class)
                                                            .retrieve()
                                                            .bodyToMono(String.class)
                                                            .flatMap(response -> {
                                                                if (response.equals("ok")) {
                                                                    return this.setAuthorizedSchedules(form, providerId, id, form.allSchedules)
                                                                            .flatMap(authorizedSchedules -> this.setAuthorizedRosters(form, providerId)
                                                                                    .flatMap(authorizedRosters -> this.setPersonalAuthorizedRosters(form.subdivisionId, id)
                                                                                            .flatMap(personalRosters -> {
                                                                                                        authorizedRosters.addAll(personalRosters);
                                                                                                        return this.saveTimeOffBalance(id, form.getTimeOffBalance())
                                                                                                                .flatMap(balance -> {
                                                                                                                    employee.setTimeOffBalance(balance);
                                                                                                                    employee.setAuthorizedSchedules(authorizedSchedules);

                                                                                                                    if (!form.email.equalsIgnoreCase(employee.getUsername())) {
                                                                                                                        return this.isRegistered(form.email, id)
                                                                                                                                .flatMap(registered -> {
                                                                                                                                    if (registered) {
                                                                                                                                        return this.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                                .flatMap(emp -> this.persistSubdivision(form, providerId, emp, true))
                                                                                                                                                .flatMap(entity -> Mono.just(ResponseEntity.ok(new Forms.GenericResponse("existingUser"))));
                                                                                                                                    } else {
                                                                                                                                        employee.setUsername(form.email);
                                                                                                                                        return this.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                                .flatMap(emp -> this.persistSubdivision(form, providerId, emp, true));
                                                                                                                                    }
                                                                                                                                });
                                                                                                                    } else {
                                                                                                                        return this.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                .flatMap(emp -> this.persistSubdivision(form, providerId, emp, true));
                                                                                                                    }


                                                                                                                });

                                                                                                    }
                                                                                            )
                                                                                    )
                                                                            );

                                                                } else {
                                                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("editError")));
                                                                }
                                                            });
                                                });

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                    }

                                }
                        )
                );
    }



    public Mono<ResponseEntity> getListOfTimeRequests(long id){

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> this.commonHelper.getCurrentUser()
                                .flatMap(userDetails -> {
                                            if (employee.getProviderId() == providerId) {
                                                if (userDetails instanceof EmployeeDetails) {
                                                    long loggedUserId = ((EmployeeDetails) userDetails).getId();

                                                    if (loggedUserId == employee.getEmployeeId()) {
                                                        return Mono.just(ResponseEntity.ok(new ArrayList<>()));
                                                    } else {
                                                        return this.getTimeOff(employee.getEmployeeId());
                                                    }
                                                } else {
                                                    return this.getTimeOff(employee.getEmployeeId());
                                                }
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        }
                                )
                        )
                );
    }

    private Mono<? extends ResponseEntity> persistSubdivision(Forms.NewEmployeeForm form, Long providerId, Employee employee, boolean edit) {
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

    private Mono<ResponseEntity> setSubdivision(long subdivisionId, Employee employee) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                            subdivision.setDivision(division);
                            employee.setSubdivision(subdivision);

                            return this.persistEmployee(employee, true);
                        }));
    }

    private Mono<TimeOffBalance> saveTimeOffBalance(long employeeId, EmployeeEntity.TimeOffEntity timeOffBalance) {
        TimeOffBalance balance = new TimeOffBalance(employeeId, 0,0,0,0,0,0);
        Mono<TimeOffBalance> newBalance = this.employeeService.saveTimeOff(balance);

        return this.employeeService.getTimeOff(employeeId)
                .switchIfEmpty(Mono.defer(() -> newBalance))
                .flatMap(timeOff -> {
                    if (timeOffBalance != null) {
                        timeOff.setVacationDays(timeOffBalance.getVacationDays());
                        timeOff.setVacationRolloverDays(timeOffBalance.getVacationRolloverDays());
                        timeOff.setComplimentaryBankHolidayDays(timeOffBalance.getComplimentaryBankHolidayDays());
                        timeOff.setComplimentaryBankHolidayRolloverDays(timeOffBalance.getComplimentaryBankHolidayRolloverDays());
                        timeOff.setCompensationDays(timeOffBalance.getCompensationDays());
                        timeOff.setCompensationRolloverDays(timeOffBalance.getCompensationRolloverDays());
                        return this.employeeService.saveTimeOff(timeOff);
                    } else {
                        return Mono.just(timeOff);
                    }
                });
    }

    private Mono<Boolean> checkSubdivision(long providerId, long subdivisionId) {
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


    private Mono<Long> findSubdivision(long providerId, String divisionName, String subdivisionName) {
        return this.employeeService.getDivisionByName(providerId, divisionName)
                .switchIfEmpty(Mono.defer(() -> this.saveDivision(providerId, divisionName)))
                .flatMap(division -> this.employeeService.getSubdivisionByName(division.getDivisionId(), subdivisionName)
                        .switchIfEmpty(Mono.defer(() -> this.saveSubdivision(division.getDivisionId(), subdivisionName)))
                        .flatMap(subdivision -> Mono.just(subdivision.getSubdivisionId())));
    }

    private Mono<Division> saveDivision(long providerId, String name) {
        var division = new Division(providerId, name);
        return this.employeeService.saveDivision(division)
                .flatMap(savedDivision -> this.employeeService.getDivisionByName(providerId, name));
    }

    private Mono<Subdivision> saveSubdivision(long divisionId, String name) {
        var subdivision = new Subdivision(divisionId, name);
        return this.employeeService.saveSubdivision(subdivision)
                .flatMap(savedDivision -> this.employeeService.getSubdivisionByName(divisionId, name));
    }


    private Mono<String> deleteEmployeeDetails(String username, long providerId, long employeeId) {
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

    private Mono<ResponseEntity> persistEmployee(Employee emp, boolean edit) {

        if (edit) {
            return this.employeeService.saveEmployee(emp)
                    .then(this.buildEmployeeEntity(emp)
                            .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity))));
        } else {
            return this.employeeService.saveEmployee(emp)
                    .flatMap(e -> this.employeeService.getEmployeeByUsername(e.getProviderId(), e.getUsername()))
                    .flatMap(employee -> this.employeeService.saveTimeOff(emp.getTimeOffBalance()))
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

    private Mono<List<Long>> setPersonalAuthorizedRosters (Long subDivisionId, long providerId) {
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

//    private Mono<Subdivision> loadSubdivision(Long subDivisionId, long providerId) {
//        return this.employeeService.getSubdivision(subDivisionId)
//                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
//                        .flatMap(division -> {
//                                    if (division.getProviderId() == providerId) {
//                                        return Mono.just(subdivision);
//                                    } else {
//                                        return Mono.empty();
//                                    }
//                                }
//                        )
//                );
//    }

    private Mono<List<Long>> setAuthorizedRosters (Forms.NewEmployeeForm employeeForm, long providerId) {
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

    private Mono<List<Long>> setAuthorizedSchedules (Forms.NewEmployeeForm employeeForm, long providerId, Long employeeId, boolean deleteAll) {
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

    private Mono<ResponseEntity> getTimeOff(long id) {

        return this.employeeService.getTimeOffRequest(id)
                .flatMap(timeOffRequests -> Flux.fromIterable(timeOffRequests)
                        .flatMap(timeOffRequest -> {
                            var entity = new TimeRequestEntity(timeOffRequest);
                            var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl + "/upload//timeoff/attachments/" + timeOffRequest.getId());

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
                        })
                );
    }

    private Mono<ResponseEntity> filterEmployeeByName(List<Employee> employees) {
        return Flux.fromIterable(employees)
                .flatMap(this::buildShortEmployeeEntity)
                .collectList()
                .flatMap(employeeEntities -> Mono.just(ResponseEntity.ok(employeeEntities)));
    }

    private Mono<ResponseEntity> filteredEmployees(List<Employee> employees, Integer page, Integer employeesPerPage) {
        var map = new Forms.EmployeeMap();

        map.setTotal(employees.size());

        PagedListHolder<Employee> employeePagedListHolder=new PagedListHolder<>(employees);

        if (page==null){
            page=1;
        }

        int goToSubProvidersPage=page-1;

        if (goToSubProvidersPage<=employeePagedListHolder.getPageCount()&&goToSubProvidersPage>=0){
            employeePagedListHolder.setPage(goToSubProvidersPage);
        }

        employeePagedListHolder.setPageSize(1000);

        return Flux.fromIterable(employeePagedListHolder.getPageList())
                .flatMap(this::buildShortEmployeeEntity)
                .collectList()
                .flatMap(entities -> {
                    map.setEntities(entities);
                    return Mono.just(ResponseEntity.ok(map));
                });


    }

    private Mono<List<Employee>> getEmployees(String employeeId, String subdivisionId, String divisionId, long providerId, List<Employee> employees) {
        if (employeeId != null) {
            employees = employees.stream()
                    .filter(employee ->  employee.getEmployeeId() == Long.parseLong(employeeId))
                    .sorted(Comparator.comparing(Employee::getName,
                            Comparator.comparing(String::toLowerCase)))
                    .collect(Collectors.toList());
        }

        if (subdivisionId != null) {
            employees = employees.stream()
                    .filter(employee ->  employee.getSubdivisionId() != null && employee.getSubdivisionId() == Long.parseLong(subdivisionId))
                    .sorted(Comparator.comparing(Employee::getName,
                            Comparator.comparing(String::toLowerCase)))
                    .collect(Collectors.toList());
        }

        if (divisionId != null) {
            List<Employee> finalEmployees = employees;
            return employeeService.getDivision(Long.parseLong(divisionId))
                    .flatMap(division -> {

                        if (division.getProviderId() == providerId) {
                            List<Long> subdivisionsIds = new ArrayList<>();

                            division.getSubdivisions().forEach(
                                    subdivision -> {
                                        subdivisionsIds.add(subdivision.getSubdivisionId());
                                    }
                            );

                            return Mono.just(finalEmployees.stream()
                                    .filter(employee ->  employee.getSubdivisionId() !=null && subdivisionsIds.contains(employee.getSubdivisionId()))
                                    .sorted(Comparator.comparing(Employee::getName,
                                            Comparator.comparing(String::toLowerCase)))
                                    .collect(Collectors.toList()));
                        } else {
                            return Mono.just(new ArrayList<>());
                        }


                    });

        } else {
            return Mono.just(employees);
        }

    }

    private Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {

        List<Long> schedules = new ArrayList<>();

        if (employee.getAuthorizedSchedules() != null) {
            schedules.addAll(employee.getAuthorizedSchedules());
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


    private Mono<EmployeeEntity> buildShortEmployeeEntity(Employee employee) {
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

    private Mono<Employee> loadEmployee(long employeeId) {

        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/authorities/" + employeeId);

        return client.get()
                .retrieve()
                .bodyToMono(EmployeeAuthority[].class)
                .flatMap(array -> {
                    var auths = Arrays.asList(array);
                    List<String> authorities = new ArrayList<>();
                    auths.forEach(
                            employeeAuthority -> {
                                if (!employeeAuthority.getAuthority().equals("ROLE_PRO")
                                        || !employeeAuthority.getAuthority().equals("ROLE_BUSINESS")
                                        || !employeeAuthority.getAuthority().equals("ROLE_ENTERPRISE"))
                                    authorities.add(employeeAuthority.getAuthority());
                            }
                    );
                    return this.employeeService.getEmployee(employeeId)
                            .flatMap(employee -> this.employeeService.getFamily(employeeId)
                                    .flatMap(familyMembers -> {
                                        employee.setFamily(familyMembers);
                                        return Mono.just(employee);
                                    })
                            )
                            .flatMap(employee -> this.employeeService.getPhones(employeeId)
                                    .flatMap(phones -> {
                                        employee.setPhones(phones);
                                        return Mono.just(employee);
                                    })
                            )
                            .flatMap(employee -> this.employeeService.getAddress(employeeId)
                                    .flatMap(address -> {
                                        employee.setAddress(address);
                                        return Mono.just(employee);
                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> this.employeeService.getTimeOff(employeeId)
                                    .flatMap(timeOffBalance -> {
                                        employee.setTimeOffBalance(timeOffBalance);
                                        return Mono.just(employee);
                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> this.employeeService.getSubdivision(employee.getSubdivisionId())
                                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                            .flatMap(division -> {
                                                subdivision.setDivision(division);
                                                employee.setSubdivision(subdivision);
                                                return Mono.just(employee);
                                            }))
                                    .switchIfEmpty(Mono.defer(() -> Mono.just(employee)))
                            )
                            .flatMap(employee -> this.employeeService.getAuthorizedSchedules(employeeId)
                                    .flatMap(schedules -> {
                                        employee.setAuthorizedSchedules(schedules.stream().map(AuthorizedSchedule::getScheduleId).collect(Collectors.toList()));
                                        if (schedules.size()>0) {
                                            return this.setAuthorizedScheduleNames(employee, schedules.stream().map(AuthorizedSchedule::getScheduleId).collect(Collectors.toList()));
                                        } else {
                                            return Mono.just(employee);
                                        }


                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> this.employeeService.getAuthorizedRosters(employeeId)
                                    .flatMap(rosters ->  {
                                        List<Long> authorizedRosters = new ArrayList<>();
                                        rosters.forEach(roster -> authorizedRosters.add(roster.getRosterId()));
                                        employee.setAuthorizedRosters(authorizedRosters);
                                        return Mono.just(employee);
                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> {
                                employee.setAuthorities(authorities);
                                return Mono.just(employee);
                            });

                });



    }

    private Mono<Employee> setAuthorizedScheduleNames(Employee employee, List<Long> schedules) {
        List<String> ids = new ArrayList<>();
        schedules.forEach(schedule -> ids.add(Long.toString(schedule)));

        var webclient = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl + "/employee/schedules");
        return webclient.post()
                .body(Mono.just(new Forms.ListOfStringsForm(ids)), Forms.ListOfStringsForm.class)
                .retrieve()
                .bodyToMono(String[].class)
                .flatMap(scheduleList -> {
                    employee.setAuthorizedScheduleNames(Arrays.asList(scheduleList));
                    return Mono.just(employee);
                });
    }

    private Mono<Boolean> isRegistered(String username, long id) {

        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/registered/" + id +"?username=" + username);

        return client.get()
                .retrieve()
                .bodyToMono(Boolean.class);


    }

}
