package com.rnhappysmile.java_labs.module.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 자체의 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. DB에서 해당 Refresh Token을 가진 사용자 조회 (RTR: 항상 DB의 최신 토큰과 비교)
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("해당 토큰으로 사용자를 찾을 수 없습니다. (이미 사용되었거나 잘못된 토큰)"));

        // 3. 새로운 토큰 세트 생성
        String newAccessToken = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getRole().name());

        // 4. DB의 Refresh Token 업데이트 (Rotation)
        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new TokenDto(newAccessToken, newRefreshToken);
    }
}
