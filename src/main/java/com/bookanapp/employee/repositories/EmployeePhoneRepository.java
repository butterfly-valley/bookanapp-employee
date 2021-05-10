package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Phone;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmployeePhoneRepository extends ReactiveCrudRepository<Phone, Long> {
    Flux<Phone> getAllByEmployeeId(long employeeId);
}
