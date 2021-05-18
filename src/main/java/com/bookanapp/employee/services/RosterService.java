package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RosterService {

    private final EmployeeRosterSlotRepository rosterSlotRepository;
    private final AbsenceRequestRepository absenceRequestRepository;
    private final SubdivisionRosterSlotRepository subdivisionRosterSlotRepository;
    private final RosterPatternRepository rosterPatternRepository;
    private final RosterSlotColorRepository rosterSlotColorRepository;

    public Mono<List<EmployeeRosterSlot>> getRosterSlots(long employeeId) {
        return this.rosterSlotRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<EmployeeRosterSlot>> saveRosterSlots(List<EmployeeRosterSlot> slots) {
        return this.rosterSlotRepository.saveAll(slots).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<EmployeeRosterSlot> findSlot(long slotId) {
        return this.rosterSlotRepository.findById(slotId);
    }

    public Mono<Void> deleteSlot(EmployeeRosterSlot slot) {
        return this.rosterSlotRepository.delete(slot);
    }

    public Mono<AbsenceRequest> saveAbsenceRequest(AbsenceRequest request) {
        return this.absenceRequestRepository.save(request);
    }
    public Mono<AbsenceRequest> getAbsenceRequest(String id) {
        return this.absenceRequestRepository.findById(id);
    }

    public Mono<List<SubdivisionRosterSlot>> saveSubdivisionRosterSlots(List<SubdivisionRosterSlot> slots){
        return this.subdivisionRosterSlotRepository.saveAll(slots).collectList();
    }

    public Mono<RosterPattern> savePattern(RosterPattern pattern){
        return this.rosterPatternRepository.save(pattern);
    }

    public Mono<List<RosterPattern>> getPatterns(long providerId){
        return this.rosterPatternRepository.findAllByProviderId(providerId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<RosterSlotColor> saveColor(RosterSlotColor color){
        return this.rosterSlotColorRepository.save(color);
    }

    public Mono<List<RosterSlotColor>> getColors(long providerId){
        return this.rosterSlotColorRepository.findAllByProviderId(providerId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }
}
