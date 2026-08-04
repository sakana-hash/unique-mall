package io.github.sakana.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTUtil {

    public static String issueJWT(String secretKey, long ttlMillis, String subject) {
        Map<String, Object> claims = new HashMap<>();
        return issueJWT(secretKey, ttlMillis, subject, claims);
    }

    public static String issueJWT(String secretKey, long ttlMillis, String subject, Map<String, Object> claims) {

        Date issuedAt = new Date();
        Date expireAt = new Date(issuedAt.getTime() + ttlMillis);
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        if (claims == null || claims.isEmpty()) {
            return Jwts.builder().subject(subject)
                    .issuedAt(issuedAt)
                    .expiration(expireAt)
                    .signWith(key)
                    .compact();
        }

        return Jwts.builder().subject(subject)
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expireAt)
                .signWith(key)
                .compact();
    }

    public static Claims parseJWT(String secretKey, String jwt) throws ExpiredJwtException, MalformedJwtException, SignatureException {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
