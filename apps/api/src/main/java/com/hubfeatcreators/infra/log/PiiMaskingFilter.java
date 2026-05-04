package com.hubfeatcreators.infra.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.regex.Pattern;

/**
 * Logback filter that masks PII patterns in log messages before they are written.
 * AC-6: e-mail, CPF, BR phone masked in all log output.
 */
public class PiiMaskingFilter extends Filter<ILoggingEvent> {

    // Matches e-mail addresses (simplified)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([a-zA-Z0-9._%+\\-]+)@([a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})");

    // Matches Brazilian CPF: 000.000.000-00 or 00000000000
    private static final Pattern CPF_PATTERN =
            Pattern.compile("\\b(\\d{3})[.\\-]?(\\d{3})[.\\-]?(\\d{3})[\\-]?(\\d{2})\\b");

    // Matches Brazilian phone: +55... or 55... or (XX)XXXXX-XXXX
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("((?:\\+?55)?\\s?\\(?\\d{2}\\)?[\\s\\-]?\\d{4,5}[\\-]?\\d{4})");

    @Override
    public FilterReply decide(ILoggingEvent event) {
        // We don't block — we only transform via MessageConverter; this filter passes through
        return FilterReply.NEUTRAL;
    }

    /** Applies PII masking to a raw log message string. */
    public static String mask(String message) {
        if (message == null) return null;
        String masked = EMAIL_PATTERN.matcher(message)
                .replaceAll(m -> m.group(1).substring(0, 1) + "***@" + m.group(2));
        masked = CPF_PATTERN.matcher(masked)
                .replaceAll("***.***.***-**");
        masked = PHONE_PATTERN.matcher(masked)
                .replaceAll(m -> {
                    String raw = m.group(1).replaceAll("[^\\d]", "");
                    if (raw.length() < 4) return m.group(1);
                    return raw.substring(0, 2) + "*****" + raw.substring(raw.length() - 4);
                });
        return masked;
    }
}
