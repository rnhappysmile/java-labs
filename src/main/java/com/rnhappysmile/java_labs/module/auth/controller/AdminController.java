package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import com.rnhappysmile.java_labs.module.auth.dto.EmailOutboxResponse;
import com.rnhappysmile.java_labs.module.auth.service.EmailOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final EmailOutboxService emailOutboxService;
    
    @GetMapping("/admin/stat")
    public String adminStat() {
        return "관리자 전용 대시보드입니다. 민감한 통계 데이터를 보여줍니다.";
    }

    @GetMapping("/api/admin/emails/outbox/statistics")
    public ResponseEntity<Map<String, Long>> getEmailStatistics() {
        return ResponseEntity.ok(emailOutboxService.getStatistics());
    }

    @GetMapping("/api/admin/emails/outbox")
    public ResponseEntity<Page<EmailOutboxResponse>> getEmailOutboxes(
            @RequestParam(required = false) OutboxStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(emailOutboxService.getOutboxes(status, pageable));
    }

    @PostMapping("/api/admin/emails/outbox/{id}/retry")
    public ResponseEntity<String> retryEmail(@PathVariable Long id) {
        emailOutboxService.retry(id);
        return ResponseEntity.ok("이메일 재발송 요청이 성공했습니다. (ID: " + id + ")");
    }
}
