package com.rnhappysmile.java_labs.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MyHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // 실제로는 DB 연결 상태, 파일 권한, 외부 API 응답 등을 체크합니다.
        // 테스트를 위해 0~10 사이의 랜덤 숫자를 뽑아 5보다 작으면 장애(DOWN)로 표시해봅시다.
        int checkCode = ThreadLocalRandom.current().nextInt(10);

        if (checkCode < 5) {
            return Health.down()
                    .withDetail("error_code", checkCode)
                    .withDetail("message", "URL 생성 엔진에 문제가 생겼습니다!")
                    .build();
        }

        return Health.up()
                .withDetail("check_code", checkCode)
                .withDetail("message", "모든 시스템이 정상입니다.")
                .build();
    }
}
