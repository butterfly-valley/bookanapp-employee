package com.bookanapp.employee.controllers;

import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.services.helpers.CommonHelper;
import com.bookanapp.employee.services.helpers.EmployeeProfileHelper;
import com.bookanapp.employee.services.helpers.Forms;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee/profile")
@PreAuthorize("hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')" +
        " or hasAuthority('SUBPROVIDER_ROSTER_VIEW') or hasAuthority('SUBPROVIDER_ADMIN')" +
        " or hasAuthority('SUBPROVIDER_PAY') or hasAuthority('SUBPROVIDER_SCHED')" +
        " or hasAuthority('SUBPROVIDER_SCHED_VIEW')" )
public class EmployeeProfileController {

      private final EmployeeProfileHelper employeeProfileHelper;
    private final CommonHelper commonHelper;

    @GetMapping("/get")
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

    @PostMapping("/submit/time-off")
    public Mono<? extends ResponseEntity> submitTimeOff(@RequestBody @Valid Mono<Forms.TimeOffRequestForm> timeOffRequestFormMono) {
        return timeOffRequestFormMono
                .flatMap(this.employeeProfileHelper::submitTimeOff)
                .onErrorResume(e -> {
                    if (e instanceof WebExchangeBindException) {
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                    } else {
                      log.error("Error submitting time off request, error: " + e.getMessage());
                      return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
    }

    @PostMapping("/delete/time-off")
    public Mono<? extends ResponseEntity> deleteTimeOff(@RequestBody @Valid Mono<Forms.DeleteForm> deleteFormMono) {
        return deleteFormMono
                .flatMap(this.employeeProfileHelper::deleteTimeOff)
                .onErrorResume(e -> {
                    if (e instanceof WebExchangeBindException) {
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                    } else {
                        log.error("Error deleting time off request, error: " + e.getMessage());
                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
    }

    @PostMapping("/submit/time")
    public Mono<? extends ResponseEntity> submitAbsence(@RequestBody @Valid Mono<Forms.AbsenceRequestForm> absenceRequestFormMono) {
        return absenceRequestFormMono
                .flatMap(this.employeeProfileHelper::submitAbsence);
//                .onErrorResume(e -> {
//                    if (e instanceof WebExchangeBindException) {
//                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
//                    } else {
//                        log.error("Error submitting absence request, error: " + e.getMessage());
//                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//                    }
//                });
    }
}
