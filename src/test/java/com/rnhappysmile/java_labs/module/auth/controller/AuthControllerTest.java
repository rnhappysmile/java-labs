package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.common.error.ErrorCode;
import com.rnhappysmile.java_labs.common.error.exception.BusinessException;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.security.JwtAuthenticationFilter;
import com.rnhappysmile.java_labs.module.auth.security.OAuth2SuccessHandler;
import com.rnhappysmile.java_labs.module.auth.service.AuthService;
import com.rnhappysmile.java_labs.module.auth.service.CustomOAuth2UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // SecurityConfig에서 사용하는 빈들을 Mock으로 선언하여 의존성 주입 오류를 방지합니다.
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private OAuth2SuccessHandler oauth2SuccessHandler;

    @Test
    @DisplayName("성공: /reissue 호출 시 새로운 토큰 세트를 발급받고 쿠키에 저장한다")
    void reissue_success() throws Exception {
        // [Given]
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        TokenDto tokenDto = new TokenDto(newAccessToken, newRefreshToken);

        when(authService.reissue(oldRefreshToken)).thenReturn(tokenDto);

        // [When & Then]
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("accessToken", newAccessToken))
                .andExpect(cookie().value("refreshToken", newRefreshToken))
                .andExpect(jsonPath("$.message").value("토큰이 성공적으로 갱신되었습니다."));

        verify(authService).reissue(oldRefreshToken);
    }

    @Test
    @DisplayName("실패: /reissue 호출 시 Refresh Token이 없으면 401을 반환한다")
    void reissue_fail_no_cookie() throws Exception {
        // [When & Then]
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("토큰을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("실패: 서비스 레이어에서 예외 발생 시 해당 에러 상태를 반환한다")
    void reissue_fail_service_exception() throws Exception {
        // [Given]
        String refreshToken = "invalid-token";
        when(authService.reissue(refreshToken)).thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        // [When & Then]
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    @DisplayName("성공: /logout 호출 시 쿠키가 만료되고 200을 반환한다")
    void logout_success() throws Exception {
        // [Given]
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        // [When & Then]
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("accessToken", accessToken))
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(jsonPath("$.message").value("성공적으로 로그아웃되었습니다."));

        verify(authService).logout(accessToken, refreshToken);
    }
}
