package com.rnhappysmile.java_labs.auth.dto;

import java.util.Map;

import com.rnhappysmile.java_labs.auth.OAuth2UserInfo;

public class NaverUserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    public NaverUserInfo(Map<String, Object> attributes) {
        // 네이버는 "response"라는 키 안에 실제 정보가 들어있습니다.
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() { return (String) attributes.get("id"); }

    @Override
    public String getProvider() { return "naver"; }

    @Override
    public String getEmail() { return (String) attributes.get("email"); }

    @Override
    public String getName() { return (String) attributes.get("name"); }
}