package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Subdivision;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SubdivisionRepository extends ReactiveCrudRepository<Subdivision, Long> {
}
