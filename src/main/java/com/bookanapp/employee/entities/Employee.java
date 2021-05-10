package com.bookanapp.employee.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class Employee {

    @Column("employee_id")
    @Id
    private long id;
    private String username;
    private String name;
    private LocalDate registerDate;
    private long providerId;
    @Transient
    private List<AuthorizedSchedule> authorizedSchedules=new ArrayList<>();
    private String avatar;
    private Long subdivisionId;
    private String personalEmail;
    private String jobTitle;
    private String bankAccount;
    private String taxPayerId;
    @Transient
    private EmployeeAddress address;
    @Transient
    private List<EmployeePhone> phones = new ArrayList<>();
    @Transient
    private List<EmployeeFamilyMember> family = new ArrayList<>();
    @Transient
    private EmployeeTimeOffBalance timeOffBalance;
    @Transient
    private Subdivision subdivision;
}
