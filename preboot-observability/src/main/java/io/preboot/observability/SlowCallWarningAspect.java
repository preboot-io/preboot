package io.preboot.observability;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class SlowCallWarningAspect {

    @Around("@annotation(io.preboot.observability.WarnOnSlowCall)")
    public Object warnOnSlowCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        WarnOnSlowCall annotation = method.getAnnotation(WarnOnSlowCall.class);
        long threshold = annotation.threshold();

        long start = System.currentTimeMillis();
        Object proceed = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - start;

        if (executionTime > threshold) {
            String params = Arrays.stream(
                            Optional.ofNullable(joinPoint.getArgs()).orElse(new Object[0]))
                    .map(arg -> Optional.ofNullable(arg).map(Object::toString).orElse("null"))
                    .collect(Collectors.joining(", "));
            log.warn(
                    "{}({}) executed in {}ms ----- attention",
                    Optional.ofNullable(joinPoint.getSignature())
                            .map(Signature::toShortString)
                            .orElse("Unknown"),
                    params,
                    executionTime);
        }
        return proceed;
    }
}
