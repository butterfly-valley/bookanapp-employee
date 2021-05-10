package com.bookanapp.employee.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;



@Data
public class Address {

//    @Id
//    private long id;
    private Long employeeId;

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
