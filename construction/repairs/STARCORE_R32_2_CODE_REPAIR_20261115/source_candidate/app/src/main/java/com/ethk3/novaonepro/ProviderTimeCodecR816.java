package com.ethk3.novaonepro;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * R816 provider-time codec.
 *
 * <p>Duoqu's zone-less {@code openTime} is a UTC wire value. Parsing it in the device default
 * timezone moves the draw by the user's UTC offset (eight hours on the failing device), which in
 * turn makes a current HTTP-200 payload look stale. Every provider parser delegates here so the
 * same bytes always produce the same epoch on every phone. Numeric epochs and explicit ISO/RFC
 * offsets retain their original meaning.
 */
public final class ProviderTimeCodecR816 {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long EPOCH_SECONDS_CEILING = 100000000000L;
    private static final long METADATA_REPAIR_MARGIN_MS = 60000L;

    private ProviderTimeCodecR816() {}

    public static long parse(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return normalizeEpoch(((Number) value).longValue());
        return parseText(String.valueOf(value));
    }

    public static long parseText(String value) {
        String text = safe(value);
        if (text.length() == 0) return 0L;
        if (digitsOnly(text)) {
            try {
                return normalizeEpoch(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        String zoned = normalizeExplicitZone(text);
        if (zoned.length() > 0) {
            long parsed = parseExact(zoned, hasFraction(zoned) ? "yyyy-MM-dd'T'HH:mm:ss.SSSZ" : "yyyy-MM-dd'T'HH:mm:ssZ", UTC);
            if (parsed > 0L) return parsed;
            parsed = parseExact(zoned, hasFraction(zoned) ? "yyyy-MM-dd HH:mm:ss.SSSZ" : "yyyy-MM-dd HH:mm:ssZ", UTC);
            if (parsed > 0L) return parsed;
        }

        String normalized = normalizeFraction(text);
        String[] patterns =
                new String[] {
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm:ss.SSS",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd"
                };
        for (int i = 0; i < patterns.length; i++) {
            long parsed = parseExact(normalized, patterns[i], UTC);
            if (parsed > 0L) return parsed;
        }
        return 0L;
    }

    /**
     * Metadata-only repair selector. Business identity and first-seen provenance are deliberately
     * outside this method. A newly parsed time replaces an old value only when it is materially
     * closer to the immutable receipt time, so a later malformed provider response cannot move a
     * previously good draw.
     */
    public static long preferCorrectedOpenTime(
            long existingOpenTime, long candidateOpenTime, long firstSeenAt) {
        long existing = normalizeEpoch(existingOpenTime);
        long candidate = normalizeEpoch(candidateOpenTime);
        long anchor = normalizeEpoch(firstSeenAt);
        if (candidate <= 0L) return existing;
        if (existing <= 0L || existing == candidate) return candidate;
        if (anchor <= 0L) return existing;
        long oldDistance = distance(anchor, existing);
        long newDistance = distance(anchor, candidate);
        return newDistance + METADATA_REPAIR_MARGIN_MS < oldDistance ? candidate : existing;
    }

    public static long normalizeEpoch(long value) {
        if (value <= 0L) return 0L;
        return value < EPOCH_SECONDS_CEILING ? value * 1000L : value;
    }

    private static long parseExact(String text, String pattern, TimeZone zone) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            format.setTimeZone(zone);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(text, position);
            return parsed != null && position.getIndex() == text.length() ? parsed.getTime() : 0L;
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    /** Returns an RFC-822 offset form accepted consistently by old and new Android runtimes. */
    private static String normalizeExplicitZone(String text) {
        String value = normalizeFraction(text);
        if (value.endsWith("Z") || value.endsWith("z")) {
            return value.substring(0, value.length() - 1) + "+0000";
        }
        if (value.matches(".*[+-]\\d{2}:\\d{2}$")) {
            int colon = value.length() - 3;
            return value.substring(0, colon) + value.substring(colon + 1);
        }
        return value.matches(".*[+-]\\d{4}$") ? value : "";
    }

    /** SimpleDateFormat accepts milliseconds, not arbitrary nanosecond precision. */
    private static String normalizeFraction(String text) {
        int dot = text.indexOf('.');
        if (dot < 0) return text;
        int end = dot + 1;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        String fraction = text.substring(dot + 1, end);
        if (fraction.length() == 0) return text;
        String millis = (fraction + "000").substring(0, 3);
        return text.substring(0, dot + 1) + millis + text.substring(end);
    }

    private static boolean hasFraction(String text) {
        return text.indexOf('.') >= 0;
    }

    private static boolean digitsOnly(String text) {
        if (text.length() == 0) return false;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) return false;
        }
        return true;
    }

    private static long distance(long left, long right) {
        if (left >= right) return left - right;
        return right - left;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
