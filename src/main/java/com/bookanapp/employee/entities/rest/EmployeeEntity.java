package com.bookanapp.employee.entities.rest;

import com.bookanapp.employee.entities.Address;
import com.bookanapp.employee.entities.FamilyMember;
import com.bookanapp.employee.entities.Phone;
import com.bookanapp.employee.entities.TimeOffBalance;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeEntity {
    private long id;
    private String username;
    private String name;
    private String registerDate;
    private Set<String> authorities = new HashSet<>();
    private List<Long> authorizedSchedules=new ArrayList<>();
    private List<Long> authorizedRosters=new ArrayList<>();
    private List<String> authorizedScheduleNames=new ArrayList<>();
    private String avatar;
    private String division;
    private String subdivision;
    private Long subdivisionId;
    private Long divisionId;
    private String jobTitle;
    private TimeOffEntity timeOffBalance;
    private Address homeAddress;
    private List<Phone> phones;
    private List<FamilyMember> family;
    private String bankAccount;
    private String taxPayerId;
    private String personalEmail;

    public EmployeeEntity(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeOffEntity {

        public TimeOffEntity(TimeOffBalance timeOffBalance) {
            this.vacationDays = timeOffBalance.getVacationDays();
            this.vacationRolloverDays = timeOffBalance.getVacationRolloverDays();
            this.complimentaryBankHolidayDays = timeOffBalance.getComplimentaryBankHolidayDays();
            this.complimentaryBankHolidayRolloverDays = timeOffBalance.getComplimentaryBankHolidayRolloverDays();
            this.compensationDays = timeOffBalance.getCompensationDays();
            this.compensationRolloverDays = timeOffBalance.getCompensationRolloverDays();
        }

        private float vacationDays;
        private float vacationRolloverDays;
        private float complimentaryBankHolidayDays;
        private float complimentaryBankHolidayRolloverDays;
        private float compensationDays;
        private float compensationRolloverDays;
    }
}
