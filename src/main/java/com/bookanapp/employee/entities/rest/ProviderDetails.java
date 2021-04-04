package com.bookanapp.employee.entities.rest;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data
public class ProviderDetails implements UserDetails, CredentialsContainer, Cloneable, Serializable {

    @Id
    private long id;
    private String username;
    @Transient
    private String password;
    @Transient
    private List<ProviderAuthority> authorities = new ArrayList<>();
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;


    public ProviderDetails(long id, List<ProviderAuthority> authorities) {
        this.username = "logged_provider";
        this.password = "password";
        this.id = id;
        this.authorities = authorities;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    @Override
    public String getPassword()
    {
        return this.password;
    }


    @Override
    public void eraseCredentials() { }


    @Override
    public List<ProviderAuthority> getAuthorities()
    {
        return this.authorities;
    }


    @Override
    public boolean isAccountNonExpired()
    {
        return this.accountNonExpired;
    }


    @Override
    public boolean isAccountNonLocked()
    {
        return this.accountNonLocked;
    }



    @Override
    public boolean isCredentialsNonExpired()
    {
        return this.credentialsNonExpired;
    }


    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }


    @Override
    public int hashCode()
    {
        return this.username.hashCode();
    }


    @Override
    public boolean equals(Object other)
    {
        return other instanceof ProviderDetails &&
                ((ProviderDetails)other).id == this.id;
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    protected ProviderDetails clone()
    {
        try {
            return (ProviderDetails) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // not possible
        }
    }

    @Override
    public String toString()
    {
        return this.username;
    }

}
