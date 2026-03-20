package com.rnhappysmile.java_labs.module.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter의 동작을 테스트합니다.
 * Mockito를 사용하여 JwtProvider의 복잡한 로직은 가짜(Mock)로 대체하고, 
 * 필터 자체의 논리(헤더 추출, 컨텍스트 저장 등)만 검증합니다.
 */
class JwtAuthenticationFilterTest {

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        // JwtProvider를 Mock으로 생성하여 validateToken 등의 결과를 우리가 원하는 대로 설정할 수 있게 합니다.
        jwtProvider = mock(JwtProvider.class);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);
        filterChain = mock(FilterChain.class);
        
        // 테스트 전 SecurityContext(인증 정보 저장소)를 비웁니다.
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후에도 인증 정보를 비워 다른 테스트에 영향을 주지 않도록 합니다.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("성공: 유효한 토큰이 헤더에 있으면 SecurityContext에 인증 정보가 저장되어야 함")
    void valid_token_sets_authentication() throws ServletException, IOException {
        // [Given]
        String token = "valid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token); // Bearer 접두사 포함
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        Authentication authentication = mock(Authentication.class);
        // jwtProvider.validateToken(token)이 호출되면 무조건 true를 반환하도록 설정 (Mocking)
        when(jwtProvider.validateToken(token)).thenReturn(true);
        // getAuthentication 호출 시 준비한 authentication 객체 반환하도록 설정
        when(jwtProvider.getAuthentication(token)).thenReturn(authentication);

        // [When] 필터 실행
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // [Then]
        // SecurityContextHolder에 우리가 만든 인증 정보가 잘 들어가 있는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        // 다음 필터로 요청이 전달되었는지 확인
        verify(filterChain).doFilter(request, response);
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
