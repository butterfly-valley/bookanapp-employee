package com.bookanapp.employee.controllers;


import com.bookanapp.employee.services.helpers.CommonHelper;
import com.bookanapp.employee.services.helpers.EmployeeHelper;
import com.bookanapp.employee.services.helpers.Forms;
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

@RestController
@CrossOrigin()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/employee")
@PreAuthorize("hasAuthority('ROLE_PRO') || hasAuthority('ROLE_BUSINESS') || hasAuthority('ROLE_ENTERPRISE')")
public class EmployeeController {

    private final EmployeeHelper employeeHelper;
    private final CommonHelper commonHelper;

    @GetMapping("/get")
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

    /*******
     *Returns available divisions
     ******/
    @GetMapping("/get/divisions")
    public Mono<? extends ResponseEntity> getDivisionList() {

        return this.employeeHelper.currentDivisions()
                .onErrorResume(e -> {
                    log.error("Error displaying divisions for provider for providerId, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping("/search")
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
                  .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error registering new employee, error: ", "newUserError"));

    }

    @PostMapping("/edit/{id}")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> editEmployee(@PathVariable("id") long id, @RequestBody @Valid Mono<Forms.NewEmployeeForm> employeeFormMono) {
        return employeeFormMono
                .flatMap(form -> this.employeeHelper.editEmployee(id, form))
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error editing employee, error: ", "editError"));

    }

    @PostMapping("/delete")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> deleteCustomer(@RequestBody @Valid Mono<Forms.DeleteForm> employeeFormMono) {
        return employeeFormMono
                .flatMap(this.employeeHelper::deleteEmployee)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error deleting employee, error: ", "deleteEmployeeError"));

    }

    @PostMapping("/division/update")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> updateDivision(@RequestBody @Valid Mono<Forms.DivisionForm> divisionFormMono) {
        return divisionFormMono
                .flatMap(this.employeeHelper::updateDivision)
                .onErrorResume(e -> this.commonHelper.returnErrorMessage(e, "Error updating division, error: ", "error"));

    }

    @GetMapping(value="/subdivision/all")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> loadAllSubdivisions() {

        return this.employeeHelper.loadAllSubdivisions()
                .onErrorResume(e -> {
                    log.error("Error returning list of subdivisions, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping(value="/get/time/list/{id}")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN') || hasAuthority('SUBPROVIDER_ROSTER')")
    public Mono<? extends ResponseEntity> getListOfTimeRequests(@PathVariable("id") long id) {

        return this.employeeHelper.getListOfTimeRequests(id)
                .onErrorResume(e -> {
                    log.error("Error returning employee timeoff requests, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @GetMapping(value="/get/schedules")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> showSchedules() {

        return this.employeeHelper.showSchedules()
                .onErrorResume(e -> {
                    log.error("Error showing schedules, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }

    @PostMapping(value = "/upload/image/{id}", produces="application/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity> uploadAvatar(@PathVariable("id") long id, @RequestPart("fileKey") Mono<FilePart> file) {
        return this.employeeHelper.uploadPhoto(id, file)
                .onErrorResume(e -> {
                    log.error("Error saving image for employeeId: " + id + ", error: " + e.getMessage());
                    return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, "imageUploadError")));
                });
    }

    @GetMapping(value="/delete/image/{id}")
    @PreAuthorize( "hasAuthority('PROVIDER')" +
            " || hasAuthority('SUBPROVIDER_FULL') || hasAuthority('SUBPROVIDER_ADMIN')")
    public Mono<? extends ResponseEntity> deleteAvatar(@PathVariable("id") long id) {

        return this.employeeHelper.deleteAvatar(id, true)
                .onErrorResume(e -> {
                    log.error("Error deleting employee avatar, error: " + e.getMessage());
                    return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                });

    }




}
