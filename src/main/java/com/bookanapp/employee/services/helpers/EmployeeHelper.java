package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
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
                                                .filter(employee -> employee.getId() != loggedUserId)
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
                                        .filter(employee -> employee.getId() != loggedUserId)
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

                                                    if (loggedUserId == employee.getId()) {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                                    } else {
                                                        return this.loadEmployee(employee.getId())
                                                                .flatMap(this::buildEmployeeEntity)
                                                                .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                                    }


                                                } else {
                                                    return this.loadEmployee(employee.getId())
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

                                                                return this.setAuthorizedSchedules(form, providerId)
                                                                        .flatMap(authorizedSchedules -> this.setAuthorizedRosters(form, providerId)
                                                                                .flatMap(authorizedRosters -> this.setPersonalAuthorizedRosters(form.subdivisionId, employeeId)
                                                                                        .flatMap(personalRosters -> {
                                                                                                    authorizedRosters.addAll(personalRosters);
                                                                                                    Subdivision subdivision = null;
                                                                                                    var timeOffBalance = new TimeOffBalance(employeeId, 0,0,0,0,0,0);
                                                                                                    var employee = Employee.builder()
                                                                                                            .id(employeeId)
                                                                                                            .providerId(providerId)
                                                                                                            .name(form.name)
                                                                                                            .username(form.email)
                                                                                                            .subdivision(subdivision)
                                                                                                            .jobTitle(form.jobTitle)
                                                                                                            .personalEmail(form.personalEmail)
                                                                                                            .timeOffBalance(timeOffBalance)
                                                                                                            .authorizedRosters(authorizedRosters)
                                                                                                            .authorizedSchedules(authorizedSchedules)
                                                                                                            .authorities(authorities.stream().map(EmployeeAuthority::getAuthority).collect(Collectors.toList()))
                                                                                                            .build();

                                                                                                    return this.persistNewEmployee(employee);
                                                                                                }
                                                                                        )
                                                                                )
                                                                        )
                                                                        .cast(ResponseEntity.class)
                                                                        .onErrorResume(e -> {
                                                                            log.error("Error registering new employee, error: " + e.getMessage());
                                                                            return this.deleteEmployeeDetails(employeeId)
                                                                                    .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("newUserError"))));
                                                                        });





                                                            });


                                                });


                                    }
                            );


                });
    }

    public Mono<ResponseEntity> getListOfTimeRequests(long id){

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> this.commonHelper.getCurrentUser()
                                .flatMap(userDetails -> {
                                            if (employee.getProviderId() == providerId) {
                                                if (userDetails instanceof EmployeeDetails) {
                                                    long loggedUserId = ((EmployeeDetails) userDetails).getId();

                                                    if (loggedUserId == employee.getId()) {
                                                        return Mono.just(ResponseEntity.ok(new ArrayList<>()));
                                                    } else {
                                                        return this.getTimeOff(employee.getId());
                                                    }
                                                } else {
                                                    return this.getTimeOff(employee.getId());
                                                }
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        }
                                )
                        )
                );




    }

    private Mono<String> deleteEmployeeDetails(long id) {
        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/delete/" + id);
        return client.get()
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> this.employeeService.getEmployee(id)
                        .switchIfEmpty(Mono.just(new Employee()))
                        .flatMap(employee -> {
                            if (employee.getId() != null) {
                                return this.employeeService.deleteEmployee(employee)
                                        .then(Mono.just("ok"));
                            } else {
                                return Mono.just("ok");
                            }
                        })

                );

    }

    private Mono<ResponseEntity> persistNewEmployee(Employee emp) {
        return this.employeeService.saveEmployee(emp)
                .flatMap(e -> this.employeeService.getEmployeeByUsername(e.getProviderId(), e.getUsername()))
                .flatMap(employee -> {
                    employee.getId();
                    return this.employeeService.saveTimeOff(emp.getTimeOffBalance());
                })
                .flatMap(timeOff -> {
                    List<AuthorizedSchedule> authorizedSchedules = new ArrayList<>();
                    emp.getAuthorizedSchedules().forEach(authorizedSchedule -> authorizedSchedules.add(new AuthorizedSchedule(emp.getId(), authorizedSchedule)));
                    return this.employeeService.saveAuthorizedSchedules(authorizedSchedules)
                            .flatMap(authorizedSchedList -> {
                                List<AuthorizedRoster> authorizedRosters = new ArrayList<>();
                                emp.getAuthorizedRosters().forEach(authorizedRoster -> authorizedRosters.add(new AuthorizedRoster(emp.getId(), authorizedRoster)));
                                return this.employeeService.saveAuthorizedRosters(authorizedRosters)
                                        .then(Mono.just(ResponseEntity.ok(this.buildShortEmployeeEntity(emp))));
                            });
                });
    }

    private Mono<List<Long>> setPersonalAuthorizedRosters (Long subDivisionId, long employeeId) {
        if (subDivisionId != null) {
            return this.employeeService.getSubdivision(employeeId)
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
    }

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

    private Mono<List<Long>> setAuthorizedSchedules (Forms.NewEmployeeForm employeeForm, long providerId) {
        if (employeeForm.availabilityIds!=null && employeeForm.availabilityIds.size()>0){

            return Flux.fromIterable(employeeForm.availabilityIds)
                    .flatMap(id -> {
                        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl +
                                "/schedule/get/" + id + "?providerId=" + providerId);
                        return client.get()
                                .retrieve()
                                .bodyToMono(Long.class);

                    })
                    .collectList()
                    .flatMap(list -> Mono.just(list.stream()
                            .filter(id -> id>0)
                            .collect(Collectors.toList())));
        } else {
            return Mono.just(new ArrayList<>());
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
                    .filter(employee ->  employee.getId() == Long.parseLong(employeeId))
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



        return this.loadEmployee(employee.getId())
                .flatMap(loadedEmployee -> {
                    List<Long> schedules = new ArrayList<>();

                    if (employee.getAuthorizedSchedules() != null) {
                        schedules.addAll(employee.getAuthorizedSchedules());
                    }



                    return Mono.just(EmployeeEntity.builder()
                            .id(loadedEmployee.getId())
                            .name(loadedEmployee.getName())
                            .authorities(employee.getAuthorities())
                            .authorizedSchedules(schedules)
                            .authorizedScheduleNames(employee.getAuthorizedScheduleNames())
                            .registerDate(loadedEmployee.getRegisterDate().toString())
                            .username(loadedEmployee.getUsername())
                            .avatar(loadedEmployee.getAvatar())
                            .subdivision(employee.getSubdivision() != null ? employee.getSubdivision().getName() : null)
                            .division(employee.getSubdivision() != null ? employee.getSubdivision().getDivision().getName() : null)
                            .subdivisionId(employee.getSubdivision() != null ? employee.getSubdivision().getSubdivisionId() : null)
                            .divisionId(employee.getSubdivision() != null && employee.getSubdivision().getDivision() != null ? employee.getSubdivision().getDivision().getDivisionId() : null)
                            .jobTitle(loadedEmployee.getJobTitle())
                            .authorizedRosters(loadedEmployee.getAuthorizedRosters())
                            .timeOffBalance(employee.getTimeOffBalance() != null ? new EmployeeEntity.TimeOffEntity(employee.getTimeOffBalance()) : null)
                            .homeAddress(loadedEmployee.getAddress())
                            .phones(loadedEmployee.getPhones())
                            .family(loadedEmployee.getFamily())
                            .bankAccount(loadedEmployee.getBankAccount())
                            .taxPayerId(loadedEmployee.getTaxPayerId())
                            .personalEmail(loadedEmployee.getPersonalEmail())
                            .build());
                });




    }


    private Mono<EmployeeEntity> buildShortEmployeeEntity(Employee employee) {
        if (employee.getSubdivisionId() != null) {
            return this.employeeService.getSubdivision(employee.getSubdivisionId())
                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                            .flatMap(division -> Mono.just(EmployeeEntity.builder()
                                    .id(employee.getId())
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
                    .id(employee.getId())
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
                                        employee.setAuthorizedSchedules(schedules.stream().map(AuthorizedSchedule::getId).collect(Collectors.toList()));
                                        if (schedules.size()>0) {
                                            List<String> ids = new ArrayList<>();
                                            schedules.forEach(schedule -> ids.add(Long.toString(schedule.getScheduleId())));

                                            var webclient = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl + "/employee/schedules");
                                            return webclient.post()
                                                    .body(Mono.just(new Forms.ListStringForm(ids)), Forms.ListStringForm.class)
                                                    .retrieve()
                                                    .bodyToMono(String[].class)
                                                    .flatMap(scheduleList -> {
                                                        employee.setAuthorizedScheduleNames(Arrays.asList(scheduleList));
                                                        return Mono.just(employee);
                                                    });

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



}
