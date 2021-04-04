package com.bookanapp.employee.entities.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubProviderDetails implements UserDetails, CredentialsContainer, Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String username;
    @Transient
    private String password;
    private List<ProviderAuthority> authorities = new ArrayList<>();
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

    public SubProviderDetails(long id, List<ProviderAuthority> authorities) {
        this.id = id;
        this.authorities = authorities;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    @Override
    public void eraseCredentials() {

    }

    @Override
    public List<ProviderAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword()
    {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }
    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public String toString() {
        return "SubProvider{" +
                ", username='" + username + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubProviderDetails)) return false;
        SubProviderDetails that = (SubProviderDetails) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }
}
