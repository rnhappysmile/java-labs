package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.common.error.ErrorCode;
import com.rnhappysmile.java_labs.common.error.exception.BusinessException;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private void addTokenToCookie(HttpServletResponse response, String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setHttpOnly(name.equals("refreshToken"));
        cookie.setSecure(false);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = getRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 2. 서비스 로직 호출 (RTR 수행 및 토큰 세트 변환)
        TokenDto tokenDto = authService.reissue(refreshToken);

        // 3. 응답 쿠키 설정 (보안 강화: HttpOnly, Secure 등)
        addTokenToCookie(response, "accessToken", tokenDto.getNewAccessToken(), 3600, false);
        addTokenToCookie(response, "refreshToken", tokenDto.getNewRefreshToken(), 1209600, true);

        log.info("토큰 재발급 및 로테이션 성공");
        return ResponseEntity.ok(Map.of("message", "토큰이 성공적으로 갱신되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 요청에서 Access Token 추출
        String accessToken = resolveToken(request);
        
        // 2. 쿠키에서 Refresh Token 추출
        String refreshToken = getRefreshTokenFromCookie(request);

        // 3. 서비스 로직 호출 (Blacklist 등록 및 Refresh Token 삭제)
        if (accessToken != null) {
            authService.logout(accessToken, refreshToken);
        }

        // 4. 쿠키 무효화
        expireCookie(response, "accessToken");
        expireCookie(response, "refreshToken");

        log.info("로그아웃 성공");
        return ResponseEntity.ok(Map.of("message", "성공적으로 로그아웃되었습니다."));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 쿠키에서도 확인 (기존 reissue 로직 참고)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("accessToken")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void expireCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
