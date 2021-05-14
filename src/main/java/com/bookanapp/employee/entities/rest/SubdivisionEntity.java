package com.bookanapp.employee.entities.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubdivisionEntity {
    private String division;
    private String subdivision;
    private long divisionId;
    private long subdivisionId;
}
