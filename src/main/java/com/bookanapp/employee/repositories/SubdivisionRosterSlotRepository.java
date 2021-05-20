package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.SubdivisionRosterSlot;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface SubdivisionRosterSlotRepository extends ReactiveCrudRepository<SubdivisionRosterSlot, Long> {
    Flux<SubdivisionRosterSlot> findAllBySubdivisionIdAndDate(long id, LocalDate date);
}
