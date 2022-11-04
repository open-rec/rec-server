package com.openrec.aop;

import com.openrec.proto.JsonReq;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class ApiDecorator {

    private static final String REQUEST_ID = "REQUEST_ID";

    private final String API_EXP = "execution(* com.openrec.controller..*Controller.*(..))";

    private void setRequestIdByParams(Object[] args) {
        Object request = args[0];
        if (request instanceof JsonReq) {
            MDC.put(REQUEST_ID, ((JsonReq) request).getRequestId());
        } else {
            MDC.put(REQUEST_ID, "inner_" + UUID.randomUUID());
        }
    }

    @Around(API_EXP)
    public Object apiAccessDecorator(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String clazz = joinPoint.getTarget().getClass().getName();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();
        LocalVariableTableParameterNameDiscoverer paramDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] params = paramDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        setRequestIdByParams(args);

        Object res = joinPoint.proceed(args);
        log.info("API-{}:{} access cost time:{}", clazz, methodName, System.currentTimeMillis() - start);
        return res;
    }
}
