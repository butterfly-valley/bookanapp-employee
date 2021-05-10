package com.bookanapp.employee.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class TimeOffBalance {

    @Id
    private long id;
    private long employeeId;

    private float vacationDays;
    private float vacationRolloverDays;
    private float complimentaryBankHolidayDays;
    private float complimentaryBankHolidayRolloverDays;
    private float compensationDays;
    private float compensationRolloverDays;

    public TimeOffBalance(long employeeId,
                          float vacationDays, float vacationRolloverDays,
                          float complimentaryBankHolidayDays,
                          float complimentaryBankHolidayRolloverDays, float compensationDays, float compensationRolloverDays) {
        this.employeeId = employeeId;
        this.vacationDays = vacationDays;
        this.vacationRolloverDays = vacationRolloverDays;
        this.complimentaryBankHolidayDays = complimentaryBankHolidayDays;
        this.complimentaryBankHolidayRolloverDays = complimentaryBankHolidayRolloverDays;
        this.compensationDays = compensationDays;
        this.compensationRolloverDays = compensationRolloverDays;
    }

}
