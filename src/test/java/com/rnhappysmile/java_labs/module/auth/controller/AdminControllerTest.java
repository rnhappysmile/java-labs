package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import com.rnhappysmile.java_labs.module.auth.repository.EmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // 한글 깨짐 방지 및 시큐리티 필터 적용을 위한 MockMvc 재설정
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();

        emailOutboxRepository.deleteAll();
        
        saveOutbox("success@test.com", OutboxStatus.PROCESSED);
        saveOutbox("pending@test.com", OutboxStatus.PENDING);
        saveOutbox("failed@test.com", OutboxStatus.FAILED);
    }

    private void saveOutbox(String recipient, OutboxStatus status) {
        EmailOutbox outbox = EmailOutbox.builder()
                .recipient(recipient)
                .templateName("mail/test")
                .payload("{}")
                .build();
        
        emailOutboxRepository.save(outbox);
        
        if (status == OutboxStatus.PROCESSED) {
            outbox.complete();
        } else if (status == OutboxStatus.FAILED) {
            outbox.permanentlyFail("Test Error");
        } else if (status == OutboxStatus.PROCESSING) {
            outbox.startProcessing();
        }
        
        emailOutboxRepository.saveAndFlush(outbox);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이메일 통계 조회 API는 모든 상태의 건수를 반환한다")
    void getEmailStatistics_Success() throws Exception {
        mockMvc.perform(get("/api/admin/emails/outbox/statistics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PROCESSED").value(1))
                .andExpect(jsonPath("$.PENDING").value(1))
                .andExpect(jsonPath("$.FAILED").value(1))
                .andExpect(jsonPath("$.TOTAL").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이메일 목록 조회 API는 페이징된 데이터를 반환한다")
    void getEmailOutboxes_Success() throws Exception {
        mockMvc.perform(get("/api/admin/emails/outbox")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].recipient").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상태 필터링을 포함한 이메일 목록 조회 API가 정상 동작한다")
    void getEmailOutboxes_WithStatusFilter() throws Exception {
        mockMvc.perform(get("/api/admin/emails/outbox")
                        .param("status", "FAILED"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].recipient").value("failed@test.com"));
    }
}
