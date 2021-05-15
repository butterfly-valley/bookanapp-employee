package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.EmployeeRosterSlot;
import com.bookanapp.employee.repositories.EmployeeRosterSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RosterService {

    private final EmployeeRosterSlotRepository rosterSlotRepository;

    public Mono<List<EmployeeRosterSlot>> getRosterSlots(long employeeId) {
        return this.rosterSlotRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<EmployeeRosterSlot>> saveRosterSlots(List<EmployeeRosterSlot> slots) {
        return this.rosterSlotRepository.saveAll(slots).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }
}
