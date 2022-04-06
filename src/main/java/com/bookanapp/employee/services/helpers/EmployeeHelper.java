package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeHelper {

    private final CommonHelper commonHelper;
    public final EmployeeService employeeService;

    public Mono<ResponseEntity> currentEmployees(Integer page, Integer employeesPerPage, String employeeId, String subdivisionId, String divisionId){

//        var pageRequest = PageRequest.of(page, employeesPerPage);

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getAllEmployees(providerId)
                        .flatMap(employees ->  this.getEmployees(employeeId, subdivisionId, divisionId, providerId, employees)
                                .flatMap(filtered -> this.filteredEmployees(filtered, page, employeesPerPage))));


    }

    public Mono<ResponseEntity> currentDivisions(){

        var divisionEntityList = this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getDivisions)
                .flatMap(divisions -> Flux.fromIterable(divisions)
                        .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                                .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                        .flatMap(subdivision -> Mono.just(new Forms.SubdivisionList(subdivision.getName(), subdivision.getSubdivisionId())))
                                        .collectList()
                                        .flatMap(subdivisionLists -> Mono.just(new Forms.DivisionEntity(division.getName(), division.getDivisionId(), subdivisionLists)))
                                ))
                        .collectList()

                );

        var unassignedEmployees = this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::findAllUnassignedEmployees)
                .flatMap(employees -> {
                    if (employees.size()>0) {
                        return  Mono.just(new Forms.DivisionEntity(null, null, new ArrayList<>()));
                    } else {
                        return Mono.empty();
                    }
                });

        return divisionEntityList.
                flatMap(divisionEntities -> unassignedEmployees
                        .flatMap(divisionEntity -> {
                            divisionEntities.add(divisionEntity);
                            return Mono.just(ResponseEntity.ok(divisionEntities));
                        })
                        .switchIfEmpty(Mono.just(ResponseEntity.ok(divisionEntities)))
                );

    }

