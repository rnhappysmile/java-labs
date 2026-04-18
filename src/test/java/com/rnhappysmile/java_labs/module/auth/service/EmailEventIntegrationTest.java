package com.rnhappysmile.java_labs.module.auth.service;

import com.rnhappysmile.java_labs.module.auth.event.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class EmailEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("회원가입 이벤트 발행 시 트랜잭션 커밋 후 비동기로 메일 서비스가 호출되는지 확인")
    void userRegisteredEvent_TriggerEmailService_AfterCommit() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent("test@gmail.com", "테스터");

        // when: 트랜잭션 안에서 이벤트 발행
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(event);
            return null;
        });

        // then: 비동기 실행이므로 await 사용 (최대 2초 대기)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(emailService, atLeastOnce()).sendWelcomeEmail(anyString(), anyString());
        });
    }
}
