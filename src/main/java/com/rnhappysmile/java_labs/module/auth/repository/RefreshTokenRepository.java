package com.rnhappysmile.java_labs.module.auth.repository;

import org.springframework.data.repository.CrudRepository;

import com.rnhappysmile.java_labs.module.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
