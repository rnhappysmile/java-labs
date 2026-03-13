package com.rnhappysmile.java_labs.auth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.rnhappysmile.java_labs.auth.OAuth2UserInfo;
import com.rnhappysmile.java_labs.auth.OAuth2UserInfoFactory;
import com.rnhappysmile.java_labs.auth.domain.Role;
import com.rnhappysmile.java_labs.auth.domain.User;
import com.rnhappysmile.java_labs.auth.dto.PrincipalDetails;
import com.rnhappysmile.java_labs.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService{

    private final UserRepository userRepository;

    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        User user = saveOrUpdate(oAuth2UserInfo);

        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo) {
        return userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .map(entity -> {  // 이미 있으면 업데이트
                    // 필요 시 이름이나 이메일 업데이트 로직 추가
                    return entity;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(userInfo.getName())
                        .email(userInfo.getEmail())
                        .provider(userInfo.getProvider())
                        .providerId(userInfo.getProviderId())
                        .role(Role.USER)
                        .build()));
    }
    
}
