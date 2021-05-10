package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Division;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface DivisionRepository extends ReactiveCrudRepository<Division, Long> {
}
