package com.bookanapp.employee.controllers;

import com.bookanapp.employee.services.helpers.Forms;
import com.bookanapp.employee.services.helpers.RosterHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee/roster")
@PreAuthorize("hasAuthority('ROLE_PRO') or hasAuthority('ROLE_BUSINESS')  or hasAuthority('ROLE_ENTERPRISE')")
public class RosterController {
    private final RosterHelper rosterHelper;

    @GetMapping("/get/all")
    public Mono<? extends ResponseEntity> getAllEmployees() {
        return this.rosterHelper.getAllEmployees()
                .onErrorResume(e -> {
                    log.error("Error displaying employees, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping("/search")
    public Mono<? extends ResponseEntity> findEmployeeByName(@RequestParam("term") String term) {
        return this.rosterHelper.findEmployeeByName(term)
                .onErrorResume(e -> {
                    log.error("Error searching employee list by term, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

}
