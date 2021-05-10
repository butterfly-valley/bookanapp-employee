package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.EmployeeTimeOffBalance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EmployeeTimeOffRepository extends ReactiveCrudRepository<EmployeeTimeOffBalance, Long> {
    Mono<EmployeeTimeOffBalance> getByEmployeeId(long employeeId);
}
