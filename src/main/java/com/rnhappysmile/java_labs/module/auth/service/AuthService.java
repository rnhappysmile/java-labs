package com.rnhappysmile.java_labs.module.auth.service;

import com.rnhappysmile.java_labs.common.error.ErrorCode;
import com.rnhappysmile.java_labs.common.error.exception.BusinessException;
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
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

    @Value("${jwt.refresh-token.grace-period-seconds:10}")
    private long refreshTokenGracePeriodSeconds;

    public void logout(String accessToken, String refreshToken) {
        // 1. Access Token 유효성 검증 (이미 필터에서 검증되었겠지만 한 번 더 체크 가능)
        if (!jwtProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Access Token의 남은 유효 시간을 계산하여 Blacklist에 등록
        Long expiration = jwtProvider.getExpiration(accessToken);
        redisTemplate.opsForValue()
                .set("blacklist:" + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);

        // 3. Redis에서 Refresh Token 삭제
        if (refreshToken != null) {
            refreshTokenRepository.findById(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 자체의 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Redis에서 Refresh Token 조회
        RefreshToken storedToken = refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new BusinessException("해당 토큰으로 사용자를 찾을 수 없습니다. (이미 사용되었거나 잘못된 토큰)", ErrorCode.INVALID_TOKEN));

        // 2.1. RTR Grace Period 체크: 이미 회전된 토큰인 경우 저장된 새 토큰 반환 (Idempotency)
        if (storedToken.isRotated()) {
            return new TokenDto(storedToken.getNewAccessToken(), storedToken.getNewRefreshToken());
        }

        // 3. DB에서 사용자 조회 (Roles 갱신 등을 위해)
        User user = userRepository.findByEmail(storedToken.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 새로운 토큰 세트 생성
        String newAccessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole().name());

        // 5. Redis 업데이트 (Rotation) 
        // 5.1. 기존 토큰을 '회전됨' 상태로 업데이트 (Grace Period 적용)
        storedToken.markAsRotated(newAccessToken, newRefreshToken, refreshTokenGracePeriodSeconds);
        refreshTokenRepository.save(storedToken);

        // 5.2. 새 토큰 저장
        // Expiration is in ms in config, convert to seconds for Redis TTL
        refreshTokenRepository.save(new RefreshToken(newRefreshToken, user.getEmail(), refreshTokenExpirationTime / 1000));

        return new TokenDto(newAccessToken, newRefreshToken);
    }
}
