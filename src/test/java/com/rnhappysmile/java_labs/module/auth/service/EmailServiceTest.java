package com.rnhappysmile.java_labs.module.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Test
    @DisplayName("환영 메일 발송 로직이 정상적으로 호출되는지 확인")
    void sendWelcomeEmail_Success() {
        // given
        String to = "test@example.com";
        String name = "테스터";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        given(templateEngine.process(eq("mail/welcome"), any(Context.class)))
                .willReturn("<html>Welcome</html>");

        // when
        emailService.sendWelcomeEmail(to, name);

        // then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine, times(1)).process(eq("mail/welcome"), any(Context.class));
    }
}
