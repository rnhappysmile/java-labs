package com.rnhappysmile.java_labs.module.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;
    private final String issuer;

    public JwtProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.access-token.expiration-time}") long accessTokenExpirationTime,
            @Value("${jwt.refresh-token.expiration-time}") long refreshTokenExpirationTime,
            @Value("${jwt.issuer}") String issuer) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
        this.issuer = issuer;
    }

    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessTokenExpirationTime);
    }

    public String createRefreshToken(String email, String role) {
        return createToken(email, role, refreshTokenExpirationTime);
    }

    private String createToken(String email, String role, long expirationTime) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Object roleClaim = claims.get("role");
        Collection<? extends GrantedAuthority> authorities;

        if (roleClaim != null && !roleClaim.toString().isEmpty()) {
            authorities = Arrays.stream(roleClaim.toString().split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = Collections.emptyList();
        }

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰 내용이 잘못되었습니다.");
        }
        return false;
    }

    public String getUserEmail(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
