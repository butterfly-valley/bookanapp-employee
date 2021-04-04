package com.bookanapp.employee.entities;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeFamilyMember {

    private Kinship kinship;
    private String name;

    public enum Kinship{
        SPOUSE,
        PARTNER,
        CHILD
    }
}
