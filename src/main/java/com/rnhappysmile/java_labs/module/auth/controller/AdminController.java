package com.rnhappysmile.java_labs.module.auth.controller;

import com.rnhappysmile.java_labs.module.auth.service.EmailOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final EmailOutboxService emailOutboxService;
    
    @GetMapping("/admin/stat")
    public String adminStat() {
        return "관리자 전용 대시보드입니다. 민감한 통계 데이터를 보여줍니다.";
    }

    @PostMapping("/api/admin/emails/outbox/{id}/retry")
    public ResponseEntity<String> retryEmail(@PathVariable Long id) {
        emailOutboxService.retry(id);
        return ResponseEntity.ok("이메일 재발송 요청이 성공했습니다. (ID: " + id + ")");
    }
}