//    public Mono<ResponseEntity> getListOfTimeRequests(long id){
//
//        return this.commonHelper.getCurrentProviderId()
//                .flatMap(providerId -> this.employeeService.getEmployee(id)
//                        .flatMap(employee -> {
//                            if (employee.getProviderId() == providerId) {
//
//                            } else {
//                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
//
//                            }
//
//                            return null;
//                        }));
//
//
//
//    }

    public Mono<ResponseEntity> findEmployeeByName(String term) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getAllEmployeesByName(providerId, term))
                .flatMap(this::filterEmployeeByName);
    }

    public Mono<ResponseEntity> showEmployee(long id) {

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> this.commonHelper.getCurrentUser()
                                .flatMap(userDetails -> {
                                            if (employee.getProviderId() == providerId) {
                                                if (userDetails instanceof EmployeeDetails) {
                                                    long loggedUserId = ((EmployeeDetails) userDetails).getId();

                                                    if (loggedUserId == employee.getEmployeeId()) {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                                    } else {
                                                        return this.loadEmployee(employee.getEmployeeId())
                                                                .flatMap(this::buildEmployeeEntity)
                                                                .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                                    }
                                                } else {
                                                    return this.loadEmployee(employee.getEmployeeId())
                                                            .flatMap(this::buildEmployeeEntity)
                                                            .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                                }

                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        }
                                )
                        )
                );

    }

    public Mono<ResponseEntity> createNewEmployee(Forms.NewEmployeeForm form) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                    var authClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/provider/authorities/" + providerId);

                    return authClient.get()
                            .retrieve()
                            .bodyToMono(ProviderAuthority[].class)
                            .flatMap(providerAuthorityArray -> {
                                        var providerAuthorities = Arrays.asList(providerAuthorityArray);

                                        return this.employeeService.getAllEmployees(providerId)
                                                .flatMap(employees -> {
                                                    List<EmployeeAuthority> authorities = new ArrayList<>();
                                                    boolean limitExceeded = false;

                                                    for (ProviderAuthority userAuthority : providerAuthorities) {
                                                        if (userAuthority.getAuthority().contains("ENTERPRISE"))
                                                            authorities.add(new EmployeeAuthority("ROLE_ENTERPRISE"));
                                                        if (userAuthority.getAuthority().contains("BUSINESS")) {
                                                            if (employees.size() > 19) {
                                                                limitExceeded = true;
                                                            } else {
                                                                authorities.add(new EmployeeAuthority("ROLE_BUSINESS"));
                                                            }
                                                        }
                                                        if (userAuthority.getAuthority().contains("ROLE_PRO")) {

                                                            if (employees.size() > 9) {
                                                                limitExceeded = true;
                                                            } else {
                                                                authorities.add(new EmployeeAuthority("ROLE_PRO"));
                                                            }

                                                        }
                                                    }

                                                    if (limitExceeded)
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("limitExceeded")));

                                                    form.authorizations.forEach(auth -> authorities.add(new EmployeeAuthority(auth)));
                                                    authorities.add(new EmployeeAuthority("SUBPROVIDER_ROSTER_VIEW"));

                                                    var employeeDetailsForm = new Forms.EmployeeRegistrationForm(form.email, form.password, providerId, authorities);

                                                    var registerClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/signup");

                                                    return registerClient.post()
                                                            .body(Mono.just(employeeDetailsForm), Forms.EmployeeRegistrationForm.class)
                                                            .retrieve()
                                                            .bodyToMono(String.class)
                                                            .flatMap(response -> {
                                                                if (response.equals("alreadyRegistered"))
                                                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("existingUser")));

                                                                var employeeId = Long.parseLong(response);

                                                                return this.setAuthorizedSchedules(form, providerId, null, false)
                                                                        .flatMap(authorizedSchedules -> this.setAuthorizedRosters(form, providerId)
                                                                                .flatMap(authorizedRosters -> this.setPersonalAuthorizedRosters(form.subdivisionId, providerId)
                                                                                        .flatMap(personalRosters -> {
                                                                                                    authorizedRosters.addAll(personalRosters);

                                                                                                    var timeOffBalance = new TimeOffBalance(employeeId, 0,0,0,0,0,0);
                                                                                                    var employee = Employee.builder()
                                                                                                            .employeeId(employeeId)
                                                                                                            .providerId(providerId)
                                                                                                            .name(form.name)
                                                                                                            .username(form.email)
                                                                                                            .jobTitle(form.jobTitle)
                                                                                                            .personalEmail(form.personalEmail)
                                                                                                            .timeOffBalance(timeOffBalance)
                                                                                                            .authorizedRosters(authorizedRosters)
                                                                                                            .authorizedSchedules(authorizedSchedules)
                                                                                                            .authorities(authorities.stream().map(EmployeeAuthority::getAuthority).collect(Collectors.toSet()))
                                                                                                            .build();

                                                                                                    return persistSubdivision(form, providerId, employee, false);
                                                                                                }
                                                                                        )
                                                                                )
                                                                        )
                                                                        .cast(ResponseEntity.class)
                                                                        .onErrorResume(e -> {
                                                                            log.error("Error registering new employee, error: " + e.getMessage());
                                                                            return this.deleteEmployeeDetails(form.email, providerId, employeeId)
                                                                                    .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("newUserError"))));
                                                                        });
                                                            });

                                                });
                                    }
                            );


                });
    }

    public Mono<ResponseEntity> editEmployee(long id, Forms.NewEmployeeForm form) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> {
                                    if (employee.getProviderId() == providerId) {
                                        List<EmployeeAuthority> authorities = new ArrayList<>();
                                        form.authorizations.forEach(auth -> authorities.add(new EmployeeAuthority(auth)));
                                        Set<String> auths = new HashSet<>();
                                        authorities.forEach(auth -> auths.add(auth.getAuthority()));

                                        var authorityForm = new Forms.SetOfStringsForm(auths);

                                        var updateAuthoritiesClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/authorities/update/" + id);

                                        employee.setPersonalEmail(form.personalEmail);
                                        employee.setName(form.name);
                                        employee.setTaxPayerId(form.taxId);
                                        employee.setAuthorities(auths);
                                        return updateAuthoritiesClient.post()
                                                .body(Mono.just(authorityForm), Forms.SetOfStringsForm.class)
                                                .retrieve()
                                                .bodyToMono(String.class)
                                                .flatMap(response -> {
                                                    if (response.equals("ok")) {
                                                        return this.setAuthorizedSchedules(form, providerId, id, form.allSchedules)
                                                                .flatMap(authorizedSchedules -> this.setAuthorizedRosters(form, providerId)
                                                                        .flatMap(authorizedRosters -> this.setPersonalAuthorizedRosters(form.subdivisionId, id)
                                                                                .flatMap(personalRosters -> {
                                                                                            authorizedRosters.addAll(personalRosters);
                                                                                            return this.saveTimeOffBalance(id, form.getTimeOffBalance())
                                                                                                    .flatMap(balance -> {
                                                                                                        employee.setTimeOffBalance(balance);
                                                                                                        employee.setAuthorizedSchedules(authorizedSchedules);

                                                                                                        if (!form.email.equalsIgnoreCase(employee.getUsername())) {
                                                                                                            return this.isRegistered(form.email, id)
                                                                                                                    .flatMap(registered -> {
                                                                                                                        if (registered) {
                                                                                                                            return this.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                    .flatMap(emp -> this.persistSubdivision(form, providerId, emp, true))
                                                                                                                                    .flatMap(entity -> Mono.just(ResponseEntity.ok(new Forms.GenericResponse("existingUser"))));
                                                                                                                        } else {
                                                                                                                            employee.setUsername(form.email);
                                                                                                                            return this.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                    .flatMap(emp -> this.persistSubdivision(form, providerId, emp, true));
                                                                                                                        }
                                                                                                                    });
                                                                                                        } else {
                                                                                                            return this.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                    .flatMap(emp -> this.persistSubdivision(form, providerId, emp, true));
                                                                                                        }
                                                                                                    });
                                                                                        }
                                                                                )
                                                                        )
                                                                );
                                                    } else {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("editError")));
                                                    }
                                                });

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                    }
                                }
                        )
                );
    }



    public Mono<ResponseEntity> getListOfTimeRequests(long id){

        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> this.commonHelper.getCurrentUser()
                                .flatMap(userDetails -> {
                                            if (employee.getProviderId() == providerId) {
                                                if (userDetails instanceof EmployeeDetails) {
                                                    long loggedUserId = ((EmployeeDetails) userDetails).getId();

                                                    if (loggedUserId == employee.getEmployeeId()) {
                                                        return Mono.just(ResponseEntity.ok(new ArrayList<>()));
                                                    } else {
                                                        return this.getTimeOff(employee.getEmployeeId());
                                                    }
                                                } else {
                                                    return this.getTimeOff(employee.getEmployeeId());
                                                }
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        }
                                )
                        )
                );
    }


    public Mono<ResponseEntity> deleteEmployee(@Valid @RequestBody Forms.DeleteForm deleteForm){


        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(deleteForm.idsToDelete)
                        .flatMap(id -> this.employeeService.getEmployee(Long.parseLong(id)))
                        .flatMap(employee -> {
                            if (employee.getProviderId() == providerId) {
                                if (employee.getAvatar() != null) {
                                    String[] splitLink=employee.getAvatar().split("avatar/");
                                    var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl
                                            + "/upload/delete?=bucket=bookanapp-provider-employees&link=" + "employee-id-" + employee.getEmployeeId() + "/avatar/"+splitLink[1]);
                                    return client.get()
                                            .retrieve()
                                            .bodyToMono(String.class)
                                            .flatMap(response ->  this.deleteEmployeeDetails(employee));
                                } else {
                                    return this.deleteEmployeeDetails(employee);
                                }
                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                            }
                        })
                        .collectList()
                        .flatMap(list -> {
                            if (list.contains("invalidEmployee")) {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("deleteEmployeeSuccess")));
                            }
                        }));

    }

    public Mono<ResponseEntity> showSchedules() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                    var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl + "/employee/schedules/show/" + providerId);
                    return client.get()
                            .retrieve()
                            .bodyToMono(ScheduleEntity[].class)
                            .flatMap(response -> Mono.just(ResponseEntity.ok(Arrays.asList(response))));
                });

    }

    public Mono<ResponseEntity> deleteAvatar(long id, boolean deleteEmployeeAvatar) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(id)
                        .flatMap(employee -> {
                            if (employee.getProviderId() == providerId) {
                                if (employee.getAvatar() != null) {
                                    String[] splitLink=employee.getAvatar().split("avatar/");
                                    var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl
                                            + "/upload/delete?=bucket=bookanapp-provider-employees&link="+"provider-id-" + providerId  + "employee-id-" + employee.getEmployeeId() + "/avatar/"+splitLink[1]);
                                    return client.get()
                                            .retrieve()
                                            .bodyToMono(String.class)
                                            .flatMap(response -> {
                                                if (deleteEmployeeAvatar) {
                                                    employee.setAvatar(null);
                                                    return this.employeeService.saveEmployee(employee)
                                                            .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("avatarDeleteSuccess"))));
                                                } else {
                                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("avatarDeleteSuccess")));
                                                }

                                            });
                                } else {
                                    if (deleteEmployeeAvatar) {
                                        employee.setAvatar(null);
                                        return this.employeeService.saveEmployee(employee)
                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("avatarDeleteSuccess"))));
                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("avatarDeleteSuccess")));
                                    }
                                }

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                            }
                        }));
    }

    public Mono<ResponseEntity> updateDivision(Forms.DivisionForm form) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getDivision(form.divisionId)
                        .flatMap(division -> {
                            if (division.getProviderId() == providerId) {
                                return this.employeeService.getSubdivision(form.subdivisionId)
                                        .flatMap(subdivision -> {
                                            if (subdivision.getDivisionId() == division.getDivisionId()) {
                                                division.setName(form.divisionName);
                                                subdivision.setName(form.subdivisionName);
                                                return this.employeeService.saveDivision(division)
                                                        .then(this.employeeService.saveSubdivision(subdivision))
                                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalid-division")));
                                            }
                                        });
                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalid-division")));
                            }
                        })
                );
    }

    public Mono<ResponseEntity> loadAllSubdivisions() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getDivisions)
                .flatMap(divisions -> Flux.fromIterable(divisions)
                        .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                                .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                        .flatMap(subdivision -> Mono.just(new SubdivisionEntity(division.getName(), subdivision.getName(), division.getDivisionId(), subdivision.getSubdivisionId())))
                                        .collectList()
                                )
                        )
                        .collectList()
                )
                .flatMap(lists -> {
                    List<SubdivisionEntity> entities = new ArrayList<>();
                    lists.forEach(entities::addAll);
                    return Mono.just(ResponseEntity.ok(entities));
                });


    }

    public Mono<ResponseEntity> uploadPhoto(long employeeId, Mono<FilePart> file) {


        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(employeeId)
                        .flatMap(employee -> {
                            if (employee.getProviderId() == providerId) {

                                return file
                                        .flatMap(filePart -> {
                                            MultipartBodyBuilder builder = new MultipartBodyBuilder();
                                            builder.part("file", filePart)
                                                    .header("content-type", Objects.requireNonNull(filePart.headers().getContentType()).toString());

                                            var uploadClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl +
                                                    "/upload/image/?employeeId=" + employeeId + "&providerId=" + providerId);

                                            return uploadClient
                                                    .post()
                                                    .contentType(MediaType.MULTIPART_FORM_DATA)
                                                    .body(BodyInserters.fromMultipartData(builder.build()))
                                                    .retrieve()
                                                    .bodyToMono(String.class)
                                                    .flatMap(link -> {
                                                        if (link.contains("http")) {
                                                            //delete existing avatar from AWS
                                                            if (employee.getAvatar() != null) {
                                                                return this.deleteAvatar(employeeId, false)
                                                                        .flatMap(responseEntity -> {
                                                                            employee.setAvatar(link);
                                                                            return this.employeeService.saveEmployee(employee)
                                                                                    .then(Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(link, null))));
                                                                        });

                                                            } else {
                                                                employee.setAvatar(link);
                                                                return this.employeeService.saveEmployee(employee)
                                                                        .then(Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(link, null))));
                                                            }



                                                        } else {
                                                            return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, link)));
                                                        }
                                                    });

                                        });


                            }else {
                                return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(null, "invalidEmployee")));
                            }

                        }))
                .cast(ResponseEntity.class);

    }





    private Mono<String> deleteEmployeeDetails(Employee employee) {
        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/delete/" + employee.getEmployeeId());
        return client.get()
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    if (response.equals("ok")) {
                        return this.employeeService.deleteEmployee(employee)
                                .then(Mono.just("ok"));
                    } else {
                        return Mono.error(new RuntimeException("Error deleting employee details from auth service"));
                    }
                });
    }

    private Mono<? extends ResponseEntity> persistSubdivision(Forms.NewEmployeeForm form, Long providerId, Employee employee, boolean edit) {
        if (form.subdivisionId != null){
            return this.checkSubdivision(providerId, form.subdivisionId)
                    .flatMap(bool -> {
                        if (bool){
                            employee.setSubdivisionId(form.subdivisionId);
                        }
                        if (edit) {
                            return this.setSubdivision(form.subdivisionId, employee);
                        } else {
                            return this.persistEmployee(employee, false);
                        }
                    });
        } else if (form.division!=null && form.subdivision !=null && form.division.length()>1 && form.subdivision.length()>1) {
            return this.findSubdivision(providerId, form.division, form.subdivision)
                    .flatMap(subdivisionId -> {
                        employee.setSubdivisionId(subdivisionId);
                        if (edit) {
                            return this.setSubdivision(subdivisionId, employee);
                        } else {
                            return this.persistEmployee(employee, false);
                        }
                    });
        } else {
            return this.persistEmployee(employee, edit);
        }
    }

    private Mono<ResponseEntity> setSubdivision(long subdivisionId, Employee employee) {
        return this.employeeService.getSubdivision(subdivisionId)
                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                        .flatMap(division -> {
                            subdivision.setDivision(division);
                            employee.setSubdivision(subdivision);

                            return this.persistEmployee(employee, true);
                        }));
    }

    private Mono<TimeOffBalance> saveTimeOffBalance(long employeeId, EmployeeEntity.TimeOffEntity timeOffBalance) {
        TimeOffBalance balance = new TimeOffBalance(employeeId, 0,0,0,0,0,0);
        Mono<TimeOffBalance> newBalance = this.employeeService.saveTimeOffBalance(balance);

        return this.employeeService.getTimeOffBalance(employeeId)
                .switchIfEmpty(Mono.defer(() -> newBalance))
                .flatMap(timeOff -> {
                    if (timeOffBalance != null) {
                        timeOff.setVacationDays(timeOffBalance.getVacationDays());
                        timeOff.setVacationRolloverDays(timeOffBalance.getVacationRolloverDays());
                        timeOff.setComplimentaryBankHolidayDays(timeOffBalance.getComplimentaryBankHolidayDays());
                        timeOff.setComplimentaryBankHolidayRolloverDays(timeOffBalance.getComplimentaryBankHolidayRolloverDays());
                        timeOff.setCompensationDays(timeOffBalance.getCompensationDays());
                        timeOff.setCompensationRolloverDays(timeOffBalance.getCompensationRolloverDays());
                        return this.employeeService.saveTimeOffBalance(timeOff);
                    } else {
                        return Mono.just(timeOff);
                    }
                });
    }

    private Mono<Boolean> checkSubdivision(long providerId, long subdivisionId) {
        return this.employeeService.getSubdivision(subdivisionId)
                .switchIfEmpty(Mono.defer(() -> Mono.just(new Subdivision())))
                .flatMap(subdivision -> {
                    if (subdivision.getSubdivisionId() > 0) {
                        return this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> Mono.just(division.getProviderId() == providerId));
                    } else {
                        return Mono.just(false);
                    }
                });
    }


    private Mono<Long> findSubdivision(long providerId, String divisionName, String subdivisionName) {
        return this.employeeService.getDivisionByName(providerId, divisionName)
                .switchIfEmpty(Mono.defer(() -> this.saveDivision(providerId, divisionName)))
                .flatMap(division -> this.employeeService.getSubdivisionByName(division.getDivisionId(), subdivisionName)
                        .switchIfEmpty(Mono.defer(() -> this.saveSubdivision(division.getDivisionId(), subdivisionName)))
                        .flatMap(subdivision -> Mono.just(subdivision.getSubdivisionId())));
    }

    private Mono<Division> saveDivision(long providerId, String name) {
        var division = new Division(providerId, name);
        return this.employeeService.saveDivision(division)
                .flatMap(savedDivision -> this.employeeService.getDivisionByName(providerId, name));
    }

    private Mono<Subdivision> saveSubdivision(long divisionId, String name) {
        var subdivision = new Subdivision(divisionId, name);
        return this.employeeService.saveSubdivision(subdivision)
                .flatMap(savedDivision -> this.employeeService.getSubdivisionByName(divisionId, name));
    }


    private Mono<String> deleteEmployeeDetails(String username, long providerId, long employeeId) {
        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/delete/" + employeeId);
        return client.get()
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> this.employeeService.getEmployeeByUsername(providerId, username)
                        .switchIfEmpty(Mono.just(new Employee()))
                        .flatMap(employee -> {
                            if (employee.getEmployeeId() != null) {
                                return this.employeeService.deleteEmployee(employee)
                                        .then(Mono.just("ok"));
                            } else {
                                return Mono.just("ok");
                            }
                        })

                );

    }

    private Mono<ResponseEntity> persistEmployee(Employee emp, boolean edit) {

        if (edit) {
            return this.employeeService.saveEmployee(emp)
                    .then(this.buildEmployeeEntity(emp)
                            .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity))));
        } else {
            return this.employeeService.saveEmployee(emp)
                    .flatMap(e -> this.employeeService.getEmployeeByUsername(e.getProviderId(), e.getUsername()))
                    .flatMap(employee -> this.employeeService.saveTimeOffBalance(emp.getTimeOffBalance()))
                    .flatMap(timeOff -> {
                        List<AuthorizedSchedule> authorizedSchedules = new ArrayList<>();
                        emp.getAuthorizedSchedules().forEach(authorizedSchedule -> authorizedSchedules.add(new AuthorizedSchedule(emp.getEmployeeId(), authorizedSchedule)));
                        return this.employeeService.saveAuthorizedSchedules(authorizedSchedules)
                                .flatMap(authorizedSchedList -> {
                                    List<AuthorizedRoster> authorizedRosters = new ArrayList<>();
                                    emp.getAuthorizedRosters().forEach(authorizedRoster -> authorizedRosters.add(new AuthorizedRoster(emp.getEmployeeId(), authorizedRoster)));
                                    return this.employeeService.saveAuthorizedRosters(authorizedRosters)
                                            .then(this.buildShortEmployeeEntity(emp)
                                                    .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity))));
                                });
                    });
        }
    }

    private Mono<List<Long>> setPersonalAuthorizedRosters (Long subDivisionId, long providerId) {
        if (subDivisionId != null) {

            return this.checkSubdivision(providerId, subDivisionId)
                    .flatMap(bool -> {
                        if (bool) {
                            return this.employeeService.getSubdivision(subDivisionId)
                                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                            .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                                                    .flatMap(subdivisions -> {
                                                        List<Long> authorizedRosters = new ArrayList<>();
                                                        subdivisions.forEach(sub ->  authorizedRosters.add(sub.getSubdivisionId()));
                                                        return Mono.just(authorizedRosters);
                                                    })
                                            )
                                    );

                        } else {
                            return Mono.just(new ArrayList<>());
                        }
                    });

        } else {
            return Mono.just(new ArrayList<>());
        }
    }

