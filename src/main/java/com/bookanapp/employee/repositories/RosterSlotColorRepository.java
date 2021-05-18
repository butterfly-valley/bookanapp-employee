package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.RosterSlotColor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RosterSlotColorRepository extends ReactiveCrudRepository<RosterSlotColor, Long> {
    Flux<RosterSlotColor> findAllByProviderId(long providerId);
}
