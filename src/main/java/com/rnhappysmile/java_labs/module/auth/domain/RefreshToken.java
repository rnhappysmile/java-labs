package com.rnhappysmile.java_labs.module.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AllArgsConstructor;
import lombok.Getter;

@RedisHash("refreshToken")
@Getter
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String refreshToken;

    private String email;

    @TimeToLive
    private Long expiration;
}
