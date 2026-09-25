package com.smart.therapy.flow.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MethodLoggingAspect {

    private static final int MAX_LOG_VALUE_LENGTH = 2000;

    private final SensitiveDataMasker sensitiveDataMasker;

    @Pointcut("within(com.smart.therapy.flow..controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.smart.therapy.flow..service..*)")
    public void serviceLayer() {
    }

    @Pointcut("controllerLayer() || (serviceLayer()" +
            " && !within(com.smart.therapy.flow.auth.service.AuthenticationService)" +
            " && !within(com.smart.therapy.flow.auth.service.MfaService))")
    public void applicationLayer() {
    }

    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        boolean isController = className.contains(".controller.");
        String layer = isController ? "CONTROLLER" : "SERVICE";
        long startNanos = System.nanoTime();

        if (isController) {
            log.info("{}_IN {}.{}", layer, className, methodName);
            if (log.isDebugEnabled()) {
                log.debug("{}_ARGS {}.{} args={}", layer, className, methodName,
                        sensitiveDataMasker.summarizeArguments(joinPoint.getArgs(), MAX_LOG_VALUE_LENGTH));
            }
        } else if (log.isDebugEnabled()) {
            log.debug("{}_IN {}.{} args={}", layer, className, methodName,
                    sensitiveDataMasker.summarizeArguments(joinPoint.getArgs(), MAX_LOG_VALUE_LENGTH));
        }

        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            if (isController) {
                log.info("{}_OUT {}.{} durationMs={}", layer, className, methodName, durationMs);
                if (log.isDebugEnabled()) {
                    log.debug("{}_RESULT {}.{} result={}", layer, className, methodName,
                            sensitiveDataMasker.summarizeResult(result, MAX_LOG_VALUE_LENGTH));
                }
            } else if (log.isDebugEnabled()) {
                log.debug("{}_OUT {}.{} durationMs={} result={}", layer, className, methodName, durationMs,
                        sensitiveDataMasker.summarizeResult(result, MAX_LOG_VALUE_LENGTH));
            }

            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("{}_ERROR {}.{} durationMs={} errorType={}", layer, className, methodName, durationMs,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
