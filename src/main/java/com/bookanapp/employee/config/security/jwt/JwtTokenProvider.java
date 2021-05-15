package com.bookanapp.employee.config.security.jwt;


import com.bookanapp.employee.entities.rest.EmployeeDetails;
import com.bookanapp.employee.entities.rest.ProviderAuthority;
import com.bookanapp.employee.entities.rest.ProviderDetails;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtTokenProvider {

    private final Logger log= LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;


    public UserDetails getUserDetailsFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        String subject = claims.getSubject();
        String authorities = claims.getAudience();

        List<String> authorityList = Arrays.asList(authorities.split(","));

        List<ProviderAuthority> authList = new ArrayList<>();
        authorityList.forEach(
                auth -> {
                    authList.add(new ProviderAuthority(auth));
                }
        );


        if  (subject.contains("providerId-")) {
            return new ProviderDetails(Long.parseLong(subject.split("providerId-")[1]), authList);

        }

        if  (subject.contains("subProviderId-")) {
            String ids = subject.split("subProviderId-")[1];
            long employeeId = Long.parseLong(ids.split("#")[0]);
            long providerId = Long.parseLong(ids.split("#")[1]);
            return new EmployeeDetails(employeeId, authList, providerId);
        }

        return null;

    }

    public boolean getAPIClientFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        String client = claims.getSubject();
        String authorities = claims.getAudience();

        return client.equals("API_CLIENT") && authorities.equals("API_CLIENT");
    }

    public String generateApiToken() {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject("API_CLIENT")
                .setAudience("API_CLIENT")
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();

    }

    /*validate JWT*/
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid token");
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Expired token");
        }   catch (MalformedJwtException ex) {
            throw new RuntimeException("Invalid token");
        }  catch (UnsupportedJwtException ex) {
            throw new RuntimeException("Invalid token");
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid token");
        }

    }

    /*validate JWT no exception thrown*/
    public String validateTokenNoException(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return "Valid token";
        } catch (SignatureException e) {
            return "Invalid token";
        } catch (ExpiredJwtException ex) {
            return "Expired token";
        }   catch (MalformedJwtException ex) {
            return "Invalid token";
        }  catch (UnsupportedJwtException ex) {
            return "Invalid token";
        } catch (IllegalArgumentException ex) {
            return "Invalid token";
        }

    }


//    public String generateToken(Authentication authentication) {
//
//        Object principal = authentication.getPrincipal();
//
////        ProviderDetails provider = null;
////        SubProviderDetails subProvider = null;
//        if (principal instanceof ProviderDetails)
//            provider = (ProviderDetails) authentication.getPrincipal();
//        if  (principal instanceof SubProviderDetails)
//            subProvider = (SubProviderDetails) authentication.getPrincipal();
//
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
//
//        String subject = null;
//        if (provider !=null)
//            subject = "providerId-" + provider.getId();
//        if (subProvider !=null)
//            subject = "subProviderId-" + subProvider.getId();
//
//        if (subject!=null) {
//            return Jwts.builder()
//                    .setSubject(subject)
//                    .setIssuedAt(new Date())
//                    .setExpiration(expiryDate)
//                    .signWith(SignatureAlgorithm.HS512, jwtSecret)
//                    .compact();
//        } else {
//            return "invalidPrincipal";
//        }
//    }

}
