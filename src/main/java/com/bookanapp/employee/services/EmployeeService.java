package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.AuthorizedRoster;
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
    private final AuthorizedRosterRepository authorizedRosterRepository;
    private final TimeOffRequestRepository timeOffRequestRepository;


    public Mono<Employee> getEmployee(long employeeId) {
        return this.employeeRepository.getByEmployeeId(employeeId);
    }

    public Mono<Employee> getEmployeeByUsername(long providerId, String username) {
        return this.employeeRepository.getByProviderIdAndUsername(providerId, username);
    }
    public Mono<Employee> saveEmployee(Employee employee) {
        return this.employeeRepository.save(employee);
    }
    public Mono<Void> deleteEmployee(Employee employee) {
        return this.employeeRepository.delete(employee);
    }
    public Mono<Division> getDivision(long id) {
        return this.divisionRepository.findById(id);
    }
    public Mono<Division> saveDivision(Division division) {
        return this.divisionRepository.save(division);
    }

    public Mono<Subdivision> getSubdivision(long id) {
        return this.subdivisionRepository.findById(id);
    }

    public Mono<Subdivision> saveSubdivision(Subdivision subdivision) {
        return this.subdivisionRepository.save(subdivision);
    }
    public Mono<List<Subdivision>> getSubdivisions(long divisionId) {
        return this.subdivisionRepository.findAllByDivisionId(divisionId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<Subdivision> getSubdivisionByName(long divisionId, String name) {
        return this.subdivisionRepository.findByDivisionIdAndName(divisionId, name);
    }
    public Mono<List<Division>> getDivisions(long providerId) {
        return this.divisionRepository.findAllByProviderId(providerId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<Division> getDivisionByName(long providerId, String name) {
        return this.divisionRepository.findByProviderIdAndName(providerId, name);
    }

    public Mono<List<Employee>> getAllEmployees(long providerId) {
        return this.employeeRepository.getAllByProviderId(providerId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<Employee>> getAllEmployeesByName(long providerId, String term) {
        return this.employeeRepository.getAllByProviderIdAndNameContaining(providerId, term).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<TimeOffBalance> getTimeOff(long id) {
        return this.employeeTimeOffRepository.getByEmployeeId(id);
    }
    public Mono<TimeOffBalance> saveTimeOff(TimeOffBalance balance) {
        return this.employeeTimeOffRepository.save(balance);
    }

    public Mono<Address> getAddress(long id) {
        return this.employeeAddressRepository.getByEmployeeId(id);
    }

    public Mono<List<FamilyMember>> getFamily(long employeeId) {
        return this.employeeFamilyRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<Phone>> getPhones(long employeeId) {
        return this.employeePhoneRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<AuthorizedSchedule>> getAuthorizedSchedules(long employeeId) {
        return this.authorizedScheduleRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<Void> deleteAuthorizedSchedules(List<AuthorizedSchedule> authorizedSchedules) {
        return this.authorizedScheduleRepository.deleteAll(authorizedSchedules);
    }


    public Mono<List<AuthorizedSchedule>> saveAuthorizedSchedules(List<AuthorizedSchedule> authorizedSchedules) {
        return this.authorizedScheduleRepository.saveAll(authorizedSchedules).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<AuthorizedRoster>> getAuthorizedRosters(long employeeId) {
        return this.authorizedRosterRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<AuthorizedRoster>> saveAuthorizedRosters(List<AuthorizedRoster> authorizedRosters) {
        return this.authorizedRosterRepository.saveAll(authorizedRosters).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }


    public Mono<List<TimeOffRequest>> getTimeOffRequest(long employeeId) {
        return this.timeOffRequestRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }


}
