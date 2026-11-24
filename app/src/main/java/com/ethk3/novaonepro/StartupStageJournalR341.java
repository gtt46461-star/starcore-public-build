package com.ethk3.novaonepro;

import android.content.Context;
import android.system.Os;
import android.system.OsConstants;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;

/** Bounded, recoverable evidence for initialization, even when the application database cannot open. */
public final class StartupStageJournalR341 {
    private static final Object FILE_GUARD = new Object();
    private static final int MAX_FRAME = 2048;
    private static final int EMERGENCY_BYTES = 4096;
    private static final int LIMIT = 256 * 1024 - EMERGENCY_BYTES;
    private static final String SCHEMA = "R341_STARTUP_FRAME_1";

    public enum Stage {
        SERVICE_INITIALIZE, CRITICAL_OPEN, OWNER_DISPATCH, OWNER_BEGIN, CHECKPOINT_PREPARE,
        CHECKPOINT_PRESERVE, RUNTIME_BASE, RETENTION_PLAN, RETENTION_UNIT,
        CHECKPOINT_POST_MIGRATION, UPGRADE_TRANSFORM, SESSION_REGISTER, OWNER_READY
    }

    public enum Outcome {
        VERIFIED, COMMITTED, WAIT_RESOURCE, WAIT_DEPENDENCY, MISSING_SOURCE, INTERRUPTED, STOPPED, BUG, FATAL
    }

    public static final class Token {
        private final String owner;
        private final String attempt;
        private final Stage stage;
        private final String id = UUID.randomUUID().toString();
        public AppendResult beginReceipt;
        Token(String owner, String attempt, Stage stage) {
            this.owner = owner;
            this.attempt = attempt;
            this.stage = stage;
        }
    }

    public static final class AppendResult {
        public final boolean durable;
        public final long sequence;
        public final String reason;
        AppendResult(boolean durable, long sequence, String reason) {
            this.durable = durable;
            this.sequence = sequence;
            this.reason = reason;
        }
    }

    private static final class Readback {
        final List<Map<String, Object>> frames = new ArrayList<>();
        long validBytes;
        long sequence;
        boolean incompleteTail;
    }

    private final File data;
    private final File lock;
    private final File emergency;
    private final RuntimeObservationScopeR341.Clock clock;
    private final RuntimeObservationScopeR341 identity;
    private volatile String error = "";
    private volatile long lastSequence;

