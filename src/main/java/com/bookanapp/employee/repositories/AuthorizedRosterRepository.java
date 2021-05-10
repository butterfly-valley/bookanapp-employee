package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.AuthorizedRoster;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AuthorizedRosterRepository extends ReactiveCrudRepository<AuthorizedRoster, Long> {

    Flux<AuthorizedRoster> findAllByEmployeeId(long employeeId);
}
