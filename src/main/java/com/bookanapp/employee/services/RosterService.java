package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.EmployeeRosterSlot;
import com.bookanapp.employee.entities.AbsenceRequest;
import com.bookanapp.employee.repositories.EmployeeRosterSlotRepository;
import com.bookanapp.employee.repositories.AbsenceRequestRepository;
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
}
