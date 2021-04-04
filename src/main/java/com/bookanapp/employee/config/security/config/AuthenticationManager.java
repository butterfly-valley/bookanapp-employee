package com.bookanapp.employee.config.security.config;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationManager implements ReactiveAuthenticationManager {

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication.getAuthorities() != null && authentication.getAuthorities().size() > 0) {
            return Mono.just(authentication);
        } else {
            return Mono.empty();
        }
    }
}
