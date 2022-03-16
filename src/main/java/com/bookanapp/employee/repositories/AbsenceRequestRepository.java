package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.TimeRequest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AbsenceRequestRepository extends ReactiveCrudRepository<TimeRequest, String> {
    Flux<TimeRequest> getAllByEmployeeId(long employeeId);
}
