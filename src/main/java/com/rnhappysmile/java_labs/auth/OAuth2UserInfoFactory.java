package com.rnhappysmile.java_labs.auth;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(attributes);
        }

        throw new IllegalArgumentException("지원하지 않는 로그인 서비스입니다.");
    }
}
