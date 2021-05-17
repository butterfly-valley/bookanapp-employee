package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.TimeOffBalance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TimeOffBalanceRepository extends ReactiveCrudRepository<TimeOffBalance, Long> {
    Mono<TimeOffBalance> getByEmployeeId(long employeeId);
}
