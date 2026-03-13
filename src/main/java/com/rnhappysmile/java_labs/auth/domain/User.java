package com.rnhappysmile.java_labs.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String provider;
    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public User(String name, String email, String provider, String providerId, Role role) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }
    
}
