package com.bookanapp.employee.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RosterSlotColor {
    @Id
    private long id;
    private long providerId;
    private String name;
    private String color;

    public RosterSlotColor(long providerId, String name, String color) {
        this.providerId = providerId;
        this.name = name;
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RosterSlotColor that = (RosterSlotColor) o;
        return providerId == that.providerId && Objects.equals(name, that.name) && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, name, color);
    }
}
