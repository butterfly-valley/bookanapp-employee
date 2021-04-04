package com.bookanapp.employee.entities.rest;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProviderAuthority implements GrantedAuthority {

    private long providerId;
    private String authority;

    public ProviderAuthority(String authority) {
        this.authority = authority;
    }
}


