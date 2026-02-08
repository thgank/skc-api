package com.tslnkk.skcapi.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
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

    /**
     * Logs method entry and exit with execution time for service methods.
     */
    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info(">>> {}.{}() called with args: {}", className, methodName, formatArgs(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("<<< {}.{}() returned in {}ms", className, methodName, elapsed);
            return result;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("<!> {}.{}() threw {} after {}ms: {}",
                    className, methodName, ex.getClass().getSimpleName(), elapsed, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Logs incoming HTTP requests at controller level.
     */
    @Before("controllerMethods()")
    public void logControllerEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.debug("HTTP -> {}() args: {}", methodName, formatArgs(args));
    }

    /**
     * Logs exception handler invocations.
     */
    @Before("exceptionHandlerMethods()")
    public void logExceptionHandling(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Exception ex) {
            log.error("Exception handled: [{}] {}", ex.getClass().getSimpleName(), ex.getMessage());
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
