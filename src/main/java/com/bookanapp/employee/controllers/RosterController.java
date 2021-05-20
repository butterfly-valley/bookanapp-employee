package com.bookanapp.employee.controllers;

import com.bookanapp.employee.services.helpers.CommonHelper;
import com.bookanapp.employee.services.helpers.Forms;
import com.bookanapp.employee.services.helpers.RosterHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee/roster")
@PreAuthorize("hasAuthority('ROLE_PRO') or hasAuthority('ROLE_BUSINESS')  or hasAuthority('ROLE_ENTERPRISE')")
public class RosterController {
    private final RosterHelper rosterHelper;
    private final CommonHelper commonHelper;

    /**
     * Returns list of employees based on query params for authenticated users
     * @param employeeId
     * @param subdivisionId
     * @param divisionId
     * @return
     */
    @GetMapping("/employees")
    public Mono<? extends ResponseEntity> displaySharedEmployees(@RequestParam(value = "employeeId", required = false) Long employeeId,
                                                          @RequestParam(value = "subdivisionId", required = false) Long subdivisionId,
                                                          @RequestParam(value = "divisionId", required = false) Long divisionId) {
        return this.rosterHelper.displaySharedEmployees(employeeId, subdivisionId, divisionId)
                .onErrorResume(e -> {
                    log.error("Error displaying employees, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping("/get/all")
    public Mono<? extends ResponseEntity> getAllEmployees() {
        return this.rosterHelper.getAllEmployees()
                .onErrorResume(e -> {
                    log.error("Error displaying employees, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping(value = "/display", produces = "application/json")
    public Mono<ResponseEntity> displayRoster(@RequestParam("start") String start,
                                           @RequestParam("end") String end,
                                           @RequestParam("offset") String offset,
                                           @RequestParam(value = "employeeId", required = false) Long employeeId,
                                           @RequestParam(value = "subdivisionId", required = false) String subdivisionId,
                                           @RequestParam(value = "divisionId", required = false) String divisionId,
                                           @RequestParam(value = "all", required = false) String all,
                                           @RequestParam(value = "showTimeOff", required = false) String showTimeOff) {
        return this.rosterHelper.displayRoster(start, end, offset, employeeId, subdivisionId, divisionId, all, showTimeOff)
                .onErrorResume(e -> {
                    log.error("Error displaying roster, error: " + e.getMessage());
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

    @PostMapping("/upload")
    public Mono<? extends ResponseEntity> uploadRoster(@RequestBody @Valid Mono<Forms.RosterForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::uploadRoster)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error uploading employee roster", "error"));
    }

    @PostMapping("/upload/subdivision")
    public Mono<? extends ResponseEntity> uploadSubdivisionRoster(@RequestBody @Valid Mono<Forms.SubdivisionRosterForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::uploadSubdivisionRoster);
//                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error uploading subdivision roster", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @GetMapping(value = "/get/colors")
    public Mono<? extends ResponseEntity> getRosterSlots(){
        return this.rosterHelper.getRosterPatternsAndColors()
                .onErrorResume(e -> {
                    log.error("Error displaying roster colors and slots, error: " + e.getMessage());
                    return Mono.just(ResponseEntity.ok(new ArrayList()));
                });

    }

}
