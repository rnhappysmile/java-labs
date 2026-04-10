package com.rnhappysmile.java_labs.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락을 적용하기 위한 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    
    /**
     * 락의 이름 (식별자)
     * SpEL(Spring Expression Language)을 사용하여 파라미터 값을 키로 활용 가능
     */
    String key();

    /**
     * 락의 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 락 획득을 위해 대기하는 시간
     */
    long waitTime() default 5L;

    /**
     * 락을 획득한 후 점유하는 시간 (이 시간이 지나면 자동 해제)
     */
    long leaseTime() default 3L;
}
