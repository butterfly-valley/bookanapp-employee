package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Address;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EmployeeAddressRepository extends ReactiveCrudRepository<Address, Long> {
    Mono<Address> getByEmployeeId(long employeeId);
}
