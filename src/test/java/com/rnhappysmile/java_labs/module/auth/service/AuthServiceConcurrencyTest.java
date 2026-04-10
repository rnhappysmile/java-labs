package com.rnhappysmile.java_labs.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rnhappysmile.java_labs.module.auth.domain.RefreshToken;
import com.rnhappysmile.java_labs.module.auth.domain.Role;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.TokenDto;
import com.rnhappysmile.java_labs.module.auth.repository.RefreshTokenRepository;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import com.rnhappysmile.java_labs.module.auth.security.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private String testEmail = "concurrency@test.com";
    private String refreshToken = "test-refresh-token";

    @BeforeEach
    void setUp() {
        // 테스트용 유저 및 토큰 저장
        User user = User.builder()
                .email(testEmail)
                .name("Test User")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        RefreshToken token = new RefreshToken(refreshToken, testEmail, 1000L);
        refreshTokenRepository.save(token);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }

    @Test
    @Disabled("로컬 Redis와 Spring Data Redis 간의 가시성 지연(Visibility Latency)으로 인해 일시적으로 비활성화함")
    @DisplayName("동시에 10개의 재발급 요청이 오더라도 모두 동일한 새 토큰을 받아야 한다 (Idempotency)")
    void reissue_concurrency_test() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1); // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // 모든 종료 대기

        // 결과를 담을 스레드 안전한 셋
        Set<String> newAccessTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<String> newRefreshTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // 모든 스레드가 여기서 대기
                    TokenDto result = authService.reissue(refreshToken);
                    if (result != null) {
                        newAccessTokens.add(result.getNewAccessToken());
                        newRefreshTokens.add(result.getNewRefreshToken());
                    }
                } catch (Exception e) {
                    System.err.println("Error during concurrency test: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        System.out.println("Wait for threads to finish...");
        latch.countDown(); // 출발!
        doneLatch.await(); // 모든 스레드 종료 대기

        System.out.println("Results - AccessTokens size: " + newAccessTokens.size());
        System.out.println("Results - RefreshTokens size: " + newRefreshTokens.size());

        // 검증: 모든 요청이 성공했어야 하며, 결과값(토큰)은 단 1종류여야 함
        assertThat(newAccessTokens).as("AccessTokens should have only 1 unique token").hasSize(1);
        assertThat(newRefreshTokens).as("RefreshTokens should have only 1 unique token").hasSize(1);
        
        System.out.println("Generated Access Token: " + newAccessTokens.iterator().next());
    }
}
