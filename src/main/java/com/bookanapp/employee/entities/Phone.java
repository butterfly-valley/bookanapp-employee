package com.bookanapp.employee.entities;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;


import java.util.Objects;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Phone {

    @Id
    private long id;
    private Long employeeId;
    private PhoneType phoneType;
    private String code;
    private String number;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone that = (Phone) o;
        return Objects.equals(code, that.code) && Objects.equals(number, that.number);
    }

    public Phone(Long employeeId, PhoneType phoneType, String code, String number) {
        this.employeeId = employeeId;
        this.phoneType = phoneType;
        this.code = code;
        this.number = number;
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
