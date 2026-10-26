package com.entrevio.entrevio_backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateTimeUtil() {}

    public static String toIso(LocalDateTime dt) {
        return dt == null ? null : ISO.format(dt);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}