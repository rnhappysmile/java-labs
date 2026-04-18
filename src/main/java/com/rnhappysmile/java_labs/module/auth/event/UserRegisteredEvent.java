package com.rnhappysmile.java_labs.module.auth.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserRegisteredEvent {
    private final String email;
    private final String name;
}
