package com.rnhappysmile.java_labs.module.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rnhappysmile.java_labs.module.auth.domain.RefreshToken;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.repository.RefreshTokenRepository;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 자체의 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. Redis에서 Refresh Token 조회
        RefreshToken storedToken = refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new RuntimeException("해당 토큰으로 사용자를 찾을 수 없습니다. (이미 사용되었거나 잘못된 토큰)"));

        // 3. DB에서 사용자 조회 (Roles 갱신 등을 위해)
        User user = userRepository.findByEmail(storedToken.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 4. 새로운 토큰 세트 생성
        String newAccessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole().name());

        // 5. Redis 업데이트 (Rotation) - 기존 토큰 삭제 및 새 토큰 저장
        refreshTokenRepository.delete(storedToken);
        // Expiration is in ms in config, convert to seconds for Redis TTL
        refreshTokenRepository.save(new RefreshToken(newRefreshToken, user.getEmail(), refreshTokenExpirationTime / 1000));

        return new TokenDto(newAccessToken, newRefreshToken);
    }
}
