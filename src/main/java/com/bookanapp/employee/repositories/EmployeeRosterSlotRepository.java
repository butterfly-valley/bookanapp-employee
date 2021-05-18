package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.EmployeeRosterSlot;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmployeeRosterSlotRepository extends ReactiveCrudRepository<EmployeeRosterSlot, Long> {
    Flux<EmployeeRosterSlot> findAllByEmployeeId(long employeeId);
}