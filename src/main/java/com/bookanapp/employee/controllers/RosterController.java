package com.bookanapp.employee.controllers;

import com.bookanapp.employee.services.helpers.CommonHelper;
import com.bookanapp.employee.services.helpers.Forms;
import com.bookanapp.employee.services.helpers.RosterHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.ArrayList;

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee/roster")
@PreAuthorize("hasAuthority('PROVIDER')" +
        " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER') or hasAuthority('SUBPROVIDER_ROSTER_VIEW') or hasAuthority('SUBPROVIDER_ADMIN')" )
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

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/upload")
    public Mono<? extends ResponseEntity> uploadRoster(@RequestBody @Valid Mono<Forms.RosterForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::uploadRoster)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error uploading employee roster", "uploadRosterError"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/upload/range")
    public Mono<? extends ResponseEntity> uploadRosterRange(@RequestBody @Valid Mono<Forms.RosterRangeForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::uploadRangeRoster)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error uploading employee roster", "uploadRosterError")).log();
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/upload/range/subdivision")
    public Mono<? extends ResponseEntity> uploadRosterRangeSubdivision(@RequestBody @Valid Mono<Forms.SubdivisionRosterRangeForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::uploadRangeRosterDivision)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error uploading division roster", "uploadRosterError")).log();
    }



    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/upload/subdivision")
    public Mono<? extends ResponseEntity> uploadSubdivisionRoster(@RequestBody @Valid Mono<Forms.SubdivisionRosterForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::uploadSubdivisionRoster)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error uploading subdivision roster", "uploadRosterError"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/update/slot")
    public Mono<? extends ResponseEntity> updateRosterSlot(@RequestBody @Valid Mono<Forms.RosterSlotForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::updateRosterSlot)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error updating roster slot", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/paste/slot")
    public Mono<? extends ResponseEntity> pasteRosterSlot(@RequestBody @Valid Mono<Forms.PasteRosterSlotForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::pasteRosterSlot)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error pasting roster slot", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/delete/slot")
    public Mono<? extends ResponseEntity> deleteRosterSlot(@RequestBody @Valid Mono<Forms.DeleteRosterSlotForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::deleteRosterSlot)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error deleting roster slots", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/publish")
    public Mono<? extends ResponseEntity> publishRoster(@RequestBody @Valid Mono<Forms.DeleteOrPublishRosterSlotForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(this.rosterHelper::publishRoster)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error deleting or publishing roster", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/approve/time-off")
    public Mono<? extends ResponseEntity> approveTimeOff(@RequestBody @Valid Mono<Forms.DeleteForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(form -> this.rosterHelper.approveTimeOff(form, false))
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error approving time off", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @PostMapping("/deny/time-off")
    public Mono<? extends ResponseEntity> denyTimeOff(@RequestBody @Valid Mono<Forms.DeleteForm> rosterFormMono) {
        return rosterFormMono
                .flatMap(form -> this.rosterHelper.approveTimeOff(form, true))
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error denying time off", "error"));
    }

    @PreAuthorize("hasAuthority('PROVIDER')" +
            " or hasAuthority('SUBPROVIDER_FULL') or hasAuthority('SUBPROVIDER_ROSTER')")
    @GetMapping("/approve/absence")
    public Mono<? extends ResponseEntity> approveAbsence(@RequestParam("requestId") String requestId, @RequestParam(required = false, name = "deny") String deny) {
        return  this.rosterHelper.approveAbsence(requestId, deny)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error approving absence", "error"));
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
