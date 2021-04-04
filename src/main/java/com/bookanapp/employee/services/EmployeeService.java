package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;


    public Mono<Employee> getEmployee(long id) {
        return this.employeeRepository.findById(id);
    }
}
