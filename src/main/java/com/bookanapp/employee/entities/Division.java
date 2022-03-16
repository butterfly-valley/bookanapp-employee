package com.bookanapp.employee.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
public class Division {
    @Id
    private long divisionId;
    private long providerId;
    @NotBlank
    private String name;
    @Transient
    List<Subdivision> subdivisions = new ArrayList<>();

    public Division(long providerId, String name) {
        this.providerId = providerId;
        this.name = name;
    }
}
