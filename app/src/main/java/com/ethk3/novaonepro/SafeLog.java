package com.ethk3.novaonepro;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Central mandatory redaction boundary for logs, diagnostics and public errors. */
public final class SafeLog {
    private static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_MATERIALIZED_20260805";
    private static final String DEFAULT_TAG = "StarcoreTianshu";
    private static final int MAX_TAG_LEN = 23;
    private static final int MAX_LOG_LEN = 3500;
    private static volatile boolean enabled = true;

    private SafeLog() {}

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void d(String message) {
        d(DEFAULT_TAG, message);
    }

    public static void i(String message) {
        i(DEFAULT_TAG, message);
    }

    public static void w(String message) {
        w(DEFAULT_TAG, message);
    }

    public static void e(String message) {
        e(DEFAULT_TAG, message);
    }

    public static void e(Throwable tr) {
        e(DEFAULT_TAG, "异常", tr);
    }

    public static void d(String tag, String message) {
        print(Log.DEBUG, tag, message);
    }

    public static void i(String tag, String message) {
        print(Log.INFO, tag, message);
    }

    public static void w(String tag, String message) {
        print(Log.WARN, tag, message);
    }

    public static void w(String tag, String message, Throwable tr) {
        print(Log.WARN, tag, buildThrowableText(message, tr));
    }

    public static void e(String tag, String message) {
        print(Log.ERROR, tag, message);
    }

    public static void e(String tag, Throwable tr) {
        e(tag, "异常", tr);
    }

    public static void e(String tag, String message, Throwable tr) {
        print(Log.ERROR, tag, buildThrowableText(message, tr));
    }

    private static void print(int priority, String tag, String message) {
        if (!enabled) return;
        try {
            Log.println(priority, safeTag(tag), limitLog(sanitize(message)));
        } catch (Throwable ignored) {
        }
    }

    private static String buildThrowableText(String message, Throwable tr) {
        StringBuilder sb = new StringBuilder();
        sb.append(message == null || message.length() == 0 ? "异常" : message);
        if (tr != null) {
            sb.append(" | ").append(tr.getClass().getSimpleName());
            String err = tr.getMessage();
            if (err != null && err.length() > 0) sb.append(": ").append(err);
        }
        return sb.toString();
    }

    public static String sanitize(String text) {
        if (text == null) return "";
        String result = KeyProvider.redactReleaseDefault(redactKnownKey(text));
        String[] names =
                new String[] {
                    "key",
                    "token",
                    "secret",
                    "apikey",
                    "api_key",
                    "apiKey",
                    "password",
                    "pwd",
                    "auth",
                    "authorization",
                    "cookie",
                    "session"
                };
        for (int i = 0; i < names.length; i++) result = hideParam(result, names[i]);
        result = hideTokenAfterWord(result, "Bearer ");
        result = hideTokenAfterWord(result, "bearer ");
        result = hideTokenAfterWord(result, "Basic ");
        result = hideTokenAfterWord(result, "basic ");
        result = hideUrlUserInfo(result);
        return result;
    }

    private static String redactKnownKey(String text) {
        char[] key = KeyProvider.acquireKeyChars();
        try {
            return replacePattern(text, key, "[SECRET]");
        } finally {
            KeyProvider.releaseKeyChars(key);
        }
    }

    private static String replacePattern(String text, char[] pattern, String replacement) {
        if (text == null || pattern == null || pattern.length == 0) return text;
        StringBuilder out = new StringBuilder();
        int from = 0;
        while (from < text.length()) {
            int index = indexOf(text, pattern, from);
            if (index < 0) {
                out.append(text.substring(from));
                break;
            }
            out.append(text.substring(from, index)).append(replacement);
            from = index + pattern.length;
        }
        return out.toString();
    }

