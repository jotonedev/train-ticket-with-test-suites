package edu.fudan.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;

public class JWTGenerator {

    public static String generateJwtTokenAdmin() {
        Claims claims = Jwts.claims().setSubject("admin");
        claims.put("roles", new HashSet<>(Arrays.asList("ROLE_ADMIN")));
        claims.put("id", "00000000-0000-0000-0000-000000000000");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encodeToString("secret".getBytes()))
                .compact();
    }

    public static String generateJwtTokenUser() {
        Claims claims = Jwts.claims().setSubject("fdse_microservice");
        claims.put("roles", new HashSet<>(Arrays.asList("ROLE_USER")));
        claims.put("id", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encodeToString("secret".getBytes()))
                .compact();
    }
}
