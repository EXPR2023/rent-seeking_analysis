package com.rentseeking.analysis.audit;

import com.rentseeking.analysis.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        String resultCode = ErrorCode.SUCCESS;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            resultCode = ErrorCode.SYSTEM_ERROR;
            throw ex;
        } finally {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                operationLogService.record(operationLog, attributes.getRequest(), resultCode);
            }
        }
    }
}
