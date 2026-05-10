package com.rnhappysmile.java_labs.module.auth.service;

import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import com.rnhappysmile.java_labs.module.auth.event.UserRegisteredEvent;
import com.rnhappysmile.java_labs.module.auth.repository.EmailOutboxRepository;
import com.rnhappysmile.java_labs.module.auth.scheduler.EmailOutboxScheduler;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class EmailOutboxIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private EmailOutboxScheduler emailOutboxScheduler;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        emailOutboxRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 이벤트 발행 시 EmailOutbox가 저장되고 스케줄러에 의해 메일이 발송되는지 확인")
    void userRegisteredEvent_SaveOutboxAndProcess() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent("test@gmail.com", "테스터");

        // when: 트랜잭션 안에서 이벤트 발행
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(event);
            return null;
        });

        // then 1: Outbox에 데이터가 저장되었는지 확인
        List<EmailOutbox> outboxes = emailOutboxRepository.findAll();
        assertThat(outboxes).hasSize(1);
        assertThat(outboxes.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxes.get(0).getRecipient()).isEqualTo("test@gmail.com");

        // then 2: 스케줄러 직접 실행 (또는 @Scheduled가 돌 때까지 대기)
        emailOutboxScheduler.processPendingEmails();

        // then 3: 메일 서비스가 호출되었고 상태가 PROCESSED로 변경되었는지 확인
        await().untilAsserted(() -> {
            verify(emailService, atLeastOnce()).sendEmail(
                    eq("test@gmail.com"), 
                    anyString(), 
                    eq("mail/welcome"), 
                    anyMap()
            );
            
            EmailOutbox processedOutbox = emailOutboxRepository.findById(outboxes.get(0).getId()).orElseThrow();
            assertThat(processedOutbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        });
    }
}
