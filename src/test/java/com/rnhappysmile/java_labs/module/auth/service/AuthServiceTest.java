package com.rnhappysmile.java_labs.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rnhappysmile.java_labs.module.auth.domain.Role;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("성공: 유효한 Refresh Token으로 재발급 시 RTR이 적용되어 새로운 토큰 세트를 반환한다")
    void reissue_success() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String email = "test@example.com";
        String newRefreshToken = "new-refresh-token";
        String newAccessToken = "new-access-token";

        User user = User.builder()
                .name("테스터")
                .email(email)
                .role(Role.USER)
                .build();
        user.updateRefreshToken(oldRefreshToken);

        when(jwtProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(userRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(email, Role.USER.name())).thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(email, Role.USER.name())).thenReturn(newRefreshToken);

        // when
        TokenDto result = authService.reissue(oldRefreshToken);

        // then
        assertThat(result.getNewAccessToken()).isEqualTo(newAccessToken);
        assertThat(result.getNewRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(user.getRefreshToken()).isEqualTo(newRefreshToken); // RTR 확인: 사용자의 리프레시 토큰이 갱신됨

        verify(jwtProvider).validateToken(oldRefreshToken);
        verify(userRepository).findByRefreshToken(oldRefreshToken);
        verify(jwtProvider).createAccessToken(email, Role.USER.name());
        verify(jwtProvider).createRefreshToken(email, Role.USER.name());
        verify(userRepository).save(user); 
    }

    @Test
    @DisplayName("실패: Refresh Token이 유효하지 않으면 예외가 발생해야 한다")
    void reissue_invalid_refreshToken_throw() {
        // given
        String invalidToken = "invalid-token";
        when(jwtProvider.validateToken(invalidToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않거나 만료된 Refresh Token입니다.");

        verify(jwtProvider).validateToken(invalidToken);
        verify(userRepository, never()).findByRefreshToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: DB에 해당 Refresh Token이 없으면 예외가 발생해야 한다")
    void reissue_refreshToken_not_found_throw() {
        // given
        String refreshToken = "refreshToken";
        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(userRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 토큰으로 사용자를 찾을 수 없습니다.");

        verify(jwtProvider).validateToken(refreshToken);
        verify(userRepository).findByRefreshToken(refreshToken);
        verify(userRepository, never()).save(any());
    }
}

