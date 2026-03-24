package com.rnhappysmile.java_labs.module.auth.controller;

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
            return ResponseEntity.status(401).body(Map.of("message", "Refresh Token이 존재하지 않습니다."));
        }

        try {
            // 2. 서비스 로직 호출 (RTR 수행 및 토큰 세트 변환)
            TokenDto tokenDto = authService.reissue(refreshToken);

            // 3. 응답 쿠키 설정 (보안 강화: HttpOnly, Secure 등)
            addTokenToCookie(response, "accessToken", tokenDto.getNewAccessToken(), 3600, false);
            addTokenToCookie(response, "refreshToken", tokenDto.getNewRefreshToken(), 1209600, true);

            log.info("토큰 재발급 및 로테이션 성공");
            return ResponseEntity.ok(Map.of("message", "토큰이 성공적으로 갱신되었습니다."));
        } catch (RuntimeException e) {
            log.error("토큰 재발급 실패: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
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
