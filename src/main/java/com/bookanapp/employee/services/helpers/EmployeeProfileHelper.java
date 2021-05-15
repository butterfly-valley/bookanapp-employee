package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.AuthorizedSchedule;
import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.EmployeeRosterSlot;
import com.bookanapp.employee.entities.rest.EmployeeAuthority;
import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeProfileHelper {

    public EmployeeHelper employeeHelper;
    public CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final RosterService rosterService;

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
                .flatMap(employee -> this.employeeService.getTimeOff(employeeId)
                        .flatMap(timeOffBalance -> {
                            employee.setTimeOffBalance(timeOffBalance);
                            return Mono.just(employee);
                        })
                        .switchIfEmpty(Mono.just(employee))
                )
                .flatMap(employee -> {
                            if (employee.getSubdivision() != null) {
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
                );


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

    public Mono<ResponseEntity> submitTimeOff(Forms.TimeOffRequestForm timeOffRequestForm) {
        return this.commonHelper.getCurrentEmployee()
                .flatMap(employee -> {
                    float daysRequested = timeOffRequestForm.numberOfDays;

                    // check if only one half day is requested
                    if (daysRequested > 1 && this.hasDecimal(daysRequested))
                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("noDecimalsAllowed")));

                    return this.employeeService.getTimeOff(employee.getEmployeeId())
                            .flatMap(balance -> {
                                // check if there is enough day balance and deduct
                                for (EmployeeRosterSlot.TimeOffBalanceType timeOffBalanceType : EmployeeRosterSlot.TimeOffBalanceType.values()) {
                                    if (timeOffBalanceType.toString().equals(timeOffRequestForm.balanceType)) {
                                        switch (timeOffRequestForm.balanceType) {
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
                                        .then(sendEmailToSupervisor(employee));

                            });


                });
    }

    private Mono<ResponseEntity> sendEmailToSupervisor(Employee emp) {
        return this.employeeService.getAllEmployees(emp.getProviderId())
                .flatMap(employees -> {
                    var client = this.commonHelper.buildAPIAccessWebClient(commonHelper.providerServiceUrl + "/provider/get/" + emp.getProviderId());

                    return client.get()
                            .retrieve()
                            .bodyToMono(Provider.class)
                            .flatMap(provider -> Flux.fromIterable(employees)
                                    .filter(employee -> !employee.getEmployeeId().equals(emp.getEmployeeId()))
                                    .flatMap(employee -> {
                                        var authClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.authServiceUrl + "employee/get/authorities/" +employee.getEmployeeId());
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
                                        return Flux.fromIterable(listOfAddresses)
                                                .flatMap(emailAddress -> {
                                                    var emailClient = this.commonHelper.buildAPIAccessWebClient(commonHelper.notificationServiceUrl + "/employee/timeoff/notify");
                                                    var form = new Forms.TimeOffRequestNotificationForm(provider, emp, emailAddress);
                                                    return emailClient.post()
                                                            .body(Mono.just(form), Forms.TimeOffRequestNotificationForm.class)
                                                            .retrieve()
                                                            .bodyToMono(String.class);
                                                })
                                                .collectList()
                                                .flatMap(list -> Mono.just(ResponseEntity.ok("ok")));
                                    }))
                            .cast(ResponseEntity.class)
                            .onErrorResume(e -> {
                                        log.error("Error while sending time off request email, error: " + e.getMessage());
                                        return Mono.just(ResponseEntity.ok("ok"));
                                    }
                            );
                });

    }

    private boolean hasDecimal(float in) {
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(in));
        return in > (float) bigDecimal.intValue();

    }

}
