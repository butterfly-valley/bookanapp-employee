package com.bookanapp.employee.entities;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Objects;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePhone {

    private PhoneType phoneType;
    private String code;
    private String number;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeePhone that = (EmployeePhone) o;
        return Objects.equals(code, that.code) && Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, number);
    }

    public enum PhoneType{
        CELL,
        LANDLINE,
        MISC
    }
}
