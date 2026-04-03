package com.rnhappysmile.java_labs.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rnhappysmile.java_labs.common.error.ErrorCode;
import com.rnhappysmile.java_labs.common.error.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.rnhappysmile.java_labs.module.auth.domain.RefreshToken;
import com.rnhappysmile.java_labs.module.auth.domain.Role;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.repository.RefreshTokenRepository;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationTime", 1209600000L);
    }

    @Test
    @DisplayName("성공: 로그아웃 시 Access Token은 블랙리스트에 등록되고 Refresh Token은 삭제된다")
    void logout_success() {
        // given
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        Long expiration = 3600L;

        RefreshToken storedToken = new RefreshToken(refreshToken, "test@example.com", 1000L);

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.getExpiration(accessToken)).thenReturn(expiration);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.findById(refreshToken)).thenReturn(Optional.of(storedToken));

        // when
        authService.logout(accessToken, refreshToken);

        // then
        verify(jwtProvider).validateToken(accessToken);
        verify(jwtProvider).getExpiration(accessToken);
        verify(valueOperations).set(eq("blacklist:" + accessToken), eq("logout"), eq(expiration), any());
        verify(refreshTokenRepository).findById(refreshToken);
        verify(refreshTokenRepository).delete(storedToken);
    }

    @Test
    @DisplayName("성공: 유효한 Refresh Token으로 재발급 시 Redis에서 조회 및 갱신된다")
    void reissue_success() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String email = "test@example.com";
        String newRefreshToken = "new-refresh-token";
        String newAccessToken = "new-access-token";

        RefreshToken storedToken = new RefreshToken(oldRefreshToken, email, 1000L);
        User user = User.builder()
                .name("테스터")
                .email(email)
                .role(Role.USER)
                .build();

        when(jwtProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findById(oldRefreshToken)).thenReturn(Optional.of(storedToken));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(email, Role.USER.name())).thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(email, Role.USER.name())).thenReturn(newRefreshToken);

        // when
        TokenDto result = authService.reissue(oldRefreshToken);

        // then
        assertThat(result.getNewAccessToken()).isEqualTo(newAccessToken);
        assertThat(result.getNewRefreshToken()).isEqualTo(newRefreshToken);

        verify(jwtProvider).validateToken(oldRefreshToken);
        verify(refreshTokenRepository).findById(oldRefreshToken);
        verify(userRepository).findByEmail(email);
        verify(refreshTokenRepository).delete(storedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("실패: Refresh Token이 유효하지 않으면 예외가 발생해야 한다")
    void reissue_invalid_refreshToken_throw() {
        // given
        String invalidToken = "invalid-token";
        when(jwtProvider.validateToken(invalidToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(invalidToken))
                .isInstanceOf(BusinessException.class);

        verify(jwtProvider).validateToken(invalidToken);
        verify(refreshTokenRepository, never()).findById(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: Redis에 해당 Refresh Token이 없으면 예외가 발생해야 한다")
    void reissue_refreshToken_not_found_throw() {
        // given
        String refreshToken = "refreshToken";
        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(refreshTokenRepository.findById(refreshToken)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 토큰으로 사용자를 찾을 수 없습니다.");

        verify(jwtProvider).validateToken(refreshToken);
        verify(refreshTokenRepository).findById(refreshToken);
        verify(refreshTokenRepository, never()).save(any());
    }
}
