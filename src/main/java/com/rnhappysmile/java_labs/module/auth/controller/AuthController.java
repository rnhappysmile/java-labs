package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(cookie -> cookie.getName().equals("refreshToken"))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("message", "유효하지 않은 Refresh Token입니다."));
        }

        // 2. DB에서 해당 Refresh Token을 가진 사용자 조회
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "해당 토큰과 일치하는 사용자가 없습니다."));
        }

        // 3. 새로운 Access Token 발급
        String newAccessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        
        // 4. (선택) Refresh Token Rotation: 새로운 Refresh Token도 발급하여 보안 강화
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole().name());
        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);

        // 5. 응답에 새로운 토큰 설정
        addTokenToCookie(response, "accessToken", newAccessToken, 3600);
        addTokenToCookie(response, "refreshToken", newRefreshToken, 1209600);

        log.info("사용자 {} 의 Access Token 및 Refresh Token이 갱신되었습니다.", user.getEmail());

        return ResponseEntity.ok(Map.of("message", "토큰이 성공적으로 갱신되었습니다."));
    }

    private void addTokenToCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(name.equals("refreshToken"));
        cookie.setSecure(false);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
