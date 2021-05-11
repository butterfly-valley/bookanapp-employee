package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.rest.EmployeeAuthority;
import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.entities.rest.TimeRequestEntity;
import com.bookanapp.employee.repositories.EmployeeTimeOffRepository;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
                        employee.getAuthorizedSchedules().forEach(authorizedSchedule -> schedules.add(authorizedSchedule.getScheduleId()));
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
                                        employee.setAuthorizedSchedules(schedules);
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
