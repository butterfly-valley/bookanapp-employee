package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.rest.EmployeeEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


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
    public static class ListStringForm {
        List<String> idsToDelete;

    }
}
