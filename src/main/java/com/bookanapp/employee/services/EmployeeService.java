package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.AuthorizedRoster;
import com.bookanapp.employee.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DivisionRepository divisionRepository;
    private final SubdivisionRepository subdivisionRepository;
    private final TimeOffBalanceRepository timeOffBalanceRepository;
    private final EmployeePhoneRepository employeePhoneRepository;
    private final FamilyRepository familyRepository;
    private final EmployeeAddressRepository employeeAddressRepository;
    private final AuthorizedScheduleRepository authorizedScheduleRepository;
    private final AuthorizedRosterRepository authorizedRosterRepository;
    private final AbsenceRequestRepository absenceRequestRepository;


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


    public Mono<List<Employee>> getAllEmployees(long providerId, Pageable pageable) {
        return this.employeeRepository.getAllByProviderId(providerId, pageable).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<Employee>> findAllUnassignedEmployees(long providerId) {
        return this.employeeRepository.getAllByProviderIdAndSubdivisionIdIsNull(providerId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }


    public Mono<List<Employee>> getAllEmployeesByName(long providerId, String term) {
        return this.employeeRepository.getAllByProviderIdAndNameContaining(providerId, term).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<Employee>> searchAllEmployeesByName(long providerId, String term) {
        return this.employeeRepository.getAllByProviderIdAndNameContaining(providerId, term).collectList()
                .switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<Employee>> findAllDivisionEmployeesByName(long providerId, String term) {
        return this.divisionRepository.findAllByProviderIdAndNameIgnoreCaseContaining(providerId, term).collectList()
                .flatMap(divisions -> Flux.fromIterable(divisions)
                        .flatMap(division -> this.getSubdivisions(division.getDivisionId())
                                .flatMap(this::findAllSubdivisionEmployees)
                        )
                        .collectList()
                        .flatMap(lists -> {
                            List<Employee> employees = new ArrayList<>();
                            lists.forEach(employees::addAll);
                            return Mono.just(employees);
                        }))
                .switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<Employee>> findAllSubdivisionEmployeesByName(long providerId, String term) {
        return this.divisionRepository.findAllByProviderId(providerId).collectList()
                .flatMap(divisions -> Flux.fromIterable(divisions)
                        .flatMap(division -> this.subdivisionRepository.findAllByDivisionIdAndNameIgnoreCaseContaining(division.getDivisionId(), term).collectList()
                                .flatMap(this::findAllSubdivisionEmployees)
                        )
                        .collectList()
                        .flatMap(lists -> {
                            List<Employee> employees = new ArrayList<>();
                            lists.forEach(employees::addAll);
                            return Mono.just(employees);
                        }));
    }


    public Mono<List<Employee>> findAllSubdivisionEmployees(List<Subdivision> subdivisions) {
        return Flux.fromIterable(subdivisions)
                .flatMap(subdivision -> this.employeeRepository.getAllBySubdivisionId(subdivision.getSubdivisionId()))
                .collectList();
    }

    public Mono<List<Employee>> findEmployeesBySubdivision(long subdivisionId) {
        return this.employeeRepository.getAllBySubdivisionId(subdivisionId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<TimeOffBalance> getTimeOffBalance(long id) {
        return this.timeOffBalanceRepository.getByEmployeeId(id);
    }
    public Mono<TimeOffBalance> saveTimeOffBalance(TimeOffBalance balance) {
        return this.timeOffBalanceRepository.save(balance);
    }

    public Mono<Address> getAddress(long id) {
        return this.employeeAddressRepository.getByEmployeeId(id).switchIfEmpty(Mono.defer(() -> Mono.just(new Address())));
    }

    public Mono<Address> saveAddress(Address address) {
        return this.employeeAddressRepository.save(address);
    }


    public Mono<List<FamilyMember>> getFamily(long employeeId) {
        return this.familyRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<FamilyMember>> saveFamily(List<FamilyMember> familyMembers) {
        return this.familyRepository.saveAll(familyMembers).collectList();
    }

    public Mono<Void> deleteFamily(List<FamilyMember> familyMembers) {
        return this.familyRepository.deleteAll(familyMembers);
    }
    public Mono<List<Phone>> getPhones(long employeeId) {
        return this.employeePhoneRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<Phone> getPhone(long phoneId) {
        return this.employeePhoneRepository.findById(phoneId);
    }

    public Mono<FamilyMember> getFamilyMember(long memberId) {
        return this.familyRepository.findById(memberId);
    }

    public Mono<Void> deletePhone(Phone phone) {
        return this.employeePhoneRepository.delete(phone);
    }

    public Mono<Void> deleteFamilyMember(FamilyMember member) {
        return this.familyRepository.delete(member);
    }

    public Mono<List<Phone>> savePhones(List<Phone> phones) {
        return this.employeePhoneRepository.saveAll(phones).collectList();
    }

    public Mono<Void> deletePhones(List<Phone> phones) {
        return this.employeePhoneRepository.deleteAll(phones);
    }


    public Mono<List<AuthorizedSchedule>> getAuthorizedSchedules(long employeeId) {
        return this.authorizedScheduleRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<Void> deleteAuthorizedSchedules(List<AuthorizedSchedule> authorizedSchedules) {
        return this.authorizedScheduleRepository.deleteAll(authorizedSchedules);
    }

    public Mono<Void> deleteAuthorizedRosters(List<AuthorizedRoster> authorizedRosters) {
        return this.authorizedRosterRepository.deleteAll(authorizedRosters);
    }



    public Mono<List<AuthorizedSchedule>> saveAuthorizedSchedules(List<AuthorizedSchedule> authorizedSchedules) {
        return this.authorizedScheduleRepository.saveAll(authorizedSchedules).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Mono<List<AuthorizedRoster>> getAuthorizedRosters(long employeeId) {
        return this.authorizedRosterRepository.findAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }

    public Flux<Subdivision> getSubdivisionsByListOfIds(List<Long> ids) {
        return this.subdivisionRepository.findAllById(ids);
    }

    public Mono<List<AuthorizedRoster>> saveAuthorizedRosters(List<AuthorizedRoster> authorizedRosters) {
        return this.authorizedRosterRepository.saveAll(authorizedRosters).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }


    public Mono<List<TimeRequest>> getTimeOffRequest(long employeeId) {
        return this.absenceRequestRepository.getAllByEmployeeId(employeeId).collectList().switchIfEmpty(Mono.defer(() -> Mono.just(new ArrayList<>())));
    }


}
