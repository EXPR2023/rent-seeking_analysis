package com.rentseeking.analysis.common.util;

import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class TraceIdUtils {

    public static final String TRACE_ID_KEY = "traceId";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private TraceIdUtils() {
    }

    public static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId == null || traceId.isBlank() ? newTraceId() : traceId;
    }

    public static String newTraceId() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return LocalDateTime.now().format(FORMATTER) + suffix;
    }
}
