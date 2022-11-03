package com.openrec.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class MethodTimeCost {

    @Pointcut("@annotation(com.openrec.aop.TimeCost)")
    private void pointcut() {
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String clazz = joinPoint.getTarget().getClass().getName();
        String method = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        log.info("{}:{} exec cost:{}", clazz, method, System.currentTimeMillis() - start);
        return result;
    }
}
