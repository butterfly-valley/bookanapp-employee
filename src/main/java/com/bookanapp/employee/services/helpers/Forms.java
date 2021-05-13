package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.rest.EmployeeAuthority;
import com.bookanapp.employee.entities.rest.EmployeeEntity;
import com.bookanapp.employee.validation.Authorities;
import com.bookanapp.employee.validation.IdToDelete;
import com.bookanapp.employee.validation.Password;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;


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
    public static class ListOfStringsForm {
        List<String> strings;
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

}
