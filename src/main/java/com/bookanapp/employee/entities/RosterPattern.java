package com.bookanapp.employee.entities;

import com.bookanapp.employee.validation.IdToDelete;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.Size;
import java.time.LocalTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RosterPattern {

    @Id
    long id;
    long providerId;
    @Size(max = 30)
    private String name;
    @Size(max = 5)
    private String pattern;
    private LocalTime start;
    private LocalTime end;

    public RosterPattern(long providerId, String name, String pattern, LocalTime start, LocalTime end) {
        this.providerId = providerId;
        this.name = name;
        this.pattern = pattern;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RosterPattern that = (RosterPattern) o;
        return providerId == that.providerId && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, name);
    }
}
