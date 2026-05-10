package com.rnhappysmile.java_labs.module.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import com.rnhappysmile.java_labs.module.auth.repository.EmailOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceUnitTest {

    @Mock
    private EmailOutboxRepository emailOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmailOutboxService emailOutboxService;

    @Test
    @DisplayName("수동 재발송 호출 시 상태, 재시도 횟수, 에러 메시지가 초기화된다")
    void retry_ShouldResetOutboxState() {
        // given
        Long outboxId = 1L;
        EmailOutbox outbox = EmailOutbox.builder()
                .recipient("test@example.com")
                .templateName("mail/welcome")
                .payload("{}")
                .build();
        
        // 실패 상태로 강제 변경
        outbox.permanentlyFail("SMTP Server Down");
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getLastError()).isNotNull();

        given(emailOutboxRepository.findById(outboxId)).willReturn(Optional.of(outbox));

        // when
        emailOutboxService.retry(outboxId);

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isZero();
        assertThat(outbox.getLastError()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 재발송 호출 시 예외가 발생한다")
    void retry_WithInvalidId_ShouldThrowException() {
        // given
        Long invalidId = 999L;
        given(emailOutboxRepository.findById(invalidId)).willReturn(Optional.empty());

        // when & then
        try {
            emailOutboxService.retry(invalidId);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Outbox not found");
        }
    }
}
