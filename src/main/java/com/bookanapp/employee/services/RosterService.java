package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
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

    public Mono<List<EmployeeRosterSlot>> getRosterSlotsByDate(long employeeId, LocalDate date) {
        return this.rosterSlotRepository.findAllByEmployeeIdAndDate(employeeId, date).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>()))).retry(3);
    }

    public Mono<List<EmployeeRosterSlot>> getRosterSlotsInInterval(long employeeId, List<LocalDate> dates) {
        return this.rosterSlotRepository.findAllByEmployeeIdAndDateIn(employeeId, dates).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>()))).retry(3);
    }



    public Mono<List<EmployeeRosterSlot>> saveRosterSlots(List<EmployeeRosterSlot> slots) {
        return this.rosterSlotRepository.saveAll(slots).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<EmployeeRosterSlot> saveRosterSlot(EmployeeRosterSlot slot) {
        return this.rosterSlotRepository.save(slot);
    }

    public Mono<SubdivisionRosterSlot> saveSubdivisionRosterSlot(SubdivisionRosterSlot slot) {
        return this.subdivisionRosterSlotRepository.save(slot);
    }

    public Mono<List<SubdivisionRosterSlot>> saveSubdivisionRosterSlots(List<SubdivisionRosterSlot> slots) {
        return this.subdivisionRosterSlotRepository.saveAll(slots).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }


    public Mono<EmployeeRosterSlot> findSlot(long slotId) {
        return this.rosterSlotRepository.findById(slotId);
    }

    public Mono<Void> deleteSlot(EmployeeRosterSlot slot) {
        return this.rosterSlotRepository.delete(slot);
    }

    public Mono<Void> deleteSlots(List<EmployeeRosterSlot> slots) {
        return this.rosterSlotRepository.deleteAll(slots);
    }

    public Mono<Void> deleteSubdivisionSlot(SubdivisionRosterSlot slot) {
        return this.subdivisionRosterSlotRepository.delete(slot);
    }

    public Mono<Void> deleteSubdivisionSlots(List<SubdivisionRosterSlot> slots) {
        return this.subdivisionRosterSlotRepository.deleteAll(slots);
    }


    public Mono<TimeRequest> saveAbsenceRequest(TimeRequest request) {
        return this.absenceRequestRepository.save(request);
    }
    public Mono<TimeRequest> getAbsenceRequest(String id) {
        return this.absenceRequestRepository.findById(id);
    }


    public Mono<List<SubdivisionRosterSlot>> findSubdivisionRosterSlotsByDate(long id, LocalDate date){
        return this.subdivisionRosterSlotRepository.findAllBySubdivisionIdAndDate(id, date).collectList();
    }

    public Mono<List<SubdivisionRosterSlot>> findSubdivisionRosterSlotsByDateInRange(long id, List<LocalDate> dates){
        return this.subdivisionRosterSlotRepository.findAllBySubdivisionIdAndDateIn(id, dates).collectList();
    }


    public Mono<SubdivisionRosterSlot> findSubdivisionRosterSlot(long id){
        return this.subdivisionRosterSlotRepository.findById(id);
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