    private StartupStageJournalR341(File directory, String owner,
            RuntimeObservationScopeR341.Clock clock, RuntimeObservationScopeR341 identity) {
        if (!owner.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("R341_JOURNAL_OWNER");
        data = new File(directory, owner + ".frames");
        lock = new File(directory, owner + ".lock");
        emergency = new File(directory, owner + ".reserve");
        this.clock = clock;
        this.identity = identity;
    }

    public static StartupStageJournalR341 open(Context app, String owner) {
        return open(new File(app.getNoBackupFilesDir(), "r341-startup"), owner, RuntimeObservationScopeR341.SYSTEM,
                RuntimeObservationScopeR341.capture(app, null, null, RuntimeObservationScopeR341.SYSTEM));
    }

    public static StartupStageJournalR341 open(File directory, String owner,
            RuntimeObservationScopeR341.Clock clock, RuntimeObservationScopeR341 identity) {
        StartupStageJournalR341 journal = new StartupStageJournalR341(directory, owner, clock, identity);
        try {
            initializeDirectory(directory);
            synchronized (FILE_GUARD) {
                if (!journal.emergency.exists()) {
                    try (RandomAccessFile reserve = new RandomAccessFile(journal.emergency, "rw")) {
                        reserve.write(new byte[EMERGENCY_BYTES]);
                        reserve.getFD().sync();
                    }
                }
            }
        } catch (IOException unavailable) {
            journal.error = unavailable.getClass().getSimpleName();
        }
        return journal;
    }

    private static void initializeDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("R341_STARTUP_DIRECTORY_UNAVAILABLE");
        }
    }

    public Token begin(String owner, String attempt, Stage stage, String inputHash) {
        Token token = new Token(bounded(owner), bounded(attempt), stage);
        Map<String, Object> frame = base(token, "BEGIN");
        if (inputHash != null && inputHash.matches("[a-f0-9]{64}")) frame.put("inputHash", inputHash);
        token.beginReceipt = append(frame);
        return token;
    }

    public AppendResult end(Token token, Outcome outcome, String evidence, long retryAtMono) {
        Map<String, Object> frame = base(token, "END");
        frame.put("outcome", outcome.name());
        frame.put("evidenceRef", bounded(evidence));
        if (retryAtMono > 0) frame.put("nextRetryMono", retryAtMono);
        return append(frame);
    }

    public AppendResult recordUncaught(Token token, Throwable failure) {
        Map<String, Object> frame = base(token, "END");
        Outcome outcome = classify(failure);
        frame.put("outcome", outcome.name());
        frame.put("failureType", bounded(failure.getClass().getName()));
        Throwable cause = failure;
        StringBuilder chain = new StringBuilder();
        for (int n = 0; cause != null && n < 6; n++, cause = cause.getCause()) {
            chain.append(cause.getClass().getName()).append(':').append(code(cause)).append('|');
            StackTraceElement[] stack = cause.getStackTrace();
            if (stack.length > 0) chain.append(stack[0].getClassName()).append('.').append(stack[0].getMethodName());
        }
        frame.put("code", code(failure));
        StackTraceElement[] stack = failure.getStackTrace();
        if (stack.length > 0) frame.put("stackTop", bounded(stack[0].getClassName() + "." + stack[0].getMethodName()));
        frame.put("causeChainHash", V51ChecksumEngine.sha256(chain.toString()));
        return append(frame);
    }

    public static Outcome classify(Throwable failure) {
        if (failure instanceof Error) return Outcome.FATAL;
        Throwable cause = failure;
        for (int n = 0; cause != null && n < 8; n++, cause = cause.getCause()) {
            String code = code(cause);
            if (cause instanceof InterruptedException || code.contains("CANCEL")) return Outcome.INTERRUPTED;
            if (code.contains("MISSING_SOURCE") || code.contains("SOURCE_MISSING")
                    || code.contains("CHECKPOINT_MISSING")) return Outcome.MISSING_SOURCE;
            if (code.contains("LOCK") || code.contains("BUSY") || code.contains("SPACE")
                    || code.contains("CAPACITY") || code.contains("DISK") || code.contains("RESOURCE")) {
                return Outcome.WAIT_RESOURCE;
            }
            if (code.contains("DEPENDENCY") || code.contains("SCHEMA_PENDING")) return Outcome.WAIT_DEPENDENCY;
        }
        return Outcome.BUG;
    }

    private static String code(Throwable failure) {
        String raw = failure.getMessage();
        if (raw != null) {
            String token = raw.split("[^A-Z0-9_]", 2)[0];
            if (token.matches("[A-Z][A-Z0-9_]{1,79}")) return token;
        }
        return bounded(failure.getClass().getSimpleName());
    }

    /** Invoke from the same exclusive owner before its first begin in this process. */
    public int recoverOpenAttempts(String boot, String processEpoch) {
        return locked(-1, () -> {
            Readback current = read(data);
            int count = 0;
            for (Map<String, Object> pending : openTokens(current.frames).values()) {
                if (boot.equals(pending.get("bootId")) && processEpoch.equals(pending.get("processEpoch"))) continue;
                Map<String, Object> interrupted = new LinkedHashMap<>(pending);
                interrupted.put("record", "END");
                interrupted.put("outcome", Outcome.INTERRUPTED.name());
                interrupted.put("wallAt", clock.wall());
                interrupted.put("monoAt", clock.monotonic());
                interrupted.put("recoveryProcessEpoch", processEpoch);
                write(current, interrupted);
                count++;
            }
            return count;
        });
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "R341_STARTUP_SNAPSHOT_1");
        try {
            synchronized (FILE_GUARD) {
                Readback previous = read(data);
                lastSequence = Math.max(lastSequence, previous.sequence);
                result.put("frames", previous.frames);
                result.put("partialTail", previous.incompleteTail);
                result.put("lastSequence", previous.sequence);
                result.put("openTokens", new ArrayList<>(openTokens(previous.frames).keySet()));
                try (RandomAccessFile reserve = new RandomAccessFile(emergency, "r")) {
                    int size = reserve.readInt();
                    if (size > 0 && size <= EMERGENCY_BYTES - 4) {
                        byte[] message = new byte[size];
                        reserve.readFully(message);
                        result.put("emergency", new String(message, StandardCharsets.UTF_8));
                    }
                } catch (IOException absent) {
                    result.put("emergencyState", "UNAVAILABLE");
                }
            }
        } catch (IOException unavailable) {
            result.put("readError", unavailable.getClass().getSimpleName());
            result.put("lastSequence", lastSequence);
        }
        result.put("state", error.isEmpty() && !result.containsKey("readError") && !result.containsKey("emergency")
                ? "AVAILABLE" : "OBSERVABILITY_DEGRADED");
        if (!error.isEmpty()) result.put("writeError", error);
        return result;
    }

    public static Map<String, Object> snapshot(Context app, String owner) {
        return new StartupStageJournalR341(new File(app.getNoBackupFilesDir(), "r341-startup"),
                owner, RuntimeObservationScopeR341.SYSTEM, null).snapshot();
    }

    public String export() { return JsonR27.write(snapshot()); }

    public boolean hasDeterministicFailureLimit() {
        try {
            List<Map<String, Object>> frames = read(data).frames;
            String last = null;
            int count = 0;
            for (int i = frames.size() - 1; i >= 0; i--) {
                Map<String, Object> frame = frames.get(i);
                if (!identity.sourceCommit.equals(frame.get("sourceCommit")) ||
                        !"OWNER_BEGIN".equals(frame.get("stage")) || !"END".equals(frame.get("record"))) continue;
                if (!"BUG".equals(frame.get("outcome"))) return false;
                String fingerprint = (String) frame.get("causeChainHash");
                if (fingerprint == null || (last != null && !last.equals(fingerprint))) return false;
                last = fingerprint;
                if (++count == 3) return true;
            }
        } catch (IOException unavailable) {
            return false;
        }
        return false;
    }

    private Map<String, Object> base(Token token, String record) {
        Map<String, Object> frame = new LinkedHashMap<>(identity.exportEnvelope());
        frame.put("schema", SCHEMA);
        frame.put("record", record);
        frame.put("token", token.id);
        frame.put("owner", token.owner);
        frame.put("attempt", token.attempt);
        frame.put("stage", token.stage.name());
        frame.put("wallAt", clock.wall());
        frame.put("monoAt", clock.monotonic());
        return frame;
    }

    private AppendResult append(Map<String, Object> frame) {
        return locked(new AppendResult(false, lastSequence, "OBSERVABILITY_DEGRADED"), () -> {
            Readback current = read(data);
            if ("END".equals(frame.get("record")) && !openTokens(current.frames).containsKey(frame.get("token"))) {
                return new AppendResult(false, current.sequence, "NO_OPEN_DURABLE_BEGIN");
            }
            write(current, frame);
            return new AppendResult(true, lastSequence, "DURABLE");
        });
    }

    private interface Operation<T> { T run() throws IOException; }

    private <T> T locked(T fallback, Operation<T> operation) {
        synchronized (FILE_GUARD) {
            try (RandomAccessFile file = new RandomAccessFile(lock, "rw"); FileLock guard = file.getChannel().lock()) {
                return operation.run();
            } catch (IOException unavailable) {
                error = unavailable.getClass().getSimpleName();
                try (RandomAccessFile reserve = new RandomAccessFile(emergency, "rw")) {
                    if (reserve.length() == EMERGENCY_BYTES) {
                        byte[] marker = ("OBSERVABILITY_DEGRADED|lastSequence=" + lastSequence + "|type=" + error)
                                .getBytes(StandardCharsets.UTF_8);
                        reserve.writeInt(marker.length);
                        reserve.write(marker);
                        reserve.getFD().sync();
                    }
                } catch (IOException ignored) {
                    // The in-memory error still tells the local observer about the unavailable reserve.
                }
                return fallback;
            }
        }
    }

    private void write(Readback current, Map<String, Object> frame) throws IOException {
        long sequence = current.sequence + 1;
        frame.put("sequence", sequence);
        byte[] bytes = bytes(frame);
        if (current.validBytes + bytes.length + 8 > LIMIT) {
            rotate(current, frame);
        } else {
            try (RandomAccessFile file = new RandomAccessFile(data, "rw")) {
                if (current.incompleteTail) file.setLength(current.validBytes);
                file.seek(current.validBytes);
                writeFrame(file, bytes);
                file.getFD().sync();
            }
            current.validBytes += bytes.length + 8;
            current.incompleteTail = false;
            current.frames.add(frame);
        }
        current.sequence = lastSequence = sequence;
    }

    private void rotate(Readback current, Map<String, Object> next) throws IOException {
        Set<String> attempts = new LinkedHashSet<>();
        attempts.add((String) next.get("attempt"));
        for (int i = current.frames.size() - 1; i >= 0 && attempts.size() < 2; i--) {
            attempts.add((String) current.frames.get(i).get("attempt"));
        }
        Set<String> open = openTokens(current.frames).keySet();
        Set<String> firstFaults = new LinkedHashSet<>();
        List<Map<String, Object>> kept = new ArrayList<>();
        long required = 0;
        for (Map<String, Object> item : current.frames) {
            boolean firstFault = ("BUG".equals(item.get("outcome")) || "FATAL".equals(item.get("outcome"))
                    || "MISSING_SOURCE".equals(item.get("outcome"))) && firstFaults.add((String) item.get("stage"));
            if (!attempts.contains(item.get("attempt")) && !open.contains(item.get("token")) && !firstFault) continue;
            kept.add(item);
            required += bytes(item).length + 8;
        }
        next.put("omittedAtRotation", current.frames.size() - kept.size());
        if (required + bytes(next).length + 8 > LIMIT) throw new IOException("R341_JOURNAL_PROTECTED_CAPACITY");
        File part = File.createTempFile("r341-rotation-", ".frames", data.getParentFile());
        try {
            try (RandomAccessFile file = new RandomAccessFile(part, "rw")) {
                for (Map<String, Object> item : kept) writeFrame(file, bytes(item));
                writeFrame(file, bytes(next));
                file.getFD().sync();
                required = file.length();
            }
            if (!part.renameTo(data)) throw new IOException("R341_JOURNAL_REPLACE");
            try {
                FileDescriptor parent = Os.open(data.getParent(), OsConstants.O_RDONLY, 0);
                try { Os.fsync(parent); } finally { Os.close(parent); }
            } catch (android.system.ErrnoException unavailable) {
                throw new IOException("R341_JOURNAL_DIRECTORY_FORCE", unavailable);
            }
            current.frames.clear();
            current.frames.addAll(kept);
            current.frames.add(next);
            current.validBytes = required;
            current.incompleteTail = false;
        } finally {
            part.delete();
        }
    }

    private static byte[] bytes(Map<String, Object> fields) throws IOException {
        byte[] bytes = JsonR27.write(fields).getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 1 || bytes.length > MAX_FRAME) throw new IOException("R341_FRAME_SIZE");
        return bytes;
    }

    private static void writeFrame(RandomAccessFile file, byte[] bytes) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        file.writeInt(bytes.length);
        file.writeInt((int) crc.getValue());
        file.write(bytes);
    }

    @SuppressWarnings("unchecked")
    private static Readback read(File path) throws IOException {
        Readback result = new Readback();
        if (!path.isFile()) return result;
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            long end = file.length();
            if (end > LIMIT + MAX_FRAME + 8) throw new IOException("R341_JOURNAL_LIMIT");
            while (file.getFilePointer() < end) {
                if (end - file.getFilePointer() < 8) { result.incompleteTail = true; break; }
                int size = file.readInt();
                int checksum = file.readInt();
                if (size < 1 || size > MAX_FRAME) throw new IOException("R341_FRAME_SIZE");
                if (end - file.getFilePointer() < size) { result.incompleteTail = true; break; }
                byte[] raw = new byte[size];
                file.readFully(raw);
                CRC32 crc = new CRC32();
                crc.update(raw);
                if ((int) crc.getValue() != checksum) throw new IOException("R341_FRAME_CRC");
                try {
                    Map<String, Object> entry = (Map<String, Object>) JsonR27.parse(new String(raw, StandardCharsets.UTF_8));
                    long seq = ((Number) entry.get("sequence")).longValue();
                    if (!SCHEMA.equals(entry.get("schema")) || seq <= result.sequence) {
                        throw new IOException("R341_FRAME_SEQUENCE");
                    }
                    result.frames.add(entry);
                    result.sequence = seq;
                    result.validBytes = file.getFilePointer();
                } catch (RuntimeException malformed) {
                    throw new IOException("R341_FRAME_PAYLOAD", malformed);
                }
            }
        }
        return result;
    }

    private static Map<String, Map<String, Object>> openTokens(List<Map<String, Object>> frames) {
        Map<String, Map<String, Object>> open = new LinkedHashMap<>();
        for (Map<String, Object> frame : frames) {
            String token = (String) frame.get("token");
            if ("BEGIN".equals(frame.get("record"))) open.put(token, frame);
            else open.remove(token);
        }
        return open;
    }

    private static String bounded(String value) {
        if (value == null) return "";
        String clean = value.replaceAll("[^A-Za-z0-9_.,:@+/-]", "_");
        return clean.length() <= 96 ? clean : clean.substring(0, 96);
    }
}
