package com.bookanapp.employee.controllers;

import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.services.helpers.CommonHelper;
import com.bookanapp.employee.services.helpers.EmployeeProfileHelper;
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

    @PostMapping(value = "/edit")
    public Mono<? extends ResponseEntity> editProfileInfo(@RequestBody @Valid Mono<Forms.ProfileEditForm> profileEditFormMono) {
        return profileEditFormMono
                .flatMap(this.employeeProfileHelper::editProfile);
//                .onErrorResume(e -> {
//                    if (e instanceof WebExchangeBindException) {
//                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
//                    } else {
//                        log.error("Error editing profile, error: " + e.getMessage());
//                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//                    }
//                });

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
                .flatMap(this.employeeProfileHelper::submitAbsence)
                .onErrorResume(e -> {
                    if (e instanceof WebExchangeBindException) {
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                    } else {
                        log.error("Error submitting absence request, error: " + e.getMessage());
                        return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
    }

    @GetMapping("/get/time/list")
    public Mono<? extends ResponseEntity> getListOfTimeRequests() {

        return this.employeeProfileHelper.getListOfAbsencesOrOvertime()
                .onErrorResume(e -> {
                    log.error("Error displaying time request list, error: " + e.getMessage());
                    return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, "imageUploadError")));
                });

    }

    @PostMapping(value = "/submit/time/attachment/{id}", produces="application/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<? extends ResponseEntity> uploadAttachment(@PathVariable("id") String id, @RequestPart("fileKey") Mono<FilePart> file) {
        return this.employeeProfileHelper.uploadAttachment(id, file);
//                .onErrorResume(e -> {
//                    log.error("Error saving attachment for absence request: " + id + ", error: " + e.getMessage());
//                    return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, "imageUploadError")));
//                });

    }

    @GetMapping(value = "/get/time/attachment/{requestId}")
    public Mono<? extends ResponseEntity> downloadAttachment( @PathVariable("requestId") String requestId, @RequestParam("key") String key) {
        return this.employeeProfileHelper.downloadAttachment(requestId, key);
//                .onErrorResume(e -> {
//                    log.error("Error saving attachment for absence request: " + id + ", error: " + e.getMessage());
//                    return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, "imageUploadError")));
//                });

    }

    @GetMapping(value = "/delete/time/attachment/{requestId}")
    public Mono<? extends ResponseEntity> deleteAttachment( @PathVariable("requestId") String requestId, @RequestParam("key") String key) {
        return this.employeeProfileHelper.deleteAttachment(requestId, key);
//                .onErrorResume(e -> {
//                    log.error("Error saving attachment for absence request: " + id + ", error: " + e.getMessage());
//                    return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, "imageUploadError")));
//                });

    }
}
