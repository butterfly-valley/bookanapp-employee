package com.bookanapp.employee.entities;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;



@Data
public class EmployeeAddress implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull
    @Size(min = 3, max = 300)
    private String street;
    private String postalCode;
    @Size(max = 20)
    private String country;
    @NotNull
    @Size(min=2)
    private String city;
    private String province;
}
