package com.bookanapp.employee.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class IdToDeleteValidator
        implements ConstraintValidator<IdToDelete, List<String>>
{
    @Override
    public void initialize(IdToDelete annotation)
    {

    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context)
    {

        if (value.size()<1)
            return false;
        for (String s : value) {
            if (!s.matches("^[1-9]\\d*$") || s.length() < 1)
                return false;
        }

        return true;
    }
}
