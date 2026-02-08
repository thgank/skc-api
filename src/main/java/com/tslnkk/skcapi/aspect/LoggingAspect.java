package com.tslnkk.skcapi.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cross-cutting logging concern via AOP.
 * Automatically logs method entry, exit, and exceptions
 * for service and controller layers.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final Pattern JSON_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(\"(?:password|passwd|pwd|token|secret|authorization)\"\\s*:\\s*\")([^\"]*)(\")"
    );
    private static final Pattern KV_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)((?:password|passwd|pwd|token|secret|authorization)\\s*=\\s*)([^,\\]\\)\\s]+)"
    );
    private static final Pattern HEADER_AUTH_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)(basic|bearer)\\s+[A-Za-z0-9._\\-+/=]+"
    );

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
        Object[] args = joinPoint.getArgs();
        long startNs = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            String argsSummary = formatArgs(args);
            String resultSummary = formatResult(result);
            if (elapsedMs >= slowCallThresholdMs) {
                log.warn("Slow service call: {}.{} took {}ms args={} result={}",
                        className, methodName, elapsedMs, argsSummary, resultSummary);
            } else {
                log.info("Service call: {}.{} completed in {}ms args={} result={}",
                        className, methodName, elapsedMs, argsSummary, resultSummary);
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
        HttpServletRequest request = getCurrentRequest();
        HttpServletResponse response = getCurrentResponse();
        String method = request != null ? request.getMethod() : "n/a";
        String uri = request != null ? request.getRequestURI() : "n/a";
        String query = request != null && request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String user = request != null && request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
        long startNs = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            int status = response != null ? response.getStatus() : -1;
            log.info("HTTP {} {}{} -> {}.{} status={} user={} in {}ms",
                    method, uri, query, className, methodName, status, user, elapsedMs);
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
                .map(this::summarizeObject)
                .reduce((a, b) -> a + ", " + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");
    }

    private String formatResult(Object result) {
        return summarizeObject(result);
    }

    private String summarizeObject(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Collection<?> c) {
            return value.getClass().getSimpleName() + "(size=" + c.size() + ")";
        }
        if (value instanceof Map<?, ?> m) {
            return value.getClass().getSimpleName() + "(size=" + m.size() + ")";
        }
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName() + "[](size=" + Array.getLength(value) + ")";
        }
        String raw = value.toString().replaceAll("[\\r\\n\\t]+", " ");
        raw = sanitizeSensitiveData(raw);
        int maxLen = 180;
        if (raw.length() > maxLen) {
            return raw.substring(0, maxLen) + "...";
        }
        return raw;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getResponse() : null;
    }

    private String sanitizeSensitiveData(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = JSON_SENSITIVE_PATTERN.matcher(value).replaceAll("$1***$3");
        sanitized = KV_SENSITIVE_PATTERN.matcher(sanitized).replaceAll("$1***");
        sanitized = HEADER_AUTH_PATTERN.matcher(sanitized).replaceAll("$1$2 ***");
        return sanitized;
    }
}
