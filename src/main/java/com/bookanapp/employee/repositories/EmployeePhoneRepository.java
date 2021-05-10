package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.EmployeePhone;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmployeePhoneRepository extends ReactiveCrudRepository<EmployeePhone, Long> {
    Flux<EmployeePhone> getAllByEmployeeId(long employeeId);
}
