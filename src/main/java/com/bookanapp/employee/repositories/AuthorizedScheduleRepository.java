package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.AuthorizedSchedule;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AuthorizedScheduleRepository extends ReactiveCrudRepository<AuthorizedSchedule, Long> {
    Flux<AuthorizedSchedule> findAllByEmployeeId(long employeeId);
}
