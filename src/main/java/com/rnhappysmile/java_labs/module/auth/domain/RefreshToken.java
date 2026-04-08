package com.rnhappysmile.java_labs.module.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@RedisHash("refreshToken")
@Getter
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    private String refreshToken;

    private String email;

    @TimeToLive
    private Long expiration;

    // Grace Period 관련 필드
    private boolean rotated;
    private String newAccessToken;
    private String newRefreshToken;

    public RefreshToken(String refreshToken, String email, Long expiration) {
        this.refreshToken = refreshToken;
        this.email = email;
        this.expiration = expiration;
        this.rotated = false;
    }

    /**
     * RTR Grace Period를 위해 기존 토큰을 '회전됨' 상태로 마킹하고,
     * 새로 발급된 토큰 정보를 저장하며 TTL을 짧게 조정합니다.
     */
    public void markAsRotated(String newAccessToken, String newRefreshToken, Long gracePeriodInSeconds) {
        this.rotated = true;
        this.newAccessToken = newAccessToken;
        this.newRefreshToken = newRefreshToken;
        this.expiration = gracePeriodInSeconds;
    }
}
