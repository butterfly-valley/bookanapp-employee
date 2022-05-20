package com.bookanapp.employee.controllers;

import com.bookanapp.employee.services.helpers.RosterHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("roster")
public class AnonymousRosterController {

    private final RosterHelper rosterHelper;

    @GetMapping(value = "/display", produces = "application/json")
    public Mono<ResponseEntity> displayRoster(@RequestParam("start") String start,
                                              @RequestParam("end") String end,
                                              @RequestParam("offset") String offset,
                                              @RequestParam(value = "employeeId", required = false) Long employeeId,
                                              @RequestParam(value = "subdivisionId", required = false) String subdivisionId,
                                              @RequestParam(value = "divisionId", required = false) String divisionId) {
        return this.rosterHelper.displayAnonymousRoster(start, end, offset, employeeId, subdivisionId, divisionId)
                .onErrorResume(e -> {
                    log.error("Error displaying roster, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping(value = "/employees", produces = "application/json")
    public Mono<ResponseEntity> displaySharedEmployees(@RequestParam(value = "employeeId", required = false) Long employeeId,
                                                       @RequestParam(value = "subdivisionId", required = false) Long subdivisionId,
                                                       @RequestParam(value = "divisionId", required = false) Long divisionId) {
        return this.rosterHelper.displaySharedEmployeesAnonymously(employeeId, subdivisionId, divisionId)
                .onErrorResume(e -> {
                    log.error("Error displaying employees, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }


}
