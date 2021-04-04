package com.bookanapp.employee.repositories;

import com.bookanapp.employee.entities.Employee;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EmployeeRepository extends ReactiveCrudRepository<Employee, Long> {
}
