package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.Division;
import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.Subdivision;
import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
                .flatMap(providerId -> this.employeeService.getAllEmployees(providerId)

                        .flatMap(currentUser -> {
                            if (currentUser instanceof EmployeeDetails) {
                                long loggedUserId = ((EmployeeDetails) currentUser).getId();


                                return this.employeeService.getAllEmployeesByName(providerId, term)
                                        .flatMap(employees -> filterEmployeeByName(employees.stream()
                                                .filter(employee -> employee.getId() != loggedUserId)
                                                .collect(Collectors.toList())));


                            } else {
                                return this.employeeService.getAllEmployeesByName(providerId, term)
                                        .flatMap(this::filterEmployeeByName);

                            }
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



}
