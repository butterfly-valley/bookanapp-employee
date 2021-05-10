package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.EmployeeAddress;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EmployeeAddressRepository extends ReactiveCrudRepository<EmployeeAddress, Long> {
    Mono<EmployeeAddress> getByEmployeeId(long employeeId);
}
