package com.bookanapp.employee.controllers;

import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.services.helpers.CommonHelper;
import com.bookanapp.employee.services.helpers.EmployeeHelper;
import com.bookanapp.employee.services.helpers.EmployeeProfileHelper;
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

      private final EmployeeProfileHelper employeeProfileHelper;
    private final CommonHelper commonHelper;

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')" +
            " or hasAuthority('SUBPROVIDER_ROSTER_VIEW') or hasAuthority('SUBPROVIDER_ADMIN')" +
            " or hasAuthority('SUBPROVIDER_PAY') or hasAuthority('SUBPROVIDER_SCHED')" +
            " or hasAuthority('SUBPROVIDER_SCHED_VIEW')" )
    public Mono<? extends ResponseEntity> getAllInfo() {

        return this.commonHelper.getCurrentUser()
                .flatMap(userDetails -> {
                    if (userDetails instanceof EmployeeDetails) {
                        return this.employeeProfileHelper.loadEmployee(((EmployeeDetails) userDetails).getId())
                                .flatMap(this.employeeProfileHelper::buildEmployeeEntity)
                                .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                    } else {
                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error displaying profile info for employee, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }
}
