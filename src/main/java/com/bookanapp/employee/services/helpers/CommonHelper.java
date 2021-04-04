package com.bookanapp.employee.services.helpers;

import com.bookanapp.employee.config.security.jwt.JwtTokenProvider;
import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.entities.rest.ProviderDetails;
import com.bookanapp.employee.entities.rest.SubProviderDetails;
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
                    if (authentication.getPrincipal() instanceof SubProviderDetails){
                        user=(SubProviderDetails) authentication.getPrincipal();
                    } else {
                        user=(ProviderDetails) authentication.getPrincipal();
                    }
                    return Mono.just(user);

                }
        );
    }

    public Mono<Provider> getCurrentProvider() {
        return getCurrentUser()
                .flatMap(userDetails -> {
                    if (userDetails instanceof SubProviderDetails){
                        return this.employeeService.getEmployee(((SubProviderDetails) userDetails).getId())
                                .flatMap(employee -> {
                                    //retrieve provider details
                                    var client = this.buildAPIAccessWebClient(this.providerServiceUrl + "/provider/get/" + employee.getProviderId());
                                    return client.get()
                                            .retrieve()
                                            .bodyToMono(Provider.class)
                                            .flatMap(Mono::just);
                                            });

                    } else {

                        var client = this.buildAPIAccessWebClient(this.providerServiceUrl + "/provider/get/" + ((ProviderDetails) userDetails).getId());
                        return client.get()
                                .retrieve()
                                .bodyToMono(Provider.class)
                                .flatMap(Mono::just);
                    }

                });

    }

    public WebClient buildAPIAccessWebClient(String url) {
        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + this.jwtTokenProvider.generateApiToken())
                .build();

    }
}
