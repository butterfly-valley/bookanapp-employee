package com.bookanapp.employee.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AuthoritiesValidator implements ConstraintValidator<Authorities, List<String>> {
    @Override
    public void initialize(Authorities annotation) {

    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        List<String> authorities= Arrays.asList("SUBPROVIDER_SCHED", "SUBPROVIDER_SCHED_VIEW", "SUBPROVIDER_PAY", "SUBPROVIDER_ADMIN","SUBPROVIDER_FULL",
                "SUBPROVIDER_ROSTER", "SUBPROVIDER_ROSTER_VIEW");
        if (value!=null && value.size()>0)
            return !Collections.disjoint(authorities, value);

        return true;
    }
}