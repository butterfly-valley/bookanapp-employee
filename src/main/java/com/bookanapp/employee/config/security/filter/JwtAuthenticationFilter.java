package com.bookanapp.employee.config.security.filter;

import com.bookanapp.employee.config.security.jwt.JwtTokenProvider;
import com.bookanapp.employee.entities.rest.ProviderAuthority;
import com.bookanapp.employee.entities.rest.ProviderDetails;
import com.bookanapp.employee.entities.rest.EmployeeDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveAuthenticationManager authenticationManager;

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {

        ServerHttpRequest request = serverWebExchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        if(headers.get("Authorization") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        } else {
            String jwt = getJwtFromRequest(headers);

            if (jwt != null) {
                if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                    UserDetails userDetails;
                    try {
                        userDetails = this.jwtTokenProvider.getUserDetailsFromJWT(jwt);
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalidUser");
                    }
                    if (userDetails != null) {
                        //Process redirect
                        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                                .flatMap(authentication -> webFilterChain.filter(serverWebExchange)
                                        .subscriberContext(c -> ReactiveSecurityContextHolder.withAuthentication(authentication)))
                                .onErrorResume(AuthenticationException.class, e -> {
                                    log.error("Authentication Exception", e);
                                    return webFilterChain.filter(serverWebExchange);
                                });



                    } else {
                        boolean isApiClient;
                        try {
                            isApiClient = this.jwtTokenProvider.getAPIClientFromJWT(jwt);
                        } catch (Exception e) {
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalidUser");
                        }

                        if (isApiClient) {
                            List<ProviderAuthority> authorities = new ArrayList<>();
                            authorities.add(new ProviderAuthority("API_CLIENT"));
                            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(buildAPIClientUser(authorities), null, authorities))
                                    .flatMap(authentication -> webFilterChain.filter(serverWebExchange)
                                            .subscriberContext(c -> ReactiveSecurityContextHolder.withAuthentication(authentication)))
                                    .onErrorResume(AuthenticationException.class, e -> {
                                        log.error("Authentication Exception", e);
                                        return webFilterChain.filter(serverWebExchange);
                                    });

                        } else {
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is null");
                        }
                    }
                } else {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
                }

            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }

        }

    }
    private String getJwtFromRequest(HttpHeaders headers) {
        String bearerToken = Objects.requireNonNull(headers.get("Authorization")).get(0);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
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
