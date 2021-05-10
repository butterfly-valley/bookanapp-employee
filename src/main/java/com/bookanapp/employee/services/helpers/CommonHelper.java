package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.config.security.jwt.JwtTokenProvider;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.entities.rest.ProviderDetails;
import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CommonHelper {

    private final Mono<SecurityContext> context  = ReactiveSecurityContextHolder.getContext();
    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeService employeeService;

    @Value("${app.providerServiceUrl}")
    private String providerServiceUrl;

    public Mono<UserDetails> getCurrentUser() {
        return context.flatMap(
                context -> {
                    UserDetails user;
                    Authentication authentication = context.getAuthentication();
                    if (authentication.getPrincipal() instanceof EmployeeDetails){
                        user=(EmployeeDetails) authentication.getPrincipal();
                    } else {
                        user=(ProviderDetails) authentication.getPrincipal();
                    }
                    return Mono.just(user);

                }
        );
    }

    public Mono<Long> getCurrentProviderId() {
        return context.flatMap(
                context -> {
                    UserDetails userDetails = (UserDetails) context.getAuthentication().getPrincipal();
                    if (userDetails instanceof ProviderDetails) {
                        return Mono.just(((ProviderDetails) userDetails).getId());
                    } else if (userDetails instanceof EmployeeDetails){
                        return Mono.just(((EmployeeDetails) userDetails).getProviderId());
                    } else {
                        return Mono.empty();
                    }

                }
        );

    }
    public WebClient buildAPIAccessWebClient(String url) {
        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + this.jwtTokenProvider.generateApiToken())
                .build();

    }
}
