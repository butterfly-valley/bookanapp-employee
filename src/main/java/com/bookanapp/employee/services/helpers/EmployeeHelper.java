package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.support.PagedListHolder;
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
    private final EmployeeHelperService helperService;
    private final RosterHelperService rosterHelperService;

    public Mono<ResponseEntity> currentEmployees(Integer page, Integer employeesPerPage, String employeeId, String subdivisionId, String divisionId){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getAllEmployees(providerId)
                        .flatMap(employees ->  this.helperService.getEmployees(employeeId, subdivisionId, divisionId, providerId, employees)
                                .flatMap(filtered -> this.helperService.filteredEmployees(filtered, page, employeesPerPage))));


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

    public Mono<ResponseEntity> findEmployeeByName(String term) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getAllEmployeesByName(providerId, term))
                .flatMap(this.helperService::filterEmployeeByName);
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
                                                                .flatMap(this.helperService::buildEmployeeEntity)
                                                                .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                                    }
                                                } else {
                                                    return this.loadEmployee(employee.getEmployeeId())
                                                            .flatMap(this.helperService::buildEmployeeEntity)
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

                                                                return this.helperService.setAuthorizedSchedules(form, providerId, null, false)
                                                                        .flatMap(authorizedSchedules -> this.helperService.setAuthorizedRosters(form, providerId)
                                                                                .flatMap(authorizedRosters -> this.helperService.setPersonalAuthorizedRosters(form.subdivisionId, providerId)
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

                                                                                                    return this.helperService.persistSubdivision(form, providerId, employee, false);
                                                                                                }
                                                                                        )
                                                                                )
                                                                        )
                                                                        .cast(ResponseEntity.class)
                                                                        .onErrorResume(e -> {
                                                                            log.error("Error registering new employee, error: " + e.getMessage());
                                                                            return this.helperService.deleteEmployeeDetails(form.email, providerId, employeeId)
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
                                                        return this.helperService.setAuthorizedSchedules(form, providerId, id, form.allSchedules)
                                                                .flatMap(authorizedSchedules -> this.helperService.setAuthorizedRosters(form, providerId)
                                                                        .flatMap(authorizedRosters -> this.helperService.setPersonalAuthorizedRosters(form.subdivisionId, id)
                                                                                .flatMap(personalRosters -> {
                                                                                            authorizedRosters.addAll(personalRosters);
                                                                                            return this.helperService.saveTimeOffBalance(id, form.getTimeOffBalance())
                                                                                                    .flatMap(balance -> {
                                                                                                        employee.setTimeOffBalance(balance);
                                                                                                        employee.setAuthorizedSchedules(authorizedSchedules);

                                                                                                        if (!form.email.equalsIgnoreCase(employee.getUsername())) {
                                                                                                            return this.helperService.isRegistered(form.email, id)
                                                                                                                    .flatMap(registered -> {
                                                                                                                        if (registered) {
                                                                                                                            return this.helperService.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                    .then(this.helperService.setAuthorizedRosters(employee, authorizedRosters))
                                                                                                                                    .flatMap(emp -> this.helperService.persistSubdivision(form, providerId, emp, true))
                                                                                                                                    .flatMap(entity -> Mono.just(ResponseEntity.ok(new Forms.GenericResponse("existingUser"))));
                                                                                                                        } else {
                                                                                                                            employee.setUsername(form.email);
                                                                                                                            return this.helperService.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                                    .then(this.helperService.setAuthorizedRosters(employee, authorizedRosters))
                                                                                                                                    .then(this.helperService.persistSubdivision(form, providerId, employee, true));
                                                                                                                        }
                                                                                                                    });
                                                                                                        } else {
                                                                                                            return this.helperService.setAuthorizedScheduleNames(employee, authorizedSchedules)
                                                                                                                    .then(this.helperService.setAuthorizedRosters(employee, authorizedRosters))
                                                                                                                    .then(this.helperService.persistSubdivision(form, providerId, employee, true));
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
                                                        return this.helperService.getTimeOff(employee.getEmployeeId());
                                                    }
                                                } else {
                                                    return this.helperService.getTimeOff(employee.getEmployeeId());
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
                                            .flatMap(response ->  this.helperService.deleteEmployeeDetails(employee));
                                } else {
                                    return this.helperService.deleteEmployeeDetails(employee);
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
                            .flatMap(this.rosterHelperService::setEmployeeSubdivision)
                            .flatMap(employee -> this.employeeService.getAuthorizedSchedules(employeeId)
                                    .flatMap(schedules -> {
                                        employee.setAuthorizedSchedules(schedules.stream().map(AuthorizedSchedule::getScheduleId).collect(Collectors.toList()));
                                        if (schedules.size()>0) {
                                            return this.helperService.setAuthorizedScheduleNames(employee, schedules.stream().map(AuthorizedSchedule::getScheduleId).collect(Collectors.toList()));
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


}
