package com.bookanapp.employee.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Employee {

    @Column("id")
    @Id
    private long entityId;
    private Long employeeId;
    private String username;
    private String name;
    private LocalDate registerDate;
    private long providerId;
    @Transient
    private List<Long> authorizedSchedules=new ArrayList<>();
    private String avatar;
    private Long subdivisionId;
    private String personalEmail;
    private String jobTitle;
    private String bankAccount;
    private String taxPayerId;
    private LocalDate dob;
    @Transient
    private Address address;
    @Transient
    private List<Phone> phones = new ArrayList<>();
    @Transient
    private List<FamilyMember> family = new ArrayList<>();
    @Transient
    private TimeOffBalance timeOffBalance;
    @Transient
    private Subdivision subdivision;
    @Transient
    private Set<String> authorities = new HashSet<>();
    @Transient
    private List<String> authorizedScheduleNames = new ArrayList<>();
    @Transient
    private List<Long> authorizedRosters = new ArrayList<>();
}
