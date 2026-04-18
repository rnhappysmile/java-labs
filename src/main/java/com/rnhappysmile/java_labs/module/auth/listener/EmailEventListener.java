package com.rnhappysmile.java_labs.module.auth.listener;

import com.rnhappysmile.java_labs.module.auth.event.UserRegisteredEvent;
import com.rnhappysmile.java_labs.module.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for: {}", event.getEmail());
        emailService.sendWelcomeEmail(event.getEmail(), event.getName());
    }
}