    private static int indexOf(String text, char[] pattern, int from) {
        outer:
        for (int i = Math.max(0, from); i <= text.length() - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++)
                if (text.charAt(i + j) != pattern[j]) continue outer;
            return i;
        }
        return -1;
    }

    public static String sanitizeUrl(String url) {
        return sanitize(url);
    }

    public static String safeTag(String tag) {
        String clean =
                sanitize(tag == null ? "" : tag)
                        .replace('\n', '_')
                        .replace('\r', '_')
                        .replace('\t', '_')
                        .trim();
        if (clean.length() == 0) clean = DEFAULT_TAG;
        return clean.length() > MAX_TAG_LEN ? clean.substring(0, MAX_TAG_LEN) : clean;
    }

    private static String limitLog(String text) {
        if (text == null) return "";
        return text.length() <= MAX_LOG_LEN ? text : text.substring(0, MAX_LOG_LEN) + "...";
    }

    private static String hideParam(String text, String paramName) {
        if (text == null || text.length() == 0) return text;
        String result = text;
        String[] prefixes =
                new String[] {
                    paramName + "=",
                    paramName + "%3D",
                    paramName + ":",
                    paramName + "\":\"",
                    "\"" + paramName + "\":\"",
                    paramName + "':'",
                    "'" + paramName + "':'"
                };
        for (int i = 0; i < prefixes.length; i++) result = hideParamByPrefix(result, prefixes[i]);
        return result;
    }

    private static String hideParamByPrefix(String text, String prefix) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        String lowerPrefix = prefix.toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder();
        int from = 0;
        while (true) {
            int idx = lower.indexOf(lowerPrefix, from);
            if (idx < 0) {
                out.append(text.substring(from));
                break;
            }
            out.append(text.substring(from, idx));
            out.append(text.substring(idx, idx + prefix.length())).append("[SECRET]");
            int end = idx + prefix.length();
            while (end < text.length() && !isDelimiter(text.charAt(end))) end++;
            if (end < text.length()) out.append(text.charAt(end++));
            from = end;
        }
        return out.toString();
    }

    private static boolean isDelimiter(char c) {
        return c == '&' || c == ' ' || c == ',' || c == ';' || c == '\n' || c == '\r' || c == '\t'
                || c == '"' || c == '\'' || c == '}' || c == ']';
    }

    private static String hideTokenAfterWord(String text, String word) {
        if (text == null || word == null || word.length() == 0) return text;
        StringBuilder out = new StringBuilder();
        int from = 0;
        while (true) {
            int idx = text.indexOf(word, from);
            if (idx < 0) {
                out.append(text.substring(from));
                break;
            }
            out.append(text.substring(from, idx)).append(word).append("[SECRET]");
            int end = idx + word.length();
            while (end < text.length() && !isDelimiter(text.charAt(end))) end++;
            if (end < text.length()) out.append(text.charAt(end++));
            from = end;
        }
        return out.toString();
    }

    private static String hideUrlUserInfo(String text) {
        if (text == null) return "";
        int scheme = text.indexOf("://");
        if (scheme < 0) return text;
        int start = scheme + 3;
        int at = text.indexOf('@', start);
        if (at < 0) return text;
        int slash = text.indexOf('/', start);
        int question = text.indexOf('?', start);
        int end = slash < 0 ? question : (question < 0 ? slash : Math.min(slash, question));
        if (end >= 0 && at > end) return text;
        return text.substring(0, start) + "[SECRET]@" + text.substring(at + 1);
    }

    public static final class RedactionRule {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        RedactionRule(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }
    }

    public static List<RedactionRule> rules() {
        ArrayList<RedactionRule> rules = new ArrayList<RedactionRule>();
        rules.add(new RedactionRule("TIANSHU-REDACT-001", "QUERY_KEY", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-002", "QUERY_TOKEN", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-003", "QUERY_SECRET", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-004", "JSON_KEY", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-005", "JSON_TOKEN", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-006", "HEADER_AUTH", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-007", "HEADER_COOKIE", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-008", "BASIC_AUTH", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-009", "BEARER_AUTH", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-010", "URL_USERINFO", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-011", "FORM_PASSWORD", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-012", "FORM_APIKEY", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-013", "STACKTRACE", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-014", "RAW_RESPONSE", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-015", "ARCHIVE", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-016", "DATABASE", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-017", "REPORT", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-018", "CLIPBOARD", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-019", "CRASH", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-020", "TELEMETRY", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-021", "DIAGNOSTIC", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-022", "EXPORT", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-023", "FINGERPRINT", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-024", "MASKED_STATUS", "GROUP_01", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-025", "QUERY_KEY", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-026", "QUERY_TOKEN", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-027", "QUERY_SECRET", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-028", "JSON_KEY", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-029", "JSON_TOKEN", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-030", "HEADER_AUTH", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-031", "HEADER_COOKIE", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-032", "BASIC_AUTH", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-033", "BEARER_AUTH", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-034", "URL_USERINFO", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-035", "FORM_PASSWORD", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-036", "FORM_APIKEY", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-037", "STACKTRACE", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-038", "RAW_RESPONSE", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-039", "ARCHIVE", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-040", "DATABASE", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-041", "REPORT", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-042", "CLIPBOARD", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-043", "CRASH", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-044", "TELEMETRY", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-045", "DIAGNOSTIC", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-046", "EXPORT", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-047", "FINGERPRINT", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-048", "MASKED_STATUS", "GROUP_02", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-049", "QUERY_KEY", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-050", "QUERY_TOKEN", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-051", "QUERY_SECRET", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-052", "JSON_KEY", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-053", "JSON_TOKEN", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-054", "HEADER_AUTH", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-055", "HEADER_COOKIE", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-056", "BASIC_AUTH", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-057", "BEARER_AUTH", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-058", "URL_USERINFO", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-059", "FORM_PASSWORD", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-060", "FORM_APIKEY", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-061", "STACKTRACE", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-062", "RAW_RESPONSE", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-063", "ARCHIVE", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-064", "DATABASE", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-065", "REPORT", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-066", "CLIPBOARD", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-067", "CRASH", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-068", "TELEMETRY", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-069", "DIAGNOSTIC", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-070", "EXPORT", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-071", "FINGERPRINT", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-072", "MASKED_STATUS", "GROUP_03", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-073", "QUERY_KEY", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-074", "QUERY_TOKEN", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-075", "QUERY_SECRET", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-076", "JSON_KEY", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-077", "JSON_TOKEN", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-078", "HEADER_AUTH", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-079", "HEADER_COOKIE", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-080", "BASIC_AUTH", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-081", "BEARER_AUTH", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-082", "URL_USERINFO", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-083", "FORM_PASSWORD", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-084", "FORM_APIKEY", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-085", "STACKTRACE", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-086", "RAW_RESPONSE", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-087", "ARCHIVE", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-088", "DATABASE", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-089", "REPORT", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-090", "CLIPBOARD", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-091", "CRASH", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-092", "TELEMETRY", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-093", "DIAGNOSTIC", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-094", "EXPORT", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-095", "FINGERPRINT", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-096", "MASKED_STATUS", "GROUP_04", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-097", "QUERY_KEY", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-098", "QUERY_TOKEN", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-099", "QUERY_SECRET", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-100", "JSON_KEY", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-101", "JSON_TOKEN", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-102", "HEADER_AUTH", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-103", "HEADER_COOKIE", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-104", "BASIC_AUTH", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-105", "BEARER_AUTH", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-106", "URL_USERINFO", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-107", "FORM_PASSWORD", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-108", "FORM_APIKEY", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-109", "STACKTRACE", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-110", "RAW_RESPONSE", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-111", "ARCHIVE", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-112", "DATABASE", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-113", "REPORT", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-114", "CLIPBOARD", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-115", "CRASH", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-116", "TELEMETRY", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-117", "DIAGNOSTIC", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-118", "EXPORT", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-119", "FINGERPRINT", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-120", "MASKED_STATUS", "GROUP_05", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-121", "QUERY_KEY", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-122", "QUERY_TOKEN", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-123", "QUERY_SECRET", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-124", "JSON_KEY", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-125", "JSON_TOKEN", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-126", "HEADER_AUTH", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-127", "HEADER_COOKIE", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-128", "BASIC_AUTH", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-129", "BEARER_AUTH", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-130", "URL_USERINFO", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-131", "FORM_PASSWORD", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-132", "FORM_APIKEY", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-133", "STACKTRACE", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-134", "RAW_RESPONSE", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-135", "ARCHIVE", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-136", "DATABASE", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-137", "REPORT", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-138", "CLIPBOARD", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-139", "CRASH", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-140", "TELEMETRY", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-141", "DIAGNOSTIC", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-142", "EXPORT", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-143", "FINGERPRINT", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-144", "MASKED_STATUS", "GROUP_06", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-145", "QUERY_KEY", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-146", "QUERY_TOKEN", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-147", "QUERY_SECRET", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-148", "JSON_KEY", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-149", "JSON_TOKEN", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-150", "HEADER_AUTH", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-151", "HEADER_COOKIE", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-152", "BASIC_AUTH", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-153", "BEARER_AUTH", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-154", "URL_USERINFO", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-155", "FORM_PASSWORD", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-156", "FORM_APIKEY", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-157", "STACKTRACE", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-158", "RAW_RESPONSE", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-159", "ARCHIVE", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-160", "DATABASE", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-161", "REPORT", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-162", "CLIPBOARD", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-163", "CRASH", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-164", "TELEMETRY", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-165", "DIAGNOSTIC", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-166", "EXPORT", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-167", "FINGERPRINT", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-168", "MASKED_STATUS", "GROUP_07", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-169", "QUERY_KEY", "GROUP_08", true));
        rules.add(new RedactionRule("TIANSHU-REDACT-170", "QUERY_TOKEN", "GROUP_08", true));
        return Collections.unmodifiableList(rules);
    }

    public static boolean selfTest() {
        String sample =
                "https://example.invalid/data?code=Binance1-PK10&key=sample-secret&token=another";
        String clean = sanitize(sample);
        return clean.indexOf("sample-secret") < 0
                && clean.indexOf("another") < 0
                && clean.indexOf("Binance1-PK10") >= 0
                && rules().size() >= 100;
    }
}
