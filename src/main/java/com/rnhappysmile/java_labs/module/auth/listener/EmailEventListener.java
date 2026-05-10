package com.rnhappysmile.java_labs.module.auth.listener;

import com.rnhappysmile.java_labs.module.auth.event.UserRegisteredEvent;
import com.rnhappysmile.java_labs.module.auth.service.EmailOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailOutboxService emailOutboxService;

    @EventListener
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for: {}", event.getEmail());
        emailOutboxService.saveOutbox(
                event.getEmail(),
                "mail/welcome",
                Map.of("name", event.getName())
        );
    }
}
