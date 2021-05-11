package com.bookanapp.employee.controllers;


import com.bookanapp.employee.services.helpers.EmployeeHelper;
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

        return this.employeeHelper.currentEmployees(page, employeesPerPage, employeeId, subdivisionId, divisionId)
                .onErrorResume(e -> {
                    log.error("Error displaying employees for provider, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping("/search")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> findEmployeeByName(@RequestParam("term") String term) {

        return this.employeeHelper.findEmployeeByName(term)
                .onErrorResume(e -> {
                    log.error("Error searching employees by name for providerId, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping("/get/{id}")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> showEmployee(@PathVariable("id") long id) {

        return this.employeeHelper.showEmployee(id)
                .onErrorResume(e -> {
                    log.error("Error returning employee info, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @PostMapping("/create")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> addNewEmployee(@RequestBody @Valid Mono<Forms.NewEmployeeForm> employeeFormMono) {
          return employeeFormMono
                  .flatMap(this.employeeHelper::createNewEmployee)
                  .onErrorResume(e -> {
                      if (e instanceof WebExchangeBindException) {
                          return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                      } else {
                          log.error("Error registering new employee, error: " + e.getMessage());
                          return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("newUserError")));
                      }
                  })
;

    }

    @GetMapping(value="/get/time/list/{id}")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> getListOfTimeRequests(@PathVariable("id") long id) {

        return this.employeeHelper.getListOfTimeRequests(id)
                .onErrorResume(e -> {
                    log.error("Error returning employee timeoff requests, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

}
