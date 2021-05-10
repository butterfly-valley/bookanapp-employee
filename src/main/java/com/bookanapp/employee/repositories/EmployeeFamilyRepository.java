package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.EmployeeFamilyMember;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmployeeFamilyRepository extends ReactiveCrudRepository<EmployeeFamilyMember, Long> {
    Flux<EmployeeFamilyMember> getAllByEmployeeId(long employeeId);
}
