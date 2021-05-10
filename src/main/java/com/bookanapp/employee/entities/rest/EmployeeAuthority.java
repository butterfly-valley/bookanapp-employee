package com.bookanapp.employee.entities.rest;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeAuthority extends UserAuthority {

    @Id
    private long id;
    private long employeeId;

    public EmployeeAuthority(String authority) {
        super(authority);
    }
}


