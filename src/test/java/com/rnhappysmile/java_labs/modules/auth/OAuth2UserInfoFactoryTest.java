package com.rnhappysmile.java_labs.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import com.rnhappysmile.java_labs.auth.GoogleUserInfo;
import com.rnhappysmile.java_labs.auth.OAuth2UserInfo;
import com.rnhappysmile.java_labs.auth.OAuth2UserInfoFactory;

class OAuth2UserInfoFactoryTest {

    @Test
    @DisplayName("구글 제공자 이름이 들어오면 GoogleUserInfo를 반환해야 한다")
    void getOAuth2UserInfo_Google() {
        // given
        String registrationId = "google";
        Map<String, Object> attributes = Map.of("sub", "12345", "name", "테스터", "email", "test@google.com");

        // when
        OAuth2UserInfo result = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        // then
        assertThat(result).isInstanceOf(GoogleUserInfo.class);
        assertThat(result.getEmail()).isEqualTo("test@google.com");
    }

    @Test
    @DisplayName("지원하지 않는 제공자가 들어오면 예외를 발생시켜야 한다")
    void getOAuth2UserInfo_Exception() {
        // given
        String registrationId = "kakao"; // 아직 추가 안 한 경우

        // when & then
        assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, Map.of()))
            .isInstanceOf(OAuth2AuthenticationException.class);
    }
}