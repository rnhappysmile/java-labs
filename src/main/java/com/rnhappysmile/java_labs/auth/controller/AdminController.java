package com.rnhappysmile.java_labs.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
    
    @GetMapping("/admin/stat")
    public String adminStat() {
        return "관리자 전용 대시보드입니다. 민감한 통계 데이터를 보여줍니다.";
    }
}
