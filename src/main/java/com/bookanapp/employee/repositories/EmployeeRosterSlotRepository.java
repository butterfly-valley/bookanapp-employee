package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.EmployeeRosterSlot;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeRosterSlotRepository extends ReactiveCrudRepository<EmployeeRosterSlot, Long> {
    Flux<EmployeeRosterSlot> findAllByEmployeeId(long employeeId);

    Flux<EmployeeRosterSlot> findAllByEmployeeIdAndDate(long employeeId, LocalDate date);
    Flux<EmployeeRosterSlot> findAllByEmployeeIdAndDateIn(long employeeId, List<LocalDate> dates);
}

