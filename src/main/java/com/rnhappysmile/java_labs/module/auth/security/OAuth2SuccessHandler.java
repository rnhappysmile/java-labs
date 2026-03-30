package com.rnhappysmile.java_labs.module.auth.security;

import com.rnhappysmile.java_labs.module.auth.domain.RefreshToken;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.PrincipalDetails;
import com.rnhappysmile.java_labs.module.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();

        // 1. 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole().name());

        // 2. Refresh Token Redis 저장
        refreshTokenRepository.save(new RefreshToken(refreshToken, user.getEmail(), refreshTokenExpirationTime / 1000));

        // 3. 응답 헤더/쿠키 설정
        addTokenToCookie(response, "accessToken", accessToken, 3600); // 1시간
        addTokenToCookie(response, "refreshToken", refreshToken, (int) (refreshTokenExpirationTime / 1000)); // 14일 (HttpOnly)

        log.info("OAuth2 로그인 성공: {}, Access Token 및 Refresh Token 발급 완료", user.getEmail());

        // 4. 메인 페이지로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, "/");
    }

    private void addTokenToCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(name.equals("refreshToken")); // Refresh Token은 JavaScript에서 접근 불가하게 설정
        cookie.setSecure(false); // 로컬 테스트 환경이므로 false, 운영 환경(HTTPS)에서는 true 권장
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
