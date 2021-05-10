package com.bookanapp.employee.entities.rest;

import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Objects;


public class UserAuthority implements GrantedAuthority, Serializable

{
    private static final long serialVersionUID = 1L;

    private String authority;

    public UserAuthority() { }

    public UserAuthority(String authority)
    {
        this.authority = authority;
    }

    @Override
    public String getAuthority()
    {
        return this.authority;
    }

    public void setAuthority(String authority)
    {
        this.authority = authority;
    }

    @Override
    public String toString() {
        return authority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAuthority that = (UserAuthority) o;
        return authority.equals(that.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority);
    }
}


