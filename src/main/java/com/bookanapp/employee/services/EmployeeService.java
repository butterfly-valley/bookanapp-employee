package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.repositories.*;
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
    private final EmployeeTimeOffRepository employeeTimeOffRepository;
    private final EmployeePhoneRepository employeePhoneRepository;
    private final EmployeeFamilyRepository employeeFamilyRepository;
    private final EmployeeAddressRepository employeeAddressRepository;
    private final AuthorizedScheduleRepository authorizedScheduleRepository;


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

    public Mono<List<Employee>> getAllEmployeesByName(long providerId, String term) {
        return this.employeeRepository.getAllByProviderIdAndNameContaining(providerId, term).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<EmployeeTimeOffBalance> getTimeOff(long id) {
        return this.employeeTimeOffRepository.getByEmployeeId(id);
    }

    public Mono<EmployeeAddress> getAddress(long id) {
        return this.employeeAddressRepository.getByEmployeeId(id);
    }

    public Mono<List<EmployeeFamilyMember>> getFamily(long employeeId) {
        return this.employeeFamilyRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<EmployeePhone>> getPhones(long employeeId) {
        return this.employeePhoneRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<AuthorizedSchedule>> getAuthorizedSchedules(long employeeId) {
        return this.authorizedScheduleRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

}
