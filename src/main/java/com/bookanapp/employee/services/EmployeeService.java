package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.Division;
import com.bookanapp.employee.entities.Employee;
import com.bookanapp.employee.entities.Subdivision;
import com.bookanapp.employee.repositories.DivisionRepository;
import com.bookanapp.employee.repositories.EmployeeRepository;
import com.bookanapp.employee.repositories.SubdivisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DivisionRepository divisionRepository;
    private final SubdivisionRepository subdivisionRepository;


    public Mono<Employee> getEmployee(long id) {
        return this.employeeRepository.findById(id);
    }

    public Mono<Division> getDivision(long id) {
        return this.divisionRepository.findById(id);
    }

    public Mono<Subdivision> getSubdivision(long id) {
        return this.subdivisionRepository.findById(id);
    }

    public Mono<List<Employee>> getAllEmployees(long providerId) {
        return this.employeeRepository.getAllByProviderId(providerId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }
}
