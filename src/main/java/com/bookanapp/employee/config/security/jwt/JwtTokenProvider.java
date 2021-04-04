package com.bookanapp.employee.config.security.jwt;


import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtTokenProvider {

    private final Logger log= LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;


    public Map<Long, Map<String, List<String>>> getProviderIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        String subject = claims.getSubject();
        String authorities = claims.getAudience();

        Map<Long, Map<String, List<String>>> principalMap = new HashMap<>();
        Map<String, List<String>> authorityMap = new HashMap<>();

        List<String> authorityList = Arrays.asList(authorities.split(","));

        if  (subject.contains("providerId-")) {
            authorityMap.put("provider", authorityList);
            principalMap.put(Long.parseLong(subject.split("providerId-")[1]), authorityMap);
        }

        if  (subject.contains("subProviderId-")) {
            authorityMap.put("subProvider", authorityList);
            principalMap.put(Long.parseLong(subject.split("subProviderId-")[1]), authorityMap);
        }

        if (principalMap.size()>0) {
            return principalMap;
        } else {
            return null;
        }

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
