package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.AuthorizedSchedule;
import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.rest.EmployeeAuthority;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeProfileHelper {

    public EmployeeHelper employeeHelper;
    public CommonHelper commonHelper;
    private final EmployeeService employeeService;

    public Mono<Employee> loadEmployee(long employeeId) {
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
                            .flatMap(employee -> {
                                        if (employee.getSubdivision() != null) {
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

    public Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {



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
                .timeOffBalance(employee.getTimeOffBalance() != null ? new EmployeeEntity.TimeOffEntity(employee.getTimeOffBalance()) : null)
                .homeAddress(employee.getAddress())
                .phones(employee.getPhones())
                .family(employee.getFamily())
                .bankAccount(employee.getBankAccount())
                .taxPayerId(employee.getTaxPayerId())
                .personalEmail(employee.getPersonalEmail())
                .build());


    }

}
