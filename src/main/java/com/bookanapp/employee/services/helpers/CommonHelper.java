package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.config.security.jwt.JwtTokenProvider;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.entities.rest.ProviderDetails;
import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonHelper {

    private final Mono<SecurityContext> context  = ReactiveSecurityContextHolder.getContext();
    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeService employeeService;

    @Value("${app.providerServiceUrl}")
    private String providerServiceUrl;

    @Value("${app.authServiceUrl}")
    public String authServiceUrl;

    @Value("${app.appointmentServiceUrl}")
    public String appointmentServiceUrl;

    @Value("${app.notificationServiceUrl}")
    public String notificationServiceUrl;

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

    public Mono<ResponseEntity> returnErrorMessage(Throwable e, String toLog, String message){
        if (e instanceof WebExchangeBindException) {
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse("bindingError")));
        } else {
            log.error(toLog + e.getMessage());
            return Mono.just(ResponseEntity.ok(new Forms.GenericResponse(message)));
        }
    }
}
