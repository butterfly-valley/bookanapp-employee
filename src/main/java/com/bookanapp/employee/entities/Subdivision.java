package com.bookanapp.employee.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Subdivision {

    @Id
    private long subdivisionId;
    private long divisionId;
    @NotBlank
    private String name;
    @Transient
    private List<SubdivisionRosterSlot> rosterSlots = new ArrayList<>();
    @Transient
    private Division division;

    public Subdivision(long divisionId, String name) {
        this.divisionId = divisionId;
        this.name = name;
    }
}
