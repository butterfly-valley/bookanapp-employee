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
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                    Map<Long, Map<String, List<String>>> principalMap;
                    try {
                        principalMap = this.jwtTokenProvider.getProviderIdFromJWT(jwt);
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalidUser");
                    }
                    if (principalMap != null) {
                        //Process redirect
                        Map.Entry<Long, Map<String, List<String>>> firstEntry = principalMap.entrySet().iterator().next();
                        Map.Entry<String, List<String>> firstEntryOfEntry = firstEntry.getValue().entrySet().iterator().next();
                        UserDetails userDetails;

                        List<ProviderAuthority> authorities = new ArrayList<>();
                        firstEntryOfEntry.getValue().forEach(
                                auth -> {
                                    authorities.add(new ProviderAuthority(auth));
                                }
                        );

                        if (firstEntryOfEntry.getKey().equals("provider")) {
                            userDetails = new ProviderDetails(firstEntry.getKey(), authorities);
                        } else {
                            userDetails = new EmployeeDetails(firstEntry.getKey(), authorities);
                        }

                        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDetails, null, authorities))
                                .flatMap(authentication -> webFilterChain.filter(serverWebExchange)
                                        .subscriberContext(c -> ReactiveSecurityContextHolder.withAuthentication(authentication)))
                                .onErrorResume(AuthenticationException.class, e -> {
                                    log.error("Authentication Exception", e);
                                    return webFilterChain.filter(serverWebExchange);
                                });



                    } else {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is null");
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

}
