package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** B00 observation only. Categories describe evidence and do not grant retries or suppress faults. */
public final class RuntimeFaultEvidenceR31 {
    public static final int MAX_CAUSES = 16, MAX_STACK_BYTES = 16 * 1024;
    private static final Pattern URL_CREDENTIALS = Pattern.compile("(?i)(https?://)[^/\\s@]+@");
    private static final Pattern QUERY = Pattern.compile("(?i)(https?://[^\\s?]+)\\?[^\\s]*");
    private static final Pattern SECRET = Pattern.compile("(?i)((?:api[_-]?key|authorization|access[_-]?token|refresh[_-]?token|token|password|passwd|secret)[\\\"']?\\s*[:=]\\s*[\\\"']?)(?:Bearer\\s+)?[^\\s,;\\\"'}]+");
    public final String category, operation, causeChainJson, stack, stackHash, fingerprint;
    public final boolean truncated;

    private RuntimeFaultEvidenceR31(String category, String operation, String chain, String stack, String fingerprint, boolean truncated) {
        this.category = category; this.operation = operation; this.causeChainJson = chain;
        this.stack = stack; this.stackHash = StatsR27.sha256(stack); this.fingerprint = fingerprint;
        this.truncated = truncated;
    }

    public static RuntimeFaultEvidenceR31 capture(Throwable failure, String fallbackOperation) {
        if (failure == null) throw new IllegalArgumentException("R31_FAILURE_REQUIRED");
        List<Object> chain = new ArrayList<>();
        IdentityHashMap<Throwable, Boolean> seen = new IdentityHashMap<>();
        String operation = clean(fallbackOperation, 128), category = "FAULT";
        StringBuilder stack = new StringBuilder();
        Throwable current = failure; String rootClass = "", rootMessage = "";
        while (current != null && chain.size() < MAX_CAUSES && !seen.containsKey(current)) {
            seen.put(current, Boolean.TRUE);
            String type = current.getClass().getName(), message = clean(current.getMessage(), 1024);
            String candidate = category(type, message);
            if (!"FAULT".equals(candidate)) category = candidate;
            int op = message.indexOf("operation=");
            if (op >= 0) {
                String value = message.substring(op + 10).split("[^A-Za-z0-9_:.-]", 2)[0];
                if (!value.isEmpty()) operation = clean(value, 128);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("class", type); item.put("message", message); item.put("category", candidate); chain.add(item);
            appendBounded(stack, type + ": " + message + "\n");
            StackTraceElement[] frames = current.getStackTrace();
            for (int i = 0; i < Math.min(64, frames.length); i++) appendBounded(stack, "  at " + frames[i] + "\n");
            if (frames.length > 64) appendBounded(stack, "  [frames truncated]\n");
            rootClass = type; rootMessage = message;
            current = current.getCause();
        }
        boolean truncated = current != null;
        if (truncated) appendBounded(stack, "[cause chain bounded or cyclic]\n");
        String value = clean(stack.toString(), MAX_STACK_BYTES);
        String fingerprint = StatsR27.sha256(category + "|" + rootClass + "|" + rootMessage);
        return new RuntimeFaultEvidenceR31(category, operation, JsonR27.write(chain), value, fingerprint, truncated);
    }

    private static String category(String type, String message) {
        if (type.endsWith("SQLiteFullException")) return "DISK_FULL";
        if (type.endsWith("SQLiteDatabaseCorruptException")) return "DATABASE_CORRUPT";
        if (type.endsWith("SQLiteConstraintException")) return "CONSTRAINT";
        if (type.endsWith("SQLiteDatabaseLockedException") || type.endsWith("SQLiteTableLockedException")
                || type.endsWith("SQLiteBusyException") || message.startsWith("CP3_DB_BUSY_DEADLINE")
                || message.startsWith("R909_BACKGROUND_WRITER_YIELD")) return "WRITER_BUSY";
        if (message.startsWith("R26_STALE_FENCE") || message.startsWith("R30_PREPARED_OWNER_CHANGED")) return "STALE_OWNER";
        if (type.equals("java.lang.InterruptedException") || type.equals("java.util.concurrent.CancellationException")
                || message.equals("R27_CANCELLED")) return "CANCELLED";
        if (message.contains("MATRIX_NEEDS_STUDENT") || message.contains("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE")
                || message.contains("MISSING_TEACHER") || message.contains("SCHEMA_PENDING")) return "DEPENDENCY";
        if (type.endsWith("SQLiteException")) return "SQLITE_OTHER";
        return "FAULT";
    }

    /** UTF-8 byte limit is applied before regexes so attacker-sized messages cannot expand work unboundedly. */
    public static String clean(String value, int maxBytes) {
        String bounded = prefixUtf8(value == null ? "" : value, maxBytes);
        bounded = URL_CREDENTIALS.matcher(bounded).replaceAll("$1[REDACTED]@");
        bounded = QUERY.matcher(bounded).replaceAll("$1?[REDACTED]");
        bounded = SECRET.matcher(bounded).replaceAll("$1[REDACTED]");
        return prefixUtf8(bounded, maxBytes);
    }

    private static String prefixUtf8(String value, int maxBytes) {
        int end = 0, bytes = 0;
        while (end < value.length()) {
            int cp = value.codePointAt(end), width = cp <= 0x7f ? 1 : cp <= 0x7ff ? 2 : cp <= 0xffff ? 3 : 4;
            if (bytes + width > maxBytes) break;
            bytes += width; end += Character.charCount(cp);
        }
        return value.substring(0, end);
    }

    private static void appendBounded(StringBuilder out, String value) {
        // At most 16 Ki UTF-16 units before final UTF-8 limiting; no unbounded printStackTrace buffer.
        int remaining = MAX_STACK_BYTES - out.length();
        if (remaining > 0) out.append(clean(value, remaining));
    }

    public int bytes() { return (causeChainJson + stack).getBytes(StandardCharsets.UTF_8).length; }
}
