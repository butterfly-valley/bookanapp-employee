package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Subdivision;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SubdivisionRepository extends ReactiveCrudRepository<Subdivision, Long> {
    Flux<Subdivision> findAllByDivisionId(long divisionId);
    Mono<Subdivision> findByDivisionIdAndName(long divisionId, String name);
    Flux<Subdivision> findAllByDivisionIdAndNameIgnoreCaseContaining(long divisionId, String term);

}
