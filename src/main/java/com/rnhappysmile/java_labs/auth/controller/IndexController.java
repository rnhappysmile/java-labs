package com.rnhappysmile.java_labs.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rnhappysmile.java_labs.auth.dto.PrincipalDetails;

@RestController
public class IndexController {
    
    @GetMapping("/")
    public String index(@AuthenticationPrincipal PrincipalDetails principalDetails){
        if (principalDetails == null) {
            return "현재 로그인되지 않은 상태입니다. <a href='/oauth2/authorization/google'>구글 로그인</a>";
        }

        return String.format(
            "안녕하세요 %s님 (권한: %s) <br> " +
            "<a href='/admin/stat'>관리자 페이지</a> | " +
            "<a href='/logout'>로그아웃</a>", // 시큐리티가 자동으로 이 경로를 처리합니다.
            principalDetails.getUser().getName(),
            principalDetails.getUser().getRole().getKey()
        );
    }
}
