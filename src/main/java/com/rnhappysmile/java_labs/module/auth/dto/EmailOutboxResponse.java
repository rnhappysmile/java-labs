package com.rnhappysmile.java_labs.module.auth.dto;

import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmailOutboxResponse {
    private Long id;
    private String recipient;
    private String templateName;
    private OutboxStatus status;
    private int retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EmailOutboxResponse from(EmailOutbox outbox) {
        return EmailOutboxResponse.builder()
                .id(outbox.getId())
                .recipient(outbox.getRecipient())
                .templateName(outbox.getTemplateName())
                .status(outbox.getStatus())
                .retryCount(outbox.getRetryCount())
                .lastError(outbox.getLastError())
                .createdAt(outbox.getCreatedAt())
                .updatedAt(outbox.getUpdatedAt())
                .build();
    }
}
