package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Employee;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmployeeRepository extends ReactiveCrudRepository<Employee, Long> {

    Flux<Employee> getAllByProviderId(long providerId);
    Flux<Employee> getAllByProviderIdAndNameContaining(long providerId, String term);
}
