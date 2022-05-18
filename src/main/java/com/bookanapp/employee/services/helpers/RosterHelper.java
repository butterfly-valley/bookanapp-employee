package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.RosterEntity;
import com.bookanapp.employee.services.DateRangeService;
import com.bookanapp.employee.services.EmployeeService;
import com.bookanapp.employee.services.RosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RosterHelper {
    private final RosterService rosterService;
    private final CommonHelper commonHelper;
    private final EmployeeService employeeService;
    private final DateRangeService dateRange;
    private final RosterHelperService rosterHelperService;

    public Mono<ResponseEntity> displaySharedEmployees(Long employeeId, Long subdivisionId, Long divisionId) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.rosterHelperService.getEmployees(employeeId, subdivisionId, divisionId, providerId, false)
                        .flatMap(employees -> this.rosterHelperService.getEmployeeEntities(employees, false)));
    }

    public Mono<ResponseEntity> getAllEmployees() {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(this.employeeService::getAllEmployees)
                .flatMap(employees -> this.rosterHelperService.getEmployeeEntities(employees, false));

    }

    public Mono<ResponseEntity> displaySharedEmployeesAnonymously(Long employeeId, Long subdivisionId, Long divisionId) {
        return this.rosterHelperService.getEmployees(employeeId, subdivisionId, divisionId, 0, true)
                .flatMap(employees -> this.rosterHelperService.getEmployeeEntities(employees, true));
    }

    public Mono<ResponseEntity> displayRoster(String start, String end, String offset, Long employeeId, String subdivisionId, String divisionId, String all, String showTimeOff) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId ->  {
                    List<LocalDate> dateRange = this.dateRange.dateRangeUTC(start, end, offset);
                    boolean timeOff = showTimeOff != null;

                    Mono<List<RosterEntity>> slotEntities = null;

                    if (employeeId != null)
                        slotEntities = this.rosterHelperService.getEmployeeRoster(employeeId, dateRange, timeOff);

                    if (subdivisionId != null && !subdivisionId.equals("null")) {
                        if (!timeOff) {
                            slotEntities = this.rosterHelperService.getSubdivisionRoster(Long.parseLong(subdivisionId), dateRange, providerId);
                        } else {
                            slotEntities = this.rosterHelperService.getSubDivisionEmployees(Long.parseLong(subdivisionId), providerId, false)
                                    .flatMap(employees -> rosterHelperService.getRosterSlotEntities(dateRange, employees));

                        }
                    }
                    if (divisionId != null && !divisionId.equals("null")) {
                        slotEntities = this.employeeService.getDivision(Long.parseLong(divisionId))
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
                                        return this.employeeService.getSubdivisions(division.getDivisionId())
                                                .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                                        .flatMap(subdivision -> this.rosterHelperService.getSubDivisionEmployees(subdivision.getSubdivisionId(), providerId, false)
                                                                .flatMap(employees -> Flux.fromIterable(employees)
                                                                        .flatMap(employee -> this.rosterHelperService.getEmployeeRoster(employee.getEmployeeId(), dateRange, timeOff))
                                                                        .collectList()
                                                                )
                                                        )
                                                        .collectList()
                                                        .flatMap(lists -> {
                                                            List<RosterEntity> entities = new ArrayList<>();
                                                            lists.forEach(subLists -> subLists.forEach(entities::addAll));
                                                            return Mono.just(entities);
                                                        }));

                                    } else {
                                        return Mono.just(new ArrayList<>());
                                    }
                                });
                    }

                    if (all != null){
                        slotEntities = this.employeeService.getAllEmployees(providerId)
                                .flatMap(employees -> rosterHelperService.getRosterSlotEntities(dateRange, employees));
                    }

                    if (slotEntities != null) {
                        return Mono.just(ResponseEntity.ok(slotEntities));
                    } else {
                        return Mono.just(ResponseEntity.ok(new ArrayList<>()));
                    }
                });
    }



    public Mono<ResponseEntity> displayAnonymousRoster(String start, String end, String offset, Long employeeId, String subdivisionId, String divisionId) {
        List<LocalDate> dateRange = this.dateRange.dateRangeUTC(start, end, offset);

        Mono<List<RosterEntity>> slotEntities = null;

        if (employeeId != null)
            slotEntities = this.rosterHelperService.getAnonymousEmployeeRoster(employeeId, dateRange);

        if (subdivisionId != null && !subdivisionId.equals("null"))
            slotEntities = this.rosterHelperService.getAnonymousSubdivisionRoster(Long.parseLong(subdivisionId), dateRange);

        if (divisionId != null && !divisionId.equals("null")) {
            slotEntities = this.employeeService.getDivision(Long.parseLong(divisionId))
                    .flatMap(division -> this.employeeService.getSubdivisions(division.getDivisionId())
                            .flatMap(subdivisions -> Flux.fromIterable(subdivisions)
                                    .flatMap(subdivision -> this.rosterHelperService.getSubDivisionEmployees(subdivision.getSubdivisionId(), 0, true)
                                            .flatMap(employees -> Flux.fromIterable(employees)
                                                    .flatMap(employee -> this.rosterHelperService.getAnonymousEmployeeRoster(employee.getEmployeeId(), dateRange))
                                                    .collectList()
                                            )
                                    )
                                    .collectList()
                                    .flatMap(lists -> {
                                        List<RosterEntity> entities = new ArrayList<>();
                                        lists.forEach(subLists -> subLists.forEach(entities::addAll));
                                        return Mono.just(entities);
                                    })));
        }

        return Mono.just(ResponseEntity.ok(slotEntities));

    }


    public Mono<ResponseEntity> findEmployeeByName(String term) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.commonHelper.getCurrentEmployee()
                        .flatMap(employee -> this.rosterHelperService.returnSearchedEmployees(providerId, term, employee.getEmployeeId()))
                        .switchIfEmpty(this.rosterHelperService.returnSearchedEmployees(providerId, term, null)));
    }


    public Mono<ResponseEntity> uploadRoster(Forms.RosterForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(Long.parseLong(rosterForm.employeeId))
                        .flatMap(employee -> {
                            if (employee.getSubdivisionId() != null) {
                                return this.employeeService.getSubdivision(employee.getSubdivisionId())
                                        .flatMap(this.rosterHelperService::checkRosterAuthority)
                                        .flatMap(authorised -> {
                                            if (authorised) {
                                                return this.rosterHelperService.uploadRoster(rosterForm);
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        });
                            } else {
                                return this.rosterHelperService.uploadRoster(rosterForm);
                            }
                        }));

    }


    public Mono<ResponseEntity> uploadRangeRoster(Forms.RosterRangeForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getEmployee(Long.parseLong(rosterForm.employeeId))
                        .flatMap(employee -> {
                            if (employee.getSubdivisionId() != null) {
                                return this.employeeService.getSubdivision(employee.getSubdivisionId())
                                        .flatMap(this.rosterHelperService::checkRosterAuthority)
                                        .flatMap(authorised -> {
                                            if (authorised) {
                                                return this.rosterHelperService.uploadRange(employee, providerId, rosterForm);
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));
                                            }
                                        });
                            } else {
                                return this.rosterHelperService.uploadRange(employee, providerId, rosterForm);
                            }
                        })

                );

    }

    public Mono<ResponseEntity> uploadRangeRosterDivision(Forms.SubdivisionRosterRangeForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getSubdivision(Long.parseLong(rosterForm.subdivisionId))
                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
                                        return  this.rosterHelperService.checkRosterAuthority(subdivision)
                                                .flatMap(authorised -> {
                                                    if (authorised) {
                                                        return this.rosterHelperService.uploadRangeDivision(subdivision, rosterForm, providerId);
                                                    } else {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSubdivision")));
                                                    }
                                                });
                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSubdivision")));

                                    }
                                })

                        ));

    }


    public Mono<ResponseEntity> uploadSubdivisionRoster(Forms.SubdivisionRosterForm rosterForm) {
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.employeeService.getSubdivision(rosterForm.subdivisionId)
                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                .flatMap(division -> {
                                    if (division.getProviderId() == providerId) {
                                        return  this.rosterHelperService.checkRosterAuthority(subdivision)
                                                .flatMap(authorised -> {
                                                    if (authorised) {
                                                        return this.rosterHelperService.uploadSubdivisionRoster(subdivision, rosterForm, providerId);
                                                    } else {
                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSubdivision")));
                                                    }
                                                });

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidEmployee")));

                                    }
                                })));

    }



    public Mono<ResponseEntity> getRosterPatternsAndColors(){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.rosterService.getColors(providerId)
                        .flatMap(colors -> this.rosterService.getPatterns(providerId)
                                .flatMap(rosterPatterns -> {
                                    List<Object> patternsAndColors = new ArrayList<>();
                                    patternsAndColors.add(rosterPatterns);
                                    patternsAndColors.add(colors);
                                    return Mono.just(ResponseEntity.ok(patternsAndColors));
                                })
                        )
                );
    }

    public Mono<ResponseEntity> updateRosterSlot(Forms.RosterSlotForm slotForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(slotForm.slotDetails)
                        .flatMap(rosterSlotDetailsForm -> {
                            Mono<? extends RosterSlot> slotMono = null;
                            if (rosterSlotDetailsForm.employeeId != null && !rosterSlotDetailsForm.employeeId.equals("0"))
                                slotMono = this.rosterService.findSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (rosterSlotDetailsForm.subdivisionId != null && !rosterSlotDetailsForm.subdivisionId.equals("0"))
                                slotMono = this.rosterService.findSubdivisionRosterSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (slotForm == null)
                                return Mono.just("invalidSlot");

                            assert slotMono != null;
                            return slotMono
                                    .flatMap(slot -> this.rosterHelperService.validateSlot(providerId, slot)
                                            .flatMap(valid -> {
                                                if (!valid) {
                                                    return Mono.just("invalidSlot");
                                                } else {
                                                    if (slotForm.start != null) {
                                                        LocalTime startTime = LocalTime.of(slotForm.start.hour, slotForm.start.minute);
                                                        slot.setStart(startTime);
                                                    }

                                                    if (slotForm.end != null) {
                                                        LocalTime endTime = LocalTime.of(slotForm.end.hour, slotForm.end.minute);
                                                        slot.setEnd(endTime);

                                                    }

                                                    if (slotForm.note != null)
                                                        slot.setNote(slotForm.note);

                                                    if (slotForm.color != null)
                                                        slot.setColor(slotForm.color);

                                                    slot.setPublished(slotForm.publish);

                                                    if (slot instanceof EmployeeRosterSlot) {
                                                        ((EmployeeRosterSlot) slot).setMaternityLeave(slotForm.maternityLeave);
                                                        ((EmployeeRosterSlot) slot).setSickLeave(slotForm.sickLeave);
                                                        return this.rosterService.saveRosterSlot((EmployeeRosterSlot) slot)
                                                                .then(Mono.just("success"));

                                                    } else if  (slot instanceof SubdivisionRosterSlot) {
                                                        return this.rosterService.saveSubdivisionRosterSlot((SubdivisionRosterSlot) slot)
                                                                .then(Mono.just("success"));
                                                    } else {
                                                        return Mono.just("error");
                                                    }


                                                }
                                            })

                                    );

                        })
                        .collectList()
                        .flatMap(this.rosterHelperService::processMessages)

                );
    }

    public Mono<ResponseEntity> pasteRosterSlot(Forms.PasteRosterSlotForm slotForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                    Mono<? extends RosterSlot> slotMono = this.rosterService.findSlot(slotForm.slotId).switchIfEmpty(Mono.just(new EmployeeRosterSlot()))
                            .flatMap(slot -> {
                                if (slot.getSlotId() == null) {
                                    return this.rosterService.findSubdivisionRosterSlot(slotForm.slotId);
                                } else {
                                    return Mono.just(slot);
                                }
                            });
                    return slotMono
                            .flatMap(slot -> this.rosterHelperService.validateSlot(providerId, slot)
                                    .flatMap(valid -> {
                                        if (!valid) {
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                        } else {
                                            if (slotForm.employeeId != 0) {
                                                return this.employeeService.getEmployee(slotForm.employeeId)
                                                        .flatMap(employee -> {
                                                            if (employee.getProviderId() == providerId) {
                                                                List<EmployeeRosterSlot> slotList = new ArrayList<>();
                                                                for (int i = 0; i < slotForm.size; i++) {
                                                                    LocalDate date = slotForm.date.plusDays(i);
                                                                    EmployeeRosterSlot employeeRosterSlot = new EmployeeRosterSlot(date, slot.getStart(), slot.getEnd());
                                                                    employeeRosterSlot.setEmployeeId(employee.getEmployeeId());
                                                                    employeeRosterSlot.setColor(slot.getColor());
                                                                    slotList.add(employeeRosterSlot);
                                                                }
                                                                return this.rosterService.saveRosterSlots(slotList)
                                                                        .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                                                            } else {
                                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                                            }
                                                        });


                                            } else if (slotForm.subdivisionId != 0) {
                                                return this.employeeService.getSubdivision(slotForm.subdivisionId)
                                                        .flatMap(subdivision -> this.employeeService.getDivision(subdivision.getDivisionId())
                                                                .flatMap(division -> {
                                                                    if (division.getProviderId() == providerId) {
                                                                        List<SubdivisionRosterSlot> slotList = new ArrayList<>();
                                                                        for (int i = 0; i < slotForm.size; i++) {
                                                                            LocalDate date = slotForm.date.plusDays(i);
                                                                            var newSlot = new SubdivisionRosterSlot(date, slot.getStart(), slot.getEnd());
                                                                            newSlot.setSubdivisionId(subdivision.getSubdivisionId());
                                                                            newSlot.setColor(slot.getColor());
                                                                            slotList.add(newSlot);
                                                                        }
                                                                        return this.rosterService.saveSubdivisionRosterSlots(slotList)
                                                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));
                                                                    } else {
                                                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                                                    }
                                                                }));
                                            } else {
                                                return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidSlot")));
                                            }

                                        }
                                    })
                            );

                });
    }

    public Mono<ResponseEntity> deleteRosterSlot(Forms.DeleteRosterSlotForm slotForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(slotForm.slotDetails)
                        .flatMap(rosterSlotDetailsForm -> {
                            Mono<? extends RosterSlot> slotMono = null;
                            if (rosterSlotDetailsForm.employeeId != null && !rosterSlotDetailsForm.employeeId.equals("0"))
                                slotMono = this.rosterService.findSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (rosterSlotDetailsForm.subdivisionId != null && !rosterSlotDetailsForm.subdivisionId.equals("0"))
                                slotMono = this.rosterService.findSubdivisionRosterSlot(Long.parseLong(rosterSlotDetailsForm.slotId));

                            if (slotForm == null)
                                return Mono.just("invalidSlot");

                            assert slotMono != null;
                            return slotMono
                                    .flatMap(slot -> this.rosterHelperService.validateSlot(providerId, slot)
                                            .flatMap(valid -> {
                                                if (!valid) {
                                                    return Mono.just("invalidSlot");
                                                } else {
                                                    if (slot instanceof EmployeeRosterSlot) {
                                                        return this.rosterService.deleteSlot((EmployeeRosterSlot) slot)
                                                                .then(Mono.just("success"));

                                                    } else if  (slot instanceof SubdivisionRosterSlot) {
                                                        return this.rosterService.deleteSubdivisionSlot((SubdivisionRosterSlot) slot)
                                                                .then(Mono.just("success"));
                                                    } else {
                                                        return Mono.just("error");
                                                    }
                                                }
                                            })
                                    );

                        })
                        .collectList()
                        .flatMap(this.rosterHelperService::processMessages)

                );
    }

    public Mono<ResponseEntity> publishRoster(Forms.DeleteOrPublishRosterSlotForm deleteForm){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> {
                            List<LocalDate> interval = dateRange.enhancedDateRange(deleteForm.startDate, deleteForm.endDate);

                            return this.rosterHelperService.getEmployees(deleteForm.employeeId, deleteForm.subdivisionId, deleteForm.divisionId, providerId, false)
                                    .flatMap(employees -> {
                                        if (employees.size()>0) {
                                            return this.rosterHelperService.publishOrDeleteEmployeeRosterSlots(employees, interval, deleteForm.delete, deleteForm.unpublish)
                                                    .flatMap(response -> {
                                                        if (Objects.requireNonNull(response.getBody()).message.equals("success")){
                                                            if (deleteForm.subdivisionId != null) {
                                                                return this.rosterHelperService.checkSubdivisionId(deleteForm.subdivisionId, providerId)
                                                                        .flatMap(subdivision -> {
                                                                            if (subdivision.getName() != null) {
                                                                                return this.rosterHelperService.publishOrDeleteSubdivisionRosterSlots(subdivision, interval, deleteForm.delete);
                                                                            } else {
                                                                                return Mono.just(response);
                                                                            }
                                                                        });
                                                            } else {
                                                                return Mono.just(response);
                                                            }
                                                        } else {
                                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("error")));
                                                        }
                                                    });

                                        } else {
                                            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
                                        }
                                    });

                        }

                );
    }

    public Mono<ResponseEntity> approveTimeOff(Forms.DeleteForm deleteForm, boolean deny){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> Flux.fromIterable(deleteForm.idsToDelete)
                        .flatMap(id -> this.rosterService.findSlot(Long.parseLong(id))
                                .flatMap(slot -> this.employeeService.getEmployee(slot.getEmployeeId())
                                        .flatMap(employee -> {
                                            if (employee.getProviderId() == providerId) {
                                                slot.setTimeOffApproved(!deny);
                                                slot.setTimeOffDenied(deny);
                                                return this.rosterService.saveRosterSlot(slot)
                                                        .then(Mono.just(""));
//                                                        .then(this.rosterHelperService.sendTimeOffApprovalResponseEmail(employee, !deny));

                                            } else {
                                                return Mono.just("invalidSlot");
                                            }

                                        })
                                )
                        )
                        .collectList()
                        .flatMap(this.rosterHelperService::processMessages)
                );
    }

    public Mono<ResponseEntity> approveAbsence(String requestId, String deny){
        return this.commonHelper.getCurrentProviderId()
                .flatMap(providerId -> this.rosterService.getAbsenceRequest(requestId)
                        .flatMap(absenceRequest -> this.employeeService.getEmployee(absenceRequest.getEmployeeId())
                                .flatMap(employee -> {
                                    if (employee.getProviderId() == providerId) {
                                        absenceRequest.setToBeApproved(false);
                                        absenceRequest.setApproved(deny == null);
                                        return this.rosterService.saveAbsenceRequest(absenceRequest)
                                                .then(Mono.just(ResponseEntity.ok(new Forms.GenericResponse("success"))));

                                    } else {
                                        return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("invalidRequest")));
                                    }
                                }))
                );
    }



}
