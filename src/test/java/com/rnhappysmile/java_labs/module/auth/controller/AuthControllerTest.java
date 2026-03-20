package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.module.auth.domain.Role;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    @DisplayName("성공: 유효한 Refresh Token으로 새로운 Access Token을 발급받는다")
    void refresh_token_success() throws Exception {
        // [Given]
        String oldRefreshToken = "old-refresh-token";
        String userEmail = "test@test.com";
        User user = User.builder()
                .email(userEmail)
                .role(Role.USER)
                .build();
        user.updateRefreshToken(oldRefreshToken);

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(jwtProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(userRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userEmail, Role.USER.name())).thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(userEmail, Role.USER.name())).thenReturn(newRefreshToken);

        // [When & Then]
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf()) // CSRF 필터 대응
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("accessToken", newAccessToken))
                .andExpect(cookie().value("refreshToken", newRefreshToken))
                .andExpect(jsonPath("$.message").value("토큰이 성공적으로 갱신되었습니다."));

        // RTR 검증: 새로운 리프레시 토큰이 DB에 저장되었는지 확인
        verify(userRepository).save(any(User.class));
    }

    @Test
    @WithMockUser
    @DisplayName("실패: 유효하지 않은 Refresh Token으로 요청하면 401을 반환한다")
    void refresh_token_invalid() throws Exception {
        // [Given]
        String invalidToken = "invalid-token";
        when(jwtProvider.validateToken(invalidToken)).thenReturn(false);

        // [When & Then]
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", invalidToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token입니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("실패: DB에 존재하지 않는 토큰으로 요청하면 401을 반환한다")
    void refresh_token_not_found_in_db() throws Exception {
        // [Given]
        String validTokenButNotStored = "valid-but-not-stored";
        when(jwtProvider.validateToken(validTokenButNotStored)).thenReturn(true);
        when(userRepository.findByRefreshToken(validTokenButNotStored)).thenReturn(Optional.empty());

        // [When & Then]
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", validTokenButNotStored)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("해당 토큰과 일치하는 사용자가 없습니다."));
    }
}
