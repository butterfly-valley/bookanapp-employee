package com.bookanapp.employee.entities.rest;

import com.bookanapp.employee.entities.EmployeeAddress;
import com.bookanapp.employee.entities.EmployeeFamilyMember;
import com.bookanapp.employee.entities.EmployeePhone;
import com.bookanapp.employee.entities.EmployeeTimeOffBalance;
import lombok.*;

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
    private Set<Long> authorizedSchedules=new HashSet<>();
    private Set<Long> authorizedRosters=new HashSet<>();
    private Set<String> authorizedScheduleNames=new HashSet<>();
    private String avatar;
    private String division;
    private String subdivision;
    private Long subdivisionId;
    private Long divisionId;
    private String jobTitle;
    private TimeOffEntity timeOffBalance;
    private EmployeeAddress homeAddress;
    private List<EmployeePhone> phones;
    private List<EmployeeFamilyMember> family;
    private String bankAccount;
    private String taxPayerId;
    private String personalEmail;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeOffEntity {

        public TimeOffEntity(EmployeeTimeOffBalance timeOffBalance) {
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
