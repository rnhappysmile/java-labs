package com.rnhappysmile.java_labs.auth;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import com.rnhappysmile.java_labs.auth.dto.NaverUserInfo;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("naver")) {
            return new NaverUserInfo(attributes); // 네이버 추가!
        } else {
            throw new OAuth2AuthenticationException("허용되지 않은 소셜 로그인입니다.");
        }
    }
}
