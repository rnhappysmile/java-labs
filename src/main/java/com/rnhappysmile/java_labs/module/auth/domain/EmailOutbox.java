package com.rnhappysmile.java_labs.module.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "email_outbox", indexes = {
    @Index(name = "idx_email_outbox_status_created_at", columnList = "status, created_at")
})
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String templateName;

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON representation of template variables

    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Version
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public EmailOutbox(String recipient, String templateName, String payload) {
        this.status = OutboxStatus.PENDING;
        this.recipient = recipient;
        this.templateName = templateName;
        this.payload = payload;
        this.retryCount = 0;
    }

    public void startProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void complete() {
        this.status = OutboxStatus.PROCESSED;
    }

    public void fail(String errorMessage) {
        this.status = OutboxStatus.PENDING; // Set back to PENDING for retry
        this.retryCount++;
        this.lastError = errorMessage;
    }

    public void permanentlyFail(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.lastError = errorMessage;
    }
}
