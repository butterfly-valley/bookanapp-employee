package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.FamilyMember;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface FamilyRepository extends ReactiveCrudRepository<FamilyMember, Long> {
    Flux<FamilyMember> getAllByEmployeeId(long employeeId);
}
