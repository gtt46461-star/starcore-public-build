package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Executes the production observation classifier; Android SQLite types below are named fault doubles. */
public final class RuntimeFaultEvidenceR31HostTest {
    static final class SQLiteDatabaseLockedException extends RuntimeException { SQLiteDatabaseLockedException(String m) { super(m); } }
    static final class SQLiteFullException extends RuntimeException { SQLiteFullException(String m) { super(m); } }
    static final class SQLiteDatabaseCorruptException extends RuntimeException { SQLiteDatabaseCorruptException(String m) { super(m); } }
    static final class SQLiteConstraintException extends RuntimeException { SQLiteConstraintException(String m) { super(m); } }
    static final class SQLiteException extends RuntimeException { SQLiteException(String m) { super(m); } }
    static int assertions;
    static void require(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
    static RuntimeFaultEvidenceR31 evidence(Throwable root) { return RuntimeFaultEvidenceR31.capture(new IllegalStateException("F01_WRITE_FAILED|operation=R30_PREPARED_TASK_CLAIM", root), "FALLBACK"); }
    public static void main(String[] args) {
        require(evidence(new SQLiteDatabaseLockedException("database locked")).category.equals("WRITER_BUSY"), "nested busy");
        require(evidence(new SQLiteFullException("full")).category.equals("DISK_FULL"), "full differs from busy");
        require(evidence(new SQLiteDatabaseCorruptException("corrupt")).category.equals("DATABASE_CORRUPT"), "corruption differs from busy");
        require(evidence(new SQLiteConstraintException("unique failed")).category.equals("CONSTRAINT"), "constraint differs from busy");
        require(evidence(new SQLiteException("syntax error near FROM")).category.equals("SQLITE_OTHER"), "syntax is not busy");
        require(evidence(new InterruptedException("user stop")).category.equals("CANCELLED"), "cancel has own category");
        require(evidence(new IllegalStateException("R30_PREPARED_OWNER_CHANGED")).category.equals("STALE_OWNER"), "stale owner");
        require(evidence(new IllegalStateException("MATRIX_NEEDS_STUDENT")).category.equals("DEPENDENCY"), "student dependency");
        require(evidence(new IllegalArgumentException("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE")).category.equals("DEPENDENCY"), "original report student dependency");
        RuntimeFaultEvidenceR31 missing = RuntimeFaultEvidenceR31.capture(new IllegalStateException("F01_WRITE_FAILED"), "FALLBACK");
        require(missing.category.equals("FAULT"), "a missing root is not inferred busy");
        RuntimeFaultEvidenceR31 busy = evidence(new SQLiteDatabaseLockedException("database locked"));
        require(busy.operation.equals("R30_PREPARED_TASK_CLAIM"), "operation follows wrapper");
        require(((List<?>) JsonR27.parse(busy.causeChainJson)).size() == 2, "nested chain retained");
        RuntimeException a = new RuntimeException("a"), b = new RuntimeException("b"); a.initCause(b); b.initCause(a);
        RuntimeFaultEvidenceR31 cyclic = evidence(a);
        require(cyclic.truncated, "cycle detected");
        require(((List<?>) JsonR27.parse(cyclic.causeChainJson)).size() == 3, "cycle does not repeat");
        Throwable deep = new RuntimeException("leaf"); for (int i = 0; i < 100; i++) deep = new RuntimeException("frame" + i, deep);
        RuntimeFaultEvidenceR31 bounded = evidence(deep);
        require(((List<?>) JsonR27.parse(bounded.causeChainJson)).size() == 16, "16 cause cap");
        require(bounded.stack.getBytes(StandardCharsets.UTF_8).length <= 16 * 1024, "stack byte cap");
        RuntimeFaultEvidenceR31 unicode = evidence(new RuntimeException("数".repeat(100_000)));
        require(unicode.stack.getBytes(StandardCharsets.UTF_8).length <= 16 * 1024, "multibyte stack byte cap");
        require(!unicode.stack.contains("\ufffd"), "no split UTF-8 codepoint");
        String redacted = RuntimeFaultEvidenceR31.clean("api_key=hiddenOne password:hiddenTwo Authorization: Bearer hiddenThree https://alice:hiddenFour@example.test/?token=hiddenFive {\"token\":\"hiddenSix\"}", 4096);
        for (String secret : new String[] {"hiddenOne", "hiddenTwo", "hiddenThree", "hiddenFour", "hiddenFive", "hiddenSix"}) require(!redacted.contains(secret), "credential redacted: " + secret);
        WaitKindR27.Wait wait = WaitKindR27.classify(0, null, new SQLiteFullException("password=hidden"), 0, 1, null, false);
        require(wait.kind.equals(WaitKindR27.ERROR), "P00 preserves scheduler behavior pending P01");
        require(wait.detail.contains("DISK_FULL") && !wait.detail.contains("hidden"), "summary has safe category");
        require(busy.stackHash.equals(StatsR27.sha256(busy.stack)), "stack identity");
        require(busy.fingerprint.equals(evidence(new SQLiteDatabaseLockedException("database locked")).fingerprint), "same root aggregation identity");
        System.out.println(JsonR27.write(Map.of("scope", "PRODUCTION_JAVA_OBSERVATION_HELPER_HOST_FAULT_DOUBLES_NOT_ANDROID_SQLITE", "assertions", assertions, "status", "PASS")));
    }
}
