package com.bookanapp.employee.config.security.config;

import com.bookanapp.employee.config.security.jwt.JwtTokenProvider;
import com.bookanapp.employee.entities.rest.ProviderAuthority;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> save(ServerWebExchange swe, SecurityContext sc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange swe) {
        return Mono.justOrEmpty(swe.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")))
                .flatMap(authHeader -> {
                    String jwt = authHeader.substring(7);
                    Authentication auth;

                    if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                        UserDetails userDetails;
                        try {
                            userDetails = this.jwtTokenProvider.getUserDetailsFromJWT(jwt);
                        } catch (Exception e) {
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalidUser"));
                        }
                        if (userDetails != null) {
                            auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        } else {
                            boolean isApiClient;
                            try {
                                isApiClient = this.jwtTokenProvider.getAPIClientFromJWT(jwt);
                            } catch (Exception e) {
                                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalidUser"));
                            }

                            if (isApiClient) {
                                List<ProviderAuthority> authorities = new ArrayList<>();
                                authorities.add(new ProviderAuthority("API_CLIENT"));
                                auth = new UsernamePasswordAuthenticationToken(buildAPIClientUser(authorities), null, authorities);
                            } else {
                                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is null"));
                            }


                        }
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
                    }

                    return this.authenticationManager.authenticate(auth).map(SecurityContextImpl::new);
                });
    }


    private UserDetails buildAPIClientUser(List<ProviderAuthority> authorities) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return authorities;
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getUsername() {
                return null;
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }
}
