package com.rnhappysmile.java_labs.module.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import com.rnhappysmile.java_labs.module.auth.dto.EmailOutboxResponse;
import com.rnhappysmile.java_labs.module.auth.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        for (OutboxStatus status : OutboxStatus.values()) {
            stats.put(status.name(), emailOutboxRepository.countByStatus(status));
        }
        stats.put("TOTAL", emailOutboxRepository.count());
        return stats;
    }

    @Transactional(readOnly = true)
    public Page<EmailOutboxResponse> getOutboxes(OutboxStatus status, Pageable pageable) {
        if (status != null) {
            return emailOutboxRepository.findByStatus(status, pageable)
                    .map(EmailOutboxResponse::from);
        }
        return emailOutboxRepository.findAll(pageable)
                .map(EmailOutboxResponse::from);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOutbox(String recipient, String templateName, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            EmailOutbox outbox = EmailOutbox.builder()
                    .recipient(recipient)
                    .templateName(templateName)
                    .payload(payloadJson)
                    .build();
            
            emailOutboxRepository.save(outbox);
            log.info("Saved email outbox for: {}", recipient);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize email payload for: {}", recipient, e);
            throw new RuntimeException("Email payload serialization failed", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateStatus(Long id, OutboxStatus fromStatus, OutboxStatus toStatus) {
        return emailOutboxRepository.updateStatus(id, fromStatus, toStatus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long id) {
        emailOutboxRepository.findById(id).ifPresent(EmailOutbox::complete);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long id, String errorMessage, int maxRetry) {
        emailOutboxRepository.findById(id).ifPresent(outbox -> {
            if (outbox.getRetryCount() >= maxRetry) {
                outbox.permanentlyFail(errorMessage);
            } else {
                outbox.fail(errorMessage);
            }
        });
    }

    @Transactional
    public void retry(Long id) {
        EmailOutbox outbox = emailOutboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox not found: " + id));
        
        outbox.manualRetry();
        log.info("Manually triggered retry for email outbox id: {}", id);
    }
}
