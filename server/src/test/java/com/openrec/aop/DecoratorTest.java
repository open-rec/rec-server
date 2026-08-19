package com.openrec.aop;

import com.openrec.proto.JsonReq;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;
import org.slf4j.MDC;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DecoratorTest {
    public String endpoint(JsonReq<String> request) { return request.getBody(); }

    @Test public void apiDecoratorProceedsAndUsesRequestId() throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        JsonReq<String> request = new JsonReq<>("body"); request.setRequestId("rid");
        Method method = getClass().getMethod("endpoint", JsonReq.class);
        when(point.getTarget()).thenReturn(this); when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method); when(point.getArgs()).thenReturn(new Object[]{request});
        when(point.proceed(any(Object[].class))).thenReturn("result");
        assertEquals("result", new ApiDecorator().apiAccessDecorator(point));
        assertEquals("rid", MDC.get("REQUEST_ID"));
    }

    @Test public void methodTimerReturnsJoinPointValue() throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        org.aspectj.lang.Signature signature = mock(org.aspectj.lang.Signature.class);
        when(point.getTarget()).thenReturn(this); when(point.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("method"); when(point.proceed()).thenReturn("value");
        assertEquals("value", new MethodTimeCost().around(point));
    }
}
