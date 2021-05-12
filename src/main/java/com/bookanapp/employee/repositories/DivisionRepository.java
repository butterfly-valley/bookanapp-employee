package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Division;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DivisionRepository extends ReactiveCrudRepository<Division, Long> {
    Flux<Division> findAllByProviderId(long providerId);
    Mono<Division> findByProviderIdAndName(long providerId, String name);
}
