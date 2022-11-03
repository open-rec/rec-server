package com.openrec.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class ApiDecorator {

    private final String API_EXP = "execution(* com.openrec.controller..*Controller.*(..))";

    @Around(API_EXP)
    public Object apiAccessTimeCost(ProceedingJoinPoint joinPoint) throws Throwable {
        String clazz = joinPoint.getTarget().getClass().getName();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();
        LocalVariableTableParameterNameDiscoverer paramDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] params = paramDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        long start = System.currentTimeMillis();
        Object res = joinPoint.proceed(args);
        log.info("API-{}:{} access cost time:{}", clazz, methodName, System.currentTimeMillis() - start);
        return res;
    }
}
