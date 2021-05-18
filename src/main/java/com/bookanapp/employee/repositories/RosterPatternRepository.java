package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.RosterPattern;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RosterPatternRepository extends ReactiveCrudRepository<RosterPattern, Long> {
    Flux<RosterPattern> findAllByProviderId(long providerId);
}
