package com.bookanapp.employee.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee/profile")
@PreAuthorize("hasAuthority('ROLE_PRO') || hasAuthority('ROLE_BUSINESS') || hasAuthority('ROLE_ENTERPRISE')")
public class EmployeeProfileController {

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')" +
            " or hasAuthority('SUBPROVIDER_ROSTER_VIEW') or hasAuthority('SUBPROVIDER_ADMIN')" +
            " or hasAuthority('SUBPROVIDER_PAY') or hasAuthority('SUBPROVIDER_SCHED')" +
            " or hasAuthority('SUBPROVIDER_SCHED_VIEW')" )
    public Mono<? extends ResponseEntity> getAllInfo() {
        SubProvider employee = this.getLoggedEmployee();
        try {

            if (employee != null) {
                return ResponseEntity.ok(this.employeeControllerHelper.buildEntity(employee));
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }


        }catch (Exception e) {
            log.error("Error displaying profile info for employee, error: " + FullStackConverter.fullStack(e));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
