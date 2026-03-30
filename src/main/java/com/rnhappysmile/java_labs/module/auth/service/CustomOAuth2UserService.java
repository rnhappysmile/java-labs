package com.rnhappysmile.java_labs.module.auth.service;

import com.rnhappysmile.java_labs.module.auth.OAuth2UserInfo;
import com.rnhappysmile.java_labs.module.auth.OAuth2UserInfoFactory;
import com.rnhappysmile.java_labs.module.auth.domain.Role;
import com.rnhappysmile.java_labs.module.auth.domain.User;
import com.rnhappysmile.java_labs.module.auth.dto.PrincipalDetails;
import com.rnhappysmile.java_labs.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        User user = saveOrUpdate(oAuth2UserInfo);

        if (oAuth2User instanceof OidcUser oidcUser) {
            return new PrincipalDetails(user, oAuth2User.getAttributes(), oidcUser.getIdToken());
        }
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
