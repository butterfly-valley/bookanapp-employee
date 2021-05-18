package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.*;
import com.bookanapp.employee.entities.rest.EmployeeAuthority;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.validation.Authorities;
import com.bookanapp.employee.validation.IdToDelete;
import com.bookanapp.employee.validation.Password;
import lombok.*;


import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class Forms {

    @Data
    public static class EmployeeMap {
        private int total;
        private List<EmployeeEntity> entities;
    }

    @Data
    @AllArgsConstructor
    public static class GenericResponse {
        String message;
    }

    @Data
    @AllArgsConstructor
    public static class SetOfStringsForm {
        Set<String> strings;
    }

    @Data
    public static class DeleteForm {
        @IdToDelete
        List<String> idsToDelete;

    }

    @Data
    public static class NewEmployeeForm{
        @NotBlank
        String name;
        @Email
        String email;
        @Email
        String personalEmail;
        @Size(min = 1)
        @Authorities
        @NotNull
        List<String> authorizations;
        @Password
        String password;
        List<String> availabilityIds;
        List<String> subdivisionIds;
        @Size(max = 255)
        String jobTitle;
        boolean allSchedules;
        String division;
        String subdivision;
        Long subdivisionId;
        EmployeeEntity.TimeOffEntity timeOffBalance;
        String hireDate;
        String taxId;
    }

    @Data
    @AllArgsConstructor
    public static class EmployeeRegistrationForm {
        String username;
        String password;
        long providerId;
        List<EmployeeAuthority> authorities;
    }

    @Data
    public static class DivisionForm {
        @NotBlank
        String divisionName;
        @NotBlank
        String subdivisionName;
        @NotNull
        Long divisionId;
        @NotNull
        Long subdivisionId;
    }

    @Data
    @AllArgsConstructor
    public static class FileUploadResponse {
        private String link;
        private String error;

    }

    @Data
    public  static class TimeOffRequestForm{
        @NotNull
        LocalDate initialDate;
        @NotNull
        float numberOfDays;
        @NotNull
        String balanceType;
    }

    @Data
    @AllArgsConstructor
    static class TimeOffRequestNotificationForm {
        Provider provider;
        Employee employee;
        String recipient;
    }


    @Data
    public static class RosterDay{
        @NotBlank
        String weekday;
        @NotNull
        List<RosterDaySchedule> schedule;

        @Getter
        @Setter
        public static class RosterDaySchedule{
            @NotNull
            RosterDayScheduleHour start;
            @NotNull
            RosterDayScheduleHour end;

            @Getter
            @Setter
            public static class RosterDayScheduleHour{
                @NotNull
                int hour;
                @NotNull
                int minute;
            }

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Day day = (Day) o;
            return Objects.equals(weekday, day.weekday);
        }

        @Override
        public int hashCode() {
            return Objects.hash(weekday);
        }
    }

    @Data
    public static class Day{
        @NotBlank
        String weekday;
        @NotNull
        List<DaySchedule> schedule;

        @Getter
        @Setter
        public static class DaySchedule{
            RosterDay.RosterDaySchedule.RosterDayScheduleHour start;
            RosterDay.RosterDaySchedule.RosterDayScheduleHour end;

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Day day = (Day) o;
            return Objects.equals(weekday, day.weekday);
        }

        @Override
        public int hashCode() {
            return Objects.hash(weekday);
        }
    }


    @Data
    public static class AbsenceRequestForm {

        @NotNull
        LocalDate date;
        RosterDay.RosterDaySchedule.RosterDayScheduleHour start;
        RosterDay.RosterDaySchedule.RosterDayScheduleHour end;
        String comments;
        boolean overtime;

    }

    @Data
    public static class ProfileEditForm {

        List<FamilyMember> family;
        List<Phone> phones;
        String bankAccount;
        Address address;
        @Email
        String personalEmail;

    }

    @Data
    static class Phone{
        InternationalPhone phone;
        @NotNull
        com.bookanapp.employee.entities.Phone.PhoneType type;
    }

    @Data
    @NoArgsConstructor
    static class InternationalPhone{
        String number;
        String internationalNumber;
        String nationalNumber;
        String countryCode;
        String dialCode;
        String id;
    }

    @Data
    public static class RosterSuperForm {
        @NotNull
        Roster schedule;
        String pattern;
        RosterDay.RosterDaySchedule.RosterDayScheduleHour patternStart;
        RosterDay.RosterDaySchedule.RosterDayScheduleHour patternEnd;
        @Size(max = 36)
        String color;
        String note;
        @Size(max = 30)
        String colorName;
        boolean publish;
    }

    @Data
    static class Roster{
        @NotBlank
        String startDate;
        @NotBlank
        String endDate;
        @NotNull
        RosterDays days;

    }

    @Data
    static class RosterDays{
        @NotNull
        Set<RosterDay> day;

    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class RosterForm extends RosterSuperForm{
        @NotBlank
        String employeeId;
        String patternName;
    }


}
