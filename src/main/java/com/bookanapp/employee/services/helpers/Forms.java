package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.entities.rest.EmployeeEntity;
import lombok.Data;


import java.util.List;


public class Forms {

    @Data
    public static class EmployeeMap {
        private int total;
        private List<EmployeeEntity> entities;
    }
}
