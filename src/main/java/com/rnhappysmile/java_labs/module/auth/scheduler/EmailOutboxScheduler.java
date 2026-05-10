package com.rnhappysmile.java_labs.module.auth.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import com.rnhappysmile.java_labs.module.auth.repository.EmailOutboxRepository;
import com.rnhappysmile.java_labs.module.auth.service.EmailOutboxService;
import com.rnhappysmile.java_labs.module.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOutboxScheduler {

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${outbox.email.max-retry:5}")
    private int maxRetry;

    @Scheduled(fixedDelay = 10000)
    public void processPendingEmails() {
        List<EmailOutbox> pendingEmails = emailOutboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 10));

        for (EmailOutbox outbox : pendingEmails) {
            processEmail(outbox.getId());
        }
    }

    public void processEmail(Long outboxId) {
        // 1. Atomic status update to PROCESSING (distributed lock effect)
        int updated = emailOutboxService.updateStatus(outboxId, OutboxStatus.PENDING, OutboxStatus.PROCESSING);
        if (updated == 0) {
            return;
        }

        try {
            EmailOutbox outbox = emailOutboxRepository.findById(outboxId)
                    .orElseThrow(() -> new RuntimeException("Outbox not found: " + outboxId));

            Map<String, Object> variables = objectMapper.readValue(
                    outbox.getPayload(), new TypeReference<>() {});

            String subject = "Java Labs 알림";
            if ("mail/welcome".equals(outbox.getTemplateName())) {
                subject = "Java Labs 회원가입을 축하드립니다!";
            }

            emailService.sendEmail(outbox.getRecipient(), subject, outbox.getTemplateName(), variables);

            emailOutboxService.complete(outboxId);
            log.info("Successfully processed email outbox id: {}", outboxId);
        } catch (Exception e) {
            log.error("Failed to process email outbox id: {}", outboxId, e);
            emailOutboxService.fail(outboxId, e.getMessage(), maxRetry);
        }
    }
}
