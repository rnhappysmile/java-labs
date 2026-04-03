package com.rnhappysmile.java_labs.module.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 요청마다 JWT 토큰을 검증하는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더 또는 쿠키에서 JWT 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사 및 Blacklist 체크
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            // 2.1 Blacklist 체크
            if (isBlacklisted(token)) {
                log.warn("이미 로그아웃된 토큰(Blacklisted)으로 접근을 시도했습니다.");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 토큰이 유효하면 인증 정보(Authentication) 추출
            Authentication authentication = jwtProvider.getAuthentication(token);
            // 4. SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
        }

        // 5. 다음 필터로 이동
        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

    /**
     * HTTP Header 또는 Cookie 에서 JWT 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization Header 확인
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Cookie 확인 (Header에 없을 경우)
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
