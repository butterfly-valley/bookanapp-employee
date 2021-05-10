package com.bookanapp.employee.entities;


import lombok.*;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMember {

    @Id
    private long id;
    private Long employeeId;
    private int kinship;
    private String name;

    public enum Kinship{
        SPOUSE,
        PARTNER,
        CHILD
    }
}
