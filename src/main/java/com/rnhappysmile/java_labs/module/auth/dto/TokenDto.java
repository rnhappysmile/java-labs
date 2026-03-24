package com.rnhappysmile.java_labs.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenDto {
    public String newAccessToken;
    public String newRefreshToken;
}
