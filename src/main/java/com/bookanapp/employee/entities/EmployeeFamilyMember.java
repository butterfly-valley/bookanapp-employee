package com.bookanapp.employee.entities;


import lombok.*;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeFamilyMember {

    @Id
    private long id;
    private Long employeeId;
    private Kinship kinship;
    private String name;

    public enum Kinship{
        SPOUSE,
        PARTNER,
        CHILD
    }
}
