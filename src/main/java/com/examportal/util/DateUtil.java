package com.examportal.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Date/time formatting utilities used across the application. */
public final class DateUtil {

    private static final DateTimeFormatter DISPLAY_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter SHORT_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateUtil() {}

    public static String format(LocalDateTime dt) {
        return dt != null ? dt.format(DISPLAY_FMT) : "-";
    }

    public static String formatShort(LocalDateTime dt) {
        return dt != null ? dt.format(SHORT_FMT) : "-";
    }

    public static String formatTimestamp(LocalDateTime dt) {
        return dt != null ? dt.format(TIMESTAMP_FMT) : "";
    }
}
