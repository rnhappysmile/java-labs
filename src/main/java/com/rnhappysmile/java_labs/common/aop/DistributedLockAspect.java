package com.rnhappysmile.java_labs.common.aop;

import com.rnhappysmile.java_labs.common.error.ErrorCode;
import com.rnhappysmile.java_labs.common.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @DistributedLock 어노테이션이 붙은 메서드에 분산 락을 적용하는 Aspect
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "LOCK:";
    private final RedissonClient redissonClient;

    /**
     * @Around 어노테이션을 통해 메서드 실행 전후에 락 획득/해제를 처리
     */
    @Around("@annotation(com.rnhappysmile.java_labs.common.aop.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        // 1. 락 키 파싱 (SpEL 활용)
        String key = LOCK_PREFIX + getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);

        try {
            // 2. 락 획득 시도
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!available) {
                log.warn("Failed to acquire lock for key: {}", key);
                throw new BusinessException("잠시 후 다시 시도해주세요. (Lock Acquisition Failed)", ErrorCode.INVALID_TOKEN); 
            }

            log.info("Successfully acquired lock for key: {}", key);
            // 3. 실제 비즈니스 로직 실행 (Advice)
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted for key: {}", key, e);
            throw e;
        } finally {
            try {
                // 4. 락 해제
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                    log.info("Successfully released lock for key: {}", key);
                }
            } catch (IllegalMonitorStateException e) {
                log.warn("Lock already released or expired for key: {}", key);
            }
        }
    }

    /**
     * SpEL을 사용하여 메서드 파라미터에서 동적으로 값을 추출
     */
    private Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return parser.parseExpression(key).getValue(context, Object.class);
    }
}
