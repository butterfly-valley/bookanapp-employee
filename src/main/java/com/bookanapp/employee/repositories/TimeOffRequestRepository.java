package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.TimeOffRequest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TimeOffRequestRepository extends ReactiveCrudRepository<TimeOffRequest, Long> {
    Flux<TimeOffRequest> getAllByEmployeeId(long employeeId);
}
