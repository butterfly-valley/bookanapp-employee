package com.bookanapp.employee.controllers;


import com.bookanapp.employee.services.helpers.EmployeeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee")
@PreAuthorize("hasAuthority('ROLE_PRO') || hasAuthority('ROLE_BUSINESS') || hasAuthority('ROLE_ENTERPRISE')")
public class EmployeeController {

    private final EmployeeHelper employeeHelper;

    @GetMapping("/get")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> currentEmployees(@RequestParam("page") Integer page, @RequestParam("employeesPerPage") Integer employeesPerPage,
                                                           @RequestParam(value = "employeeId", required = false) String employeeId,
                                                           @RequestParam(value = "subdivisionId", required = false) String subdivisionId,
                                                           @RequestParam(value = "divisionId", required = false) String divisionId) {

        return this.employeeHelper.currentEmployees(page, employeesPerPage, employeeId, subdivisionId, divisionId);

    }

}
