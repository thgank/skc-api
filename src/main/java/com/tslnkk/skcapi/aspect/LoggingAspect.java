package com.tslnkk.skcapi.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cross-cutting logging concern via AOP.
 * Automatically logs method entry, exit, and exceptions
 * for service and controller layers.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Value("${app.logging.slow-call-threshold-ms:500}")
    private long slowCallThresholdMs;

    /**
     * Pointcut for all public methods in service layer.
     */
    @Pointcut("execution(public * com.tslnkk.skcapi.service..*(..))")
    public void serviceMethods() {
    }

    /**
     * Pointcut for all public methods in controller layer.
     */
    @Pointcut("execution(public * com.tslnkk.skcapi.controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * Pointcut for exception handler methods.
     */
    @Pointcut("execution(public * com.tslnkk.skcapi.exception.GlobalExceptionHandler.*(..))")
    public void exceptionHandlerMethods() {
    }

    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startNs = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            if (elapsedMs >= slowCallThresholdMs) {
                log.warn("Slow service call: {}.{} took {}ms", className, methodName, elapsedMs);
            } else if (log.isDebugEnabled()) {
                log.debug("Service call: {}.{} took {}ms", className, methodName, elapsedMs);
            }
            return result;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.error("Service call failed: {}.{} after {}ms ({})",
                    className, methodName, elapsedMs, ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    @Around("controllerMethods()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startNs = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            if (log.isDebugEnabled()) {
                log.debug("HTTP handled: {}.{} in {}ms", className, methodName, elapsedMs);
            }
            return result;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.error("HTTP handling failed: {}.{} after {}ms ({})",
                    className, methodName, elapsedMs, ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    @Before("exceptionHandlerMethods()")
    public void logExceptionHandling(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Exception ex) {
            log.warn("Exception mapped by handler: [{}] {}", ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(arg -> arg == null ? "null" : arg.toString())
                .reduce((a, b) -> a + ", " + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");
    }
}
