package com.rnhappysmile.java_labs.module.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter의 동작을 테스트합니다.
 */
class JwtAuthenticationFilterTest {

    private JwtProvider jwtProvider;
    private StringRedisTemplate redisTemplate;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, redisTemplate);
        filterChain = mock(FilterChain.class);
        
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("성공: 유효한 토큰이 헤더에 있고 블랙리스트가 아니면 SecurityContext에 인증 정보가 저장되어야 함")
    void valid_token_sets_authentication() throws ServletException, IOException {
        // [Given]
        String token = "valid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Authentication authentication = mock(Authentication.class);
        when(jwtProvider.validateToken(token)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(false);
        when(jwtProvider.getAuthentication(token)).thenReturn(authentication);

        // [When]
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // [Then]
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("실패: 유효한 토큰이지만 블랙리스트(로그아웃됨)에 있으면 인증 정보를 저장하지 않음")
    void blacklisted_token_does_not_set_authentication() throws ServletException, IOException {
        // [Given]
        String token = "blacklisted-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        when(jwtProvider.validateToken(token)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(true); // 블랙리스트에 있음

        // [When]
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // [Then]
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).getAuthentication(anyString());
    }

    @Test
    @DisplayName("실패: 토큰이 아예 없는 요청은 인증 절차를 건너뜀")
    void missing_token_does_not_set_authentication() throws ServletException, IOException {
        // [Given] Authorization 헤더가 없는 요청
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // [When] 필터 실행
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // [Then]
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull(); // 인증 정보가 없어야 함
        verify(filterChain).doFilter(request, response); // 필터 체인은 계속 진행되어야 함
        verify(jwtProvider, never()).validateToken(anyString()); // 토큰 검증 로직은 아예 호출되지 않아야 함
    }

    @Test
    @DisplayName("실패: 유효하지 않은 토큰(만료 등)은 인증 정보를 저장하지 않음")
    void invalid_token_does_not_set_authentication() throws ServletException, IOException {
        // [Given]
        String token = "invalid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // 검증 결과가 false가 나오도록 설정
        when(jwtProvider.validateToken(token)).thenReturn(false);

        // [When] 필터 실행
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // [Then]
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("성공: 유효한 토큰이 쿠키에 있으면 SecurityContext에 인증 정보가 저장되어야 함")
    void valid_token_in_cookie_sets_authentication() throws ServletException, IOException {
        // [Given] Authorization 헤더 대신 쿠키에 토큰이 있는 경우
        String token = "valid-cookie-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("accessToken", token);
        request.setCookies(cookie);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Authentication authentication = mock(Authentication.class);
        when(jwtProvider.validateToken(token)).thenReturn(true);
        when(jwtProvider.getAuthentication(token)).thenReturn(authentication);

        // [When] 필터 실행
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // [Then]
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(filterChain).doFilter(request, response);
    }
}
