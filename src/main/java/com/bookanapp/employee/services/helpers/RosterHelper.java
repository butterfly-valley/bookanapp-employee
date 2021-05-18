package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RosterHelper {
    private final RosterService rosterService;
    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final EmployeeHelper employeeHelper;

    public Mono<ResponseEntity> getAllEmployees() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getAllEmployees)
                .flatMap(employees -> Flux.fromIterable(employees)
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
                        .flatMap(list -> Mono.just(ResponseEntity.ok(list))));

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

    private Mono<ResponseEntity> getSearchedEmployeeEntities(List<Employee> employees, Long employeeId) {
        if (employeeId != null)
            employees = employees.stream().filter(employee -> !employee.getEmployeeId().equals(employeeId)).collect(Collectors.toList());

        return Flux.fromIterable(employees)
                .flatMap(employee -> this.employeeHelper.loadEmployee(employee.getEmployeeId())
                        .flatMap(this.employeeHelper::buildEmployeeEntity))
                .collectList()
                .flatMap(list -> {
                    var sortedEntities = list.stream().sorted(Comparator.comparing(EmployeeEntity::getName))
                            .collect(Collectors.toList());
                    return Mono.just(ResponseEntity.ok(sortedEntities));
                });
    }
}
