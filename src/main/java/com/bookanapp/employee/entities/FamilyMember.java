package com.bookanapp.employee.entities;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMember {

    @Id
    private long id;
    private Long employeeId;
    private int kinship;
    private String name;

}
