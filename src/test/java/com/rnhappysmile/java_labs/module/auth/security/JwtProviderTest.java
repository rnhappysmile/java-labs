package com.rnhappysmile.java_labs.module.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtProvider의 핵심 기능을 독립적으로 테스트하는 단위 테스트 클래스입니다.
 * 외부 의존성(Spring, DB 등) 없이 순수하게 토큰의 생성과 검증 로직을 확인합니다.
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;
    
    // 테스트용 비밀키 (최소 256비트 이상 권장)
    private final String secretKey = "c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5";
    private final long accessTokenExpirationTime = 3600000; // 1시간 (3600 * 1000ms)
    private final long refreshTokenExpirationTime = 1209600000; // 14일
    private final String issuer = "test-issuer";

    @BeforeEach
    void setUp() {
        // 매 테스트 시작 전 JwtProvider 객체를 새롭게 생성하여 테스트 간 독립성을 보장합니다.
        jwtProvider = new JwtProvider(secretKey, accessTokenExpirationTime, refreshTokenExpirationTime, issuer);
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 검증: 생성된 토큰이 유효하고 정보를 올바르게 포함하는지 확인")
    void createAndValidateAccessToken() {
        // [Given] 테스트 데이터 준비
        String email = "test@example.com";
        String role = "ROLE_USER";

        // [When] 토큰 생성 실행
        String token = jwtProvider.createAccessToken(email, role);

        // [Then] 결과 검증
        assertThat(token).isNotNull(); // 토큰이 비어있지 않아야 함
        assertThat(jwtProvider.validateToken(token)).isTrue(); // JwtProvider가 스스로 유효하다고 판단해야 함
        assertThat(jwtProvider.getUserEmail(token)).isEqualTo(email); // 토큰 내부의 이메일 정보가 일치해야 함
    }

    @Test
    @DisplayName("리프레시 토큰 생성 및 검증: 액세스 토큰과 마찬가지로 정상 작동하는지 확인")
    void createAndValidateRefreshToken() {
        // [Given] 테스트 데이터 준비
        String email = "test@example.com";
        String role = "ROLE_USER";

        // [When] 리프레시 토큰 생성
        String token = jwtProvider.createRefreshToken(email, role);

        // [Then] 검증
        assertThat(token).isNotNull();
        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserEmail(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증: 임의의 문자열이 주어졌을 때 false를 반환해야 함")
    void validateInvalidToken() {
        // [Given] JWT 형식이 아닌 잘못된 문자열
        String invalidToken = "invalid.token.here";
        
        // [When & Then] 검증 결과가 false여야 함 (보안의 핵심: 위조 토큰 거부)
        assertThat(jwtProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("인증 객체 추출: 토큰의 정보를 바탕으로 Spring Security용 Authentication 객체 생성 확인")
    void getAuthentication() {
        // [Given] 토큰 생성
        String email = "admin@example.com";
        String role = "ROLE_ADMIN";
        String token = jwtProvider.createAccessToken(email, role);

        // [When] 인증 객체 추출
        Authentication authentication = jwtProvider.getAuthentication(token);

        // [Then] Spring Security가 사용할 수 있는 형태인지 검증
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(email); // Principal 이름이 이메일인지 확인
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactly(role); // 권한 정보가 정확히 포함되었는지 확인
    }

    @Test
    @DisplayName("만료된 토큰 검증: 유효 시간이 지난 토큰은 거부되어야 함")
    void validateExpiredToken() throws InterruptedException {
        // [Given] 만료 시간이 매우 짧은(1ms) 임시 JwtProvider 생성
        JwtProvider shortLivedJwtProvider = new JwtProvider(secretKey, 1, 1, issuer);
        String token = shortLivedJwtProvider.createAccessToken("test@example.com", "ROLE_USER");

        // [When] 10ms 대기하여 토큰을 만료시킴
        Thread.sleep(10);

        // [Then] 검증 시 false 반환 확인 (보안의 핵심: 만료된 토큰 사용 금지)
        assertThat(shortLivedJwtProvider.validateToken(token)).isFalse();
    }
}
