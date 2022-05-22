package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.*;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeProfileHelper {

    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final RosterService rosterService;
    private final EmployeeHelperService helperService;
    private final RosterHelperService rosterHelperService;
    private final KafkaTemplate<String, Forms.TimeOffRequestNotificationForm> notifyVacationApproverTemplate;

    public Mono<Employee> loadEmployee(long employeeId) {
        return this.employeeService.getEmployee(employeeId)
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
                .flatMap(this.rosterHelperService::setEmployeeSubdivision);


    }



    public Mono<EmployeeEntity> buildEmployeeEntity(Employee employee) {

        return  Mono.just(EmployeeEntity.builder()
                .id(employee.getEmployeeId())
                .name(employee.getName())
                .registerDate(employee.getRegisterDate().toString())
                .username(employee.getUsername())
                .avatar(employee.getAvatar())
                .subdivision(employee.getSubdivision() != null ? employee.getSubdivision().getName() : null)
                .division(employee.getSubdivision() != null ? employee.getSubdivision().getDivision().getName() : null)
                .subdivisionId(employee.getSubdivisionId())
                .divisionId(employee.getSubdivision() != null && employee.getSubdivision().getDivision() != null ? employee.getSubdivision().getDivision().getDivisionId() : null)
                .jobTitle(employee.getJobTitle())
                .timeOffBalance(employee.toString() != null ? new EmployeeEntity.TimeOffEntity(employee.getTimeOffBalance()) : null)
                .homeAddress(employee.getAddress())
                .phones(employee.getPhones())
                .family(employee.getFamily())
                .bankAccount(employee.getBankAccount())
                .taxPayerId(employee.getTaxPayerId())
                .personalEmail(employee.getPersonalEmail())
                .build());


    }

    public Mono<ResponseEntity> editProfile(Forms.ProfileEditForm profileEditForm) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(emp -> this.loadEmployee(emp.getEmployeeId()))
                .flatMap(employee -> {
                    long employeeId = employee.getEmployeeId();
                    boolean newFamily = false;
                    boolean newPhoneList = false;
                    if (profileEditForm.family != null && profileEditForm.family.size()>0) {
                        employee.getFamily().clear();
                        employee.getFamily().addAll(profileEditForm.family);
                        employee.getFamily().forEach(familyMember -> familyMember.setEmployeeId(employeeId));
                        newFamily = true;
                    }

                    if (profileEditForm.phones != null && profileEditForm.phones.size()>0) {
                        List<Phone> newPhones = new ArrayList<>();
                        List<Phone> phonesToKeep = new ArrayList<>();
                        for (Forms.Phone phone : profileEditForm.phones) {
                            if (phone.type!=null) {
                                String code = phone.phone.dialCode.split("\\+")[1];
                                String number = phone.phone.nationalNumber.replace(" ", "");
                                Phone newPhone = new Phone(employeeId, phone.type, code, number);
                                if (!employee.getPhones().contains(newPhone)) {
                                    newPhones.add(new Phone(employeeId, phone.type, code, number));
                                } else {
                                    phonesToKeep.add(new Phone(employeeId, phone.type, code, number));
                                }
                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("phoneTypeError")));
                            }
                        }
                        employee.getPhones().clear();
                        employee.getPhones().addAll(phonesToKeep);
                        employee.getPhones().addAll(newPhones);
                        newPhoneList = true;
                    }

                    if (profileEditForm.bankAccount != null && profileEditForm.bankAccount.length() > 2) {
                        employee.setBankAccount(profileEditForm.bankAccount);
                    }

                    if (profileEditForm.address != null) {
                        this.setAddress(employee.getAddress(), profileEditForm, employeeId);
                    }

                    if (profileEditForm.personalEmail != null) {
                        employee.setPersonalEmail(profileEditForm.personalEmail);
                    }


                    boolean finalNewFamily = newFamily;
                    boolean finalNewPhoneList = newPhoneList;
                    return this.employeeService.saveEmployee(employee)
                            .flatMap(savedEmployee -> {
                                if (finalNewFamily) {
                                    return this.employeeService.getFamily(employeeId)
                                            .flatMap(this.employeeService::deleteFamily)
                                            .then(this.employeeService.saveFamily(employee.getFamily()));
                                } else {
                                    return Mono.just(employee.getFamily());
                                }
                            })
                            .flatMap(familyMembers -> {
                                if (finalNewPhoneList) {
                                    return this.employeeService.getPhones(employeeId)
                                            .flatMap(this.employeeService::deletePhones)
                                            .then(this.employeeService.savePhones(employee.getPhones()));
                                } else {
                                    return Mono.just(employee.getPhones());
                                }
                            })
                            .then(this.employeeService.saveFamily(employee.getFamily()))
                            .flatMap(familyMembers -> {
                                if (employee.getAddress().getStreet() != null) {
                                    return this.employeeService.saveAddress(employee.getAddress())
                                            .then(this.buildEmployeeEntity(employee)
                                                    .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity))));
                                } else {
                                    return this.buildEmployeeEntity(employee)
                                            .flatMap(employeeEntity -> Mono.just(ResponseEntity.ok(employeeEntity)));
                                }
                            });

                });

    }


    public Mono<ResponseEntity> submitTimeOff(Forms.TimeOffRequestForm timeOffRequestForm) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> {
                    float daysRequested = timeOffRequestForm.numberOfDays;

                    // check if only one half day is requested
                    if (daysRequested > 1 && this.hasDecimal(daysRequested))
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("noDecimalsAllowed")));

                    return this.employeeService.getTimeOffBalance(employee.getEmployeeId())
                            .flatMap(balance -> {
                                // check if there is enough day balance and deduct
                                Mono<ResponseEntity<Forms.GenericResponse>> insufficientBalance = deductDaysFromBalance(timeOffRequestForm.balanceType, daysRequested, balance);
                                if (insufficientBalance != null) return insufficientBalance;

                                List<EmployeeRosterSlot> slotList = new ArrayList<>();

                                if (daysRequested < 1) {
                                    EmployeeRosterSlot slot = new EmployeeRosterSlot(timeOffRequestForm.initialDate, LocalTime.MIN, LocalTime.MAX);
                                    slot.setEmployeeId(employee.getEmployeeId());
                                    slot.setTimeOff(true);
                                    slot.setBalanceType(EmployeeRosterSlot.TimeOffBalanceType.valueOf(timeOffRequestForm.balanceType));
                                    slot.setColor("gray");
                                    slot.setHalfDayOff(true);
                                    slotList.add(slot);

                                } else {
                                    for (int i = 0; i < daysRequested; i++) {
                                        LocalDate date = timeOffRequestForm.initialDate.plusDays(i);
                                        EmployeeRosterSlot slot = new EmployeeRosterSlot(date, LocalTime.MIN, LocalTime.MAX);
                                        slot.setEmployeeId(employee.getEmployeeId());
                                        slot.setTimeOff(true);
                                        slot.setBalanceType(EmployeeRosterSlot.TimeOffBalanceType.valueOf(timeOffRequestForm.balanceType));
                                        slot.setColor("gray");
                                        slotList.add(slot);
                                    }
                                }

                                return this.rosterService.saveRosterSlots(slotList)
                                        .then(this.employeeService.saveTimeOffBalance(balance))
                                        .then(this.sendEmailToSupervisor(employee));

                            });


                });
    }

    public Mono<ResponseEntity> submitTimeOffList(Forms.TimeOffRequestDateListForm timeOffRequestForm) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> {
                    float daysRequested = timeOffRequestForm.dates.size();

                    // check if only one half day is requested
                    if (daysRequested > 1 && this.hasDecimal(daysRequested))
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("noDecimalsAllowed")));

                    if (timeOffRequestForm.halfDay)
                        daysRequested = daysRequested/2;

                    // check if only one half day is requested
                    if (daysRequested > 1 && this.hasDecimal(daysRequested))
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("noDecimalsAllowed")));

                    float finalDaysRequested = daysRequested;
                    return this.employeeService.getTimeOffBalance(employee.getEmployeeId())
                            .flatMap(balance -> {
                                // check if there is enough day balance and deduct
                                Mono<ResponseEntity<Forms.GenericResponse>> insufficientBalance = deductDaysFromBalance(timeOffRequestForm.balanceType, finalDaysRequested, balance);
                                if (insufficientBalance != null) return insufficientBalance;
                                List<EmployeeRosterSlot> slotList = new ArrayList<>();

                                timeOffRequestForm.dates.forEach(date -> {
                                    EmployeeRosterSlot slot = new EmployeeRosterSlot(date, LocalTime.MIN, LocalTime.MAX);
                                    slot.setEmployeeId(employee.getEmployeeId());
                                    slot.setTimeOff(true);
                                    slot.setHalfDayOff(timeOffRequestForm.halfDay);
                                    slot.setBalanceType(EmployeeRosterSlot.TimeOffBalanceType.valueOf(timeOffRequestForm.balanceType));
                                    slot.setColor("gray");
                                    slotList.add(slot);

                                });


                                return this.rosterService.saveRosterSlots(slotList)
                                        .then(this.employeeService.saveTimeOffBalance(balance))
                                        .then(this.sendEmailToSupervisor(employee));

                            });


                });
    }



    public Mono<ResponseEntity> deleteTimeOff(Forms.DeleteForm deleteForm) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> this.employeeService.getTimeOffBalance(employee.getEmployeeId())
                        .flatMap(balance -> Flux.fromIterable(deleteForm.idsToDelete)
                                .flatMap(id -> this.rosterService.findSlot(Long.parseLong(id))
                                        .flatMap(slot -> {
                                            if (slot.getEmployeeId() == employee.getEmployeeId()) {
                                                switch (slot.getBalanceType()) {
                                                    case VACS:
                                                        balance.setVacationDays(balance.getVacationDays() + (slot.isHalfDayOff() ? 0.5f : 1.0f));
                                                        break;
                                                    case VACSROLLOVER:
                                                        balance.setVacationRolloverDays(balance.getVacationRolloverDays() + (slot.isHalfDayOff() ? 0.5f : 1.0f));
                                                        break;
                                                    case BANK:
                                                        balance.setComplimentaryBankHolidayDays(balance.getComplimentaryBankHolidayDays() + (slot.isHalfDayOff() ? 0.5f : 1.0f));
                                                        break;
                                                    case BANKROLLOVER:
                                                        balance.setComplimentaryBankHolidayRolloverDays(balance.getComplimentaryBankHolidayRolloverDays()  + (slot.isHalfDayOff() ? 0.5f : 1.0f));
                                                        break;
                                                    case COMP:
                                                        balance.setCompensationDays(balance.getCompensationDays()  + (slot.isHalfDayOff() ? 0.5f : 1.0f));
                                                        break;
                                                    default:
                                                        balance.setCompensationRolloverDays(balance.getCompensationRolloverDays()  + (slot.isHalfDayOff() ? 0.5f : 1.0f));

                                                }
                                                return this.rosterService.deleteSlot(slot)
                                                        .then(Mono.just("ok"));
                                            } else {
                                                return Mono.just("invalidSlot");
                                            }

                                        }))
                                .collectList()
                                .flatMap(list -> {
                                    if (list.contains("invalidSlot")) {
                                        return Mono.just(ResponseEntity.ok("invalidSlot"));
                                    } else {
                                        return this.employeeService.saveTimeOffBalance(balance)
                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                                    }
                                })
                        ));
    }


    public Mono<ResponseEntity> submitAbsence(Forms.AbsenceRequestForm timeRequestForm) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> {
                    LocalTime start = LocalTime.of(timeRequestForm.start.hour, timeRequestForm.start.minute);
                    LocalTime end = LocalTime.of(timeRequestForm.end.hour, timeRequestForm.end.minute);

                    TimeRequest timeRequest = TimeRequest.builder()
                            .id(UUID.randomUUID().toString())
                            .toBeApproved(true)
                            .employeeId(employee.getEmployeeId())
//                            .date(timeRequestForm.date)
                            .start(start)
                            .end(end)
                            .overtime(timeRequestForm.overtime)
                            .comments(timeRequestForm.comments)
                            .newRequest(true)
                            .build();

                    return this.rosterService.saveAbsenceRequest(timeRequest)
                            .flatMap(savedRequest -> Mono.just(ResponseEntity.ok(new TimeRequestEntity(savedRequest))));

                });
    }

    public Mono<ResponseEntity> getListOfAbsencesOrOvertime() {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> this.employeeService.getTimeOffRequest(employee.getEmployeeId())
                        .flatMap(this.helperService::getTimeOffRequestEntities));

    }

    public Mono<ResponseEntity> uploadAttachment(String id, Mono<FilePart> file) {
        return this.rosterService.getAbsenceRequest(id)
                .flatMap(absenceRequest -> this.commonHelper.getCurrentEmployee()
                        .flatMap(employee -> {
                            if (absenceRequest.getEmployeeId() == employee.getEmployeeId()) {
                                return file
                                        .flatMap(filePart -> {
                                            MultipartBodyBuilder builder = new MultipartBodyBuilder();
                                            builder.part("file", filePart)
                                                    .header("content-type", Objects.requireNonNull(filePart.headers().getContentType()).toString());

                                            var uploadClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl +
                                                    "/upload/image/?absenceRequestId=" + id);

                                            return uploadClient
                                                    .post()
                                                    .contentType(MediaType.MULTIPART_FORM_DATA)
                                                    .body(BodyInserters.fromMultipartData(builder.build()))
                                                    .retrieve()
                                                    .bodyToMono(String.class)
                                                    .flatMap(link -> {
                                                        if (link.contains("http")) {
                                                            return Mono.just(ResponseEntity.ok(new Forms.FileUploadResponse(link, null)));
                                                        } else {
                                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse(link)));
                                                        }
                                                    });

                                        });
                            } else {
                                return Mono.just(ResponseEntity.ok("error"));
                            }
                        } ));
    }

    public Mono<ResponseEntity> downloadAttachment(String id, String key) {
        return this.rosterService.getAbsenceRequest(id)
                .flatMap(absenceRequest -> this.commonHelper.getCurrentEmployee()
                        .flatMap(employee -> {
                            if (absenceRequest.getEmployeeId() == employee.getEmployeeId()) {
                                var uploadClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl +
                                        "/upload/absence-request/attachment/get/" + id + "?key=" + key);

                                return uploadClient
                                        .get()
                                        .retrieve()
                                        .bodyToMono(byte[].class)
                                        .flatMap(file -> Mono.just(ResponseEntity.ok(file)));

                            } else {
                                return Mono.just(ResponseEntity.ok("error"));
                            }
                        } ));
    }

    public Mono<ResponseEntity> deleteAttachment(String id, String key) {
        return this.rosterService.getAbsenceRequest(id)
                .flatMap(absenceRequest -> this.commonHelper.getCurrentEmployee()
                        .flatMap(employee -> {
                            if (absenceRequest.getEmployeeId() == employee.getEmployeeId()) {
                                var deleteClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl
                                        + "/upload/delete?=bucket=bookanapp-employee-absence-request-attachment&link="+ key);

                                return deleteClient
                                        .get()
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .flatMap(response ->  {
                                            if (response.equals("success")) {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success")));
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                                            }
                                        });

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                            }
                        } ));
    }

    public Mono<ResponseEntity> deletePhone(long id) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> this.employeeService.getPhone(id)
                        .flatMap(phone -> {
                            if (phone.getEmployeeId().equals(employee.getEmployeeId())) {
                                return this.employeeService.deletePhone(phone)
                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidPhone")));
                            }
                        }));
    }

    public Mono<ResponseEntity> deleteFamilyMember(long id) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> this.employeeService.getFamilyMember(id)
                        .flatMap(member -> {
                            if (member.getEmployeeId().equals(employee.getEmployeeId())) {
                                return this.employeeService.deleteFamilyMember(member)
                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                            } else {
                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidMember")));
                            }
                        }));
    }



    private Mono<ResponseEntity> sendEmailToSupervisor(Employee emp) {
        return this.employeeService.getEmployee(emp.getEmployeeId())
                .flatMap(currentEmployee -> this.employeeService.getAllEmployees(emp.getProviderId())
                        .flatMap(employees ->  this.commonHelper.fetchCurrentProvider()
                                .flatMap(provider -> Flux.fromIterable(employees)
                                        .filter(employee -> !employee.getEmployeeId().equals(emp.getEmployeeId()))
                                        .flatMap(employee -> {
                                            var authClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "/employee/authorities/" +employee.getEmployeeId());
                                            return authClient.get()
                                                    .retrieve()
                                                    .bodyToMono(String[].class)
                                                    .flatMap(array -> {
                                                        var authorities = Arrays.asList(array);
                                                        if (authorities.contains("SUBPROVIDER_ROSTER") || authorities.contains("SUBPROVIDER_FULL")) {
                                                            return Mono.just(employee.getUsername());
                                                        } else {
                                                            return Mono.empty();
                                                        }

                                                    });
                                        })
                                        .collectList()
                                        .flatMap(listOfAddresses -> {
                                            listOfAddresses.add(provider.getUsername());
                                            var form = new Forms.TimeOffRequestNotificationForm(provider.getLocale(), emp, listOfAddresses);
                                            this.notifyVacationApproverTemplate.send("notifyVacationApprover", form);
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success")));
                                        }))
                                .cast(ResponseEntity.class)
                                .onErrorResume(e -> {
                                    log.error("Error while sending time off request email, error: " + e.getMessage());
                                    return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success")));
                                })));


    }

    private boolean hasDecimal(float in) {
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(in));
        return in > (float) bigDecimal.intValue();

    }

    private void setAddress(Address address, Forms.ProfileEditForm form, long employeeId) {
        var formAddress = form.address;
        address.setCity(formAddress.getCity());
        address.setCountry(formAddress.getCountry());
        address.setStreet(formAddress.getStreet());
        address.setProvince(formAddress.getProvince());
        address.setPostalCode(formAddress.getPostalCode());
        if (address.getEmployeeId() == null)
            address.setEmployeeId(employeeId);

    }

    private Mono<ResponseEntity<Forms.GenericResponse>> deductDaysFromBalance(String balanceType, float daysRequested, TimeOffBalance balance) {
        for (EmployeeRosterSlot.TimeOffBalanceType timeOffBalanceType : EmployeeRosterSlot.TimeOffBalanceType.values()) {
            if (timeOffBalanceType.toString().equals(balanceType)) {
                switch (balanceType) {
                    case "VACS":
                        if (daysRequested > balance.getVacationDays()) {
                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("insufficientBalance")));
                        } else {
                            balance.setVacationDays(balance.getVacationDays() - daysRequested);
                        }
                        break;
                    case "VACSROLLOVER":
                        if (daysRequested > balance.getVacationRolloverDays()) {
                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("insufficientBalance")));
                        } else {
                            balance.setVacationRolloverDays(balance.getVacationRolloverDays() - daysRequested);
                        }
                        break;
                    case "BANK":
                        if (daysRequested > balance.getComplimentaryBankHolidayDays()) {
                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("insufficientBalance")));
                        } else {
                            balance.setComplimentaryBankHolidayDays(balance.getComplimentaryBankHolidayDays() - daysRequested);
                        }
                        break;
                    case "BANKROLLOVER":
                        if (daysRequested > balance.getComplimentaryBankHolidayRolloverDays()) {
                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("insufficientBalance")));
                        } else {
                            balance.setComplimentaryBankHolidayRolloverDays(balance.getComplimentaryBankHolidayRolloverDays() - daysRequested);
                        }
                        break;
                    case "COMP":
                        if (daysRequested > balance.getVacationDays()) {
                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("insufficientBalance")));
                        } else {
                            balance.setCompensationDays(balance.getCompensationDays() - daysRequested);
                        }
                        break;
                    default:
                        if (daysRequested > balance.getVacationDays()) {
                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("insufficientBalance")));
                        } else {
                            balance.setCompensationRolloverDays(balance.getCompensationRolloverDays() - daysRequested);
                        }

                }
            }
        }
        return null;
    }

    private void senApprovalEmailNotification(long providerId, Employee employee) {


    }

}