//    private Mono<Subdivision> loadSubdivision(Long subDivisionId, long providerId) {
//        return this.employeeService.getSubdivision(subDivisionId)
//                .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
//                        .flatMap(division -> {
//                                    if (division.getProviderId() == providerId) {
//                                        return Mono.just(subdivision);
//                                    } else {
//                                        return Mono.empty();
//                                    }
//                                }
//                        )
//                );
//    }

    private Mono<List<Long>> setAuthorizedRosters (Forms.NewEmployeeForm employeeForm, long providerId) {
        if (employeeForm.subdivisionIds!=null && employeeForm.subdivisionIds.size()>0){
            return Flux.fromIterable(employeeForm.subdivisionIds)
                    .flatMap(subDivisionId -> this.employeeService.getSubdivision(Long.parseLong(subDivisionId))
                            .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                    .flatMap(division -> {
                                        if (division.getProviderId() == providerId) {
                                            return Mono.just(subdivision.getSubdivisionId());
                                        } else {
                                            return Mono.empty();
                                        }
                                    }))
                    )
                    .collectList()
                    .flatMap(Mono::just);
        } else {
            return Mono.just(new ArrayList<>());
        }

    }

    private Mono<List<Long>> setAuthorizedSchedules (Forms.NewEmployeeForm employeeForm, long providerId, Long employeeId, boolean deleteAll) {
        if (deleteAll) {
            return this.employeeService.getAuthorizedSchedules(employeeId)
                    .flatMap(this.employeeService::deleteAuthorizedSchedules)
                    .then(Mono.just(new ArrayList<>()));
        } else {
            if (employeeForm.availabilityIds != null && employeeForm.availabilityIds.size() > 0) {

                return Flux.fromIterable(employeeForm.availabilityIds)
                        .flatMap(id -> {
                            var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl +
                                    "/schedule/get/" + id + "?providerId=" + providerId);
                            return client.get()
                                    .retrieve()
                                    .bodyToMono(Long.class);

                        })
                        .collectList()
                        .flatMap(list -> {
                            var authorizedSchedules = list.stream()
                                    .filter(id -> id > 0)
                                    .collect(Collectors.toList());

                            if (employeeId != null) {
                                List<AuthorizedSchedule> authorizedSchedulesToSave = new ArrayList<>();
                                authorizedSchedules.forEach(authorizedSchedule -> authorizedSchedulesToSave.add(new AuthorizedSchedule(employeeId, authorizedSchedule)));
                                return this.employeeService.getAuthorizedSchedules(employeeId)
                                        .flatMap(this.employeeService::deleteAuthorizedSchedules)
                                        .then(this.employeeService.saveAuthorizedSchedules(authorizedSchedulesToSave))
                                        .then(Mono.just(authorizedSchedules));
                            } else {
                                return Mono.just(authorizedSchedules);
                            }

                        });
            } else {
                return Mono.just(new ArrayList<>());
            }
        }

    }

    private Mono<ResponseEntity> getTimeOff(long id) {

        return this.employeeService.getTimeOffRequest(id)
                .flatMap(timeOffRequests ->
                        getTimeOffRequestEntities(timeOffRequests)
                );
    }

    public Mono<ResponseEntity<List<TimeRequestEntity>>> getTimeOffRequestEntities(List<TimeRequest> timeOffRequests) {
        return Flux.fromIterable(timeOffRequests)
                .flatMap(timeOffRequest -> {
                    var entity = new TimeRequestEntity(timeOffRequest);
                    var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl + "/upload/timeoff/attachments/" + timeOffRequest.getId());

                    return client.get()
                            .retrieve()
                            .bodyToMono(String[].class)
                            .flatMap(response -> {
                                entity.setAttachments(Arrays.asList(response));
                                return Mono.just(entity);

                            });

                })
                .collectList()
                .flatMap(entities -> {
                    var sortedEntities = entities.stream()
                            .sorted(Comparator.comparing(TimeRequestEntity::getStart))
                            .collect(Collectors.toList());

                    Collections.reverse(sortedEntities);
                    return Mono.just(ResponseEntity.ok(sortedEntities));
                });
    }

    private Mono<ResponseEntity> filterEmployeeByName(List<Employee> employees) {
        return Flux.fromIterable(employees)
                .flatMap(this::buildShortEmployeeEntity)
                .collectList()
                .flatMap(employeeEntities -> Mono.just(ResponseEntity.ok(employeeEntities)));
    }

    private Mono<ResponseEntity> filteredEmployees(List<Employee> employees, Integer page, Integer employeesPerPage) {


        var map = new Forms.EmployeeMap();
        map.setTotal(employees.size());

        PagedListHolder<Employee> employeePagedListHolder=new PagedListHolder<>(employees);

        if (page==null){
            page=1;
        }

        int goToPage=page-1;

        if (goToPage<=employeePagedListHolder.getPageCount()&&goToPage>=0){
            employeePagedListHolder.setPage(goToPage);
        }

        employeePagedListHolder.setPageSize(employeesPerPage);

        return Flux.fromIterable(employeePagedListHolder.getPageList())
                .flatMap(this::buildShortEmployeeEntity)
                .collectList()
                .flatMap(entities -> {
                    map.setEntities(entities);
                    return Mono.just(ResponseEntity.ok(map));
                });


    }

    private Mono<List<Employee>> getEmployees(String employeeId, String subdivisionId, String divisionId, long providerId, List<Employee> employees) {
        if (employeeId != null) {
            employees = employees.stream()
                    .filter(employee ->  employee.getEmployeeId() == Long.parseLong(employeeId))
                    .sorted(Comparator.comparing(Employee::getName,
                            Comparator.comparing(String::toLowerCase)))
                    .collect(Collectors.toList());
        }

        if (subdivisionId != null) {
            employees = employees.stream()
                    .filter(employee ->  employee.getSubdivisionId() != null && employee.getSubdivisionId() == Long.parseLong(subdivisionId))
                    .sorted(Comparator.comparing(Employee::getName,
                            Comparator.comparing(String::toLowerCase)))
                    .collect(Collectors.toList());
        }

        if (divisionId != null) {
            List<Employee> finalEmployees = employees;
            return employeeService.getDivision(Long.parseLong(divisionId))
                    .flatMap(division -> {

                        if (division.getProviderId() == providerId) {
                            return this.employeeService.getSubdivisions(division.getDivisionId())
                                    .flatMap(subdivisions -> Mono.just(subdivisions.stream()
                                            .map(Subdivision::getSubdivisionId)
                                            .collect(Collectors.toList()))
                                    )
                                    .flatMap(subdivisionsIds -> Mono.just(finalEmployees.stream()
                                            .filter(employee ->  employee.getSubdivisionId() !=null && subdivisionsIds.contains(employee.getSubdivisionId()))
                                            .sorted(Comparator.comparing(Employee::getName,
                                                    Comparator.comparing(String::toLowerCase)))
                                            .collect(Collectors.toList())));
                        } else {
                            return Mono.just(new ArrayList<>());
                        }


                    });

        } else {
            return Mono.just(employees);
        }

    }

    public Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {

        List<Long> schedules = new ArrayList<>();

        if (employee.getAuthorizedSchedules() != null) {
            schedules.addAll(employee.getAuthorizedSchedules());
        }

        if (employee.getAuthorities().contains("SUBPROVIDER_FULL")) {
            employee.getAuthorities().clear();
            employee.getAuthorities().add("SUBPROVIDER_FULL");
        }

        return  Mono.just(EmployeeEntity.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .authorities(employee.getAuthorities())
                .authorizedSchedules(schedules)
                .authorizedScheduleNames(employee.getAuthorizedScheduleNames())
                .registerDate(employee.getRegisterDate().toString())
                .username(employee.getUsername())
                .avatar(employee.getAvatar())
                .subdivision(employee.getSubdivision() != null ? employee.getSubdivision().getName() : null)
                .division(employee.getSubdivision() != null ? employee.getSubdivision().getDivision().getName() : null)
                .subdivisionId(employee.getSubdivisionId())
                .divisionId(employee.getSubdivision() != null && employee.getSubdivision().getDivision() != null ? employee.getSubdivision().getDivision().getDivisionId() : null)
                .jobTitle(employee.getJobTitle())
                .authorizedRosters(employee.getAuthorizedRosters())
                .timeOffBalance(employee.getTimeOffBalance() != null ? new EmployeeEntity.TimeOffEntity(employee.getTimeOffBalance()) : null)
                .homeAddress(employee.getAddress())
                .phones(employee.getPhones())
                .family(employee.getFamily())
                .bankAccount(employee.getBankAccount())
                .taxPayerId(employee.getTaxPayerId())
                .personalEmail(employee.getPersonalEmail())
                .build());


    }


    private Mono<EmployeeEntity> buildShortEmployeeEntity(Employee employee) {
        if (employee.getSubdivisionId() != null) {
            return this.employeeService.getSubdivision(employee.getSubdivisionId())
                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                            .flatMap(division -> Mono.just(EmployeeEntity.builder()
                                    .id(employee.getEmployeeId())
                                    .name(employee.getName())
                                    .avatar(employee.getAvatar())
                                    .subdivision(subdivision.getName())
                                    .division(division.getName())
                                    .subdivisionId(subdivision.getSubdivisionId())
                                    .divisionId(subdivision.getDivisionId())
                                    .jobTitle(employee.getJobTitle())
                                    .build()))
                    );
        } else {
            return Mono.just(EmployeeEntity.builder()
                    .id(employee.getEmployeeId())
                    .name(employee.getName())
                    .avatar(employee.getAvatar())
                    .jobTitle(employee.getJobTitle())
                    .build());
        }


    }

    public Mono<Employee> loadEmployee(long employeeId) {

        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/authorities/" + employeeId);

        return client.get()
                .retrieve()
                .bodyToMono(String[].class)
                .flatMap(array -> {
                    var authorities = Arrays.asList(array);
                    return this.employeeService.getEmployee(employeeId)
                            .flatMap(employee -> {
                                employee.getAuthorities().addAll(Arrays.asList(array));
                                return Mono.just(employee);
                            })
                            .flatMap(employee -> this.employeeService.getFamily(employeeId)
                                    .flatMap(familyMembers -> {
                                        employee.setFamily(familyMembers);
                                        return Mono.just(employee);
                                    })
                            )
                            .flatMap(employee -> this.employeeService.getPhones(employeeId)
                                    .flatMap(phones -> {
                                        employee.setPhones(phones);
                                        return Mono.just(employee);
                                    })
                            )
                            .flatMap(employee -> this.employeeService.getAddress(employeeId)
                                    .flatMap(address -> {
                                        employee.setAddress(address);
                                        return Mono.just(employee);
                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> this.employeeService.getTimeOffBalance(employeeId)
                                    .flatMap(timeOffBalance -> {
                                        employee.setTimeOffBalance(timeOffBalance);
                                        return Mono.just(employee);
                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> {
                                        if (employee.getSubdivisionId()  != null) {
                                            return this.employeeService.getSubdivision(employee.getSubdivisionId())
                                                    .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                                            .flatMap(division -> {
                                                                subdivision.setDivision(division);
                                                                employee.setSubdivision(subdivision);
                                                                return Mono.just(employee);
                                                            }))
                                                    .switchIfEmpty(Mono.defer(() -> Mono.just(employee)));
                                        } else {
                                            return Mono.just(employee);
                                        }
                                    }
                            )
                            .flatMap(employee -> this.employeeService.getAuthorizedSchedules(employeeId)
                                    .flatMap(schedules -> {
                                        employee.setAuthorizedSchedules(schedules.stream().map(AuthorizedSchedule::getScheduleId).collect(Collectors.toList()));
                                        if (schedules.size()>0) {
                                            return this.setAuthorizedScheduleNames(employee, schedules.stream().map(AuthorizedSchedule::getScheduleId).collect(Collectors.toList()));
                                        } else {
                                            return Mono.just(employee);
                                        }


                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            )
                            .flatMap(employee -> this.employeeService.getAuthorizedRosters(employeeId)
                                    .flatMap(rosters ->  {
                                        List<Long> authorizedRosters = new ArrayList<>();
                                        rosters.forEach(roster -> authorizedRosters.add(roster.getRosterId()));
                                        employee.setAuthorizedRosters(authorizedRosters);
                                        return Mono.just(employee);
                                    })
                                    .switchIfEmpty(Mono.just(employee))
                            );

                });



    }

    private Mono<Employee> setAuthorizedScheduleNames(Employee employee, List<Long> schedules) {
        //TODO connect to appointment service to retrieve authorised schedules

        return Mono.just(employee);
//        Set<String> ids = new HashSet<>();
//        schedules.forEach(schedule -> ids.add(Long.toString(schedule)));
//
//        var webclient = this.commonHelper.buildAPIAccessWebClient(commonHelper.appointmentServiceUrl + "/employee/schedules");
//        return webclient.post()
//                .body(Mono.just(new Forms.SetOfStringsForm(ids)), Forms.SetOfStringsForm.class)
//                .retrieve()
//                .bodyToMono(String[].class)
//                .flatMap(scheduleList -> {
//                    employee.setAuthorizedScheduleNames(Arrays.asList(scheduleList));
//                    return Mono.just(employee);
//                });
    }

    private Mono<Boolean> isRegistered(String username, long id) {

        var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/registered/" + id +"?username=" + username);

        return client.get()
                .retrieve()
                .bodyToMono(Boolean.class);


    }



}
