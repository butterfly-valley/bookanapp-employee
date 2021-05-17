package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.AbsenceRequest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AbsenceRequestRepository extends ReactiveCrudRepository<AbsenceRequest, String> {
    Flux<AbsenceRequest> getAllByEmployeeId(long employeeId);
}
