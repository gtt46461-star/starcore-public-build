package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** R31-B06-C02: content-addressed replay checkpoint with a lightweight transactional reference.
 *
 * <p>The numeric state (model, optimizer, RNG) is encoded together with its owner, input revision, next replay
 * index and model step count into one immutable envelope; its SHA-256 is the only thing the transaction writes.
 * Cursor and model steps therefore cannot be committed apart. Computation and the artifact write happen outside
 * the database transaction (prepare/verify); the transaction only checks the fence, previous state and input
 * revision, then publishes the reference and an idempotent receipt (commitReference). A failed transaction
 * publishes nothing; a repeated delivery of the same commit key is a replay, never a second training step.
 * Unreferenced envelopes are orphans: {@link #reachableStateHashes} gives the reference-reachability set for GC.</p>
 */
public final class ReplayCheckpointR31 {
    private ReplayCheckpointR31() {}
    public static final int MAGIC = 0x52435031, VERSION = 1;
    public static final int MAX_STATE_BYTES = 32 << 20;
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r31_replay_checkpoint(owner TEXT PRIMARY KEY,input_revision TEXT NOT NULL,state_hash TEXT NOT NULL,next_index INTEGER NOT NULL,model_steps INTEGER NOT NULL,fence INTEGER NOT NULL,commit_key TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_replay_checkpoint_receipt(commit_key TEXT PRIMARY KEY,owner TEXT NOT NULL,state_hash TEXT NOT NULL,previous_state_hash TEXT NOT NULL,next_index INTEGER NOT NULL,model_steps INTEGER NOT NULL,committed_at INTEGER NOT NULL)"
    };
    /** Migration owner only: a business transaction never creates replay tables. */
    public static void ensureSchemaR31(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_REPLAY_CHECKPOINT_MIGRATION_TRANSACTION_REQUIRED");
        for (String ddl : DDL) db.execSQL(ddl);
    }

    public static final class Envelope {
        public final String owner, inputRevision, stateHash;
        public final long nextIndex, modelSteps;
        private final byte[] state, encoded;
        private Envelope(String owner, String inputRevision, long nextIndex, long modelSteps, byte[] state, byte[] encoded) {
            this.owner = owner; this.inputRevision = inputRevision; this.nextIndex = nextIndex; this.modelSteps = modelSteps;
            this.state = state; this.encoded = encoded; this.stateHash = sha256(encoded);
        }
        public byte[] state() { return state.clone(); }
        public byte[] encoded() { return encoded.clone(); }
    }
    public enum Outcome { COMMITTED, REPLAYED }
    /** Typed rejection: STALE_OWNER for a superseded owner/previous state, INTEGRITY for a key reused with other content,
     * DEPENDENCY for a changed input revision (the old identity is invalidated, never overwritten). */
    public static final class Rejected extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        public final String category;
        Rejected(String category, String message) { super(message); this.category = category; }
        @Override public String failureCategory() { return category; }
        @Override public String operation() { return "R31_REPLAY_CHECKPOINT"; }
        @Override public String phase() { return "COMMIT_REFERENCE"; }
        @Override public String commitOutcomeName() { return "ROLLED_BACK"; }
    }

    /** Outside any transaction: bind the numeric state to its cursor and step count in one immutable envelope. */
    public static Envelope prepare(String owner, String inputRevision, long nextIndex, long modelSteps, byte[] state) {
        if (owner == null || owner.isEmpty() || owner.length() > 160 || inputRevision == null || inputRevision.isEmpty() || inputRevision.length() > 160
                || nextIndex < 0 || modelSteps < 0 || state == null || state.length > MAX_STATE_BYTES)
            throw new IllegalArgumentException("R31_REPLAY_CHECKPOINT_INPUT");
        byte[] copy = state.clone();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(copy.length + 512);
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC); out.writeInt(VERSION); out.writeUTF(owner); out.writeUTF(inputRevision);
                out.writeLong(nextIndex); out.writeLong(modelSteps); out.writeUTF(sha256(copy)); out.writeInt(copy.length); out.write(copy);
            }
            return new Envelope(owner, inputRevision, nextIndex, modelSteps, copy, bytes.toByteArray());
        } catch (IOException impossible) { throw new IllegalStateException("R31_REPLAY_CHECKPOINT_ENCODE", impossible); }
    }
    /** Outside any transaction: the stored bytes must hash to the reference and decode under a known version. */
    public static Envelope verify(byte[] encoded, String expectedHash) {
        if (encoded == null || encoded.length > MAX_STATE_BYTES + 4096 || expectedHash == null || !expectedHash.equals(sha256(encoded)))
            throw new IllegalArgumentException("R31_REPLAY_CHECKPOINT_CHECKSUM");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
            if (in.readInt() != MAGIC) throw new IOException("magic");
            int version = in.readInt();
            if (version != VERSION) throw new IOException("unknown replay checkpoint version " + version);
            String owner = in.readUTF(), revision = in.readUTF();
            long next = in.readLong(), steps = in.readLong();
            String stateHash = in.readUTF();
            int length = in.readInt();
            if (length < 0 || length > MAX_STATE_BYTES || next < 0 || steps < 0) throw new IOException("bounds");
            byte[] state = new byte[length]; in.readFully(state);
            if (in.read() != -1) throw new IOException("trailing bytes");
            if (!stateHash.equals(sha256(state))) throw new IOException("state hash");
            Envelope envelope = prepare(owner, revision, next, steps, state);
            if (!Arrays.equals(envelope.encoded, encoded)) throw new IOException("non-canonical encoding");
            return envelope;
        } catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException("R31_REPLAY_CHECKPOINT_DECODE", invalid);
        }
    }
    public static String commitKey(String owner, String inputRevision, String previousStateHash, long nextIndex) {
        return sha256((owner + "|" + inputRevision + "|" + (previousStateHash == null ? "" : previousStateHash) + "|" + nextIndex).getBytes(StandardCharsets.UTF_8));
    }

    /** Inside the owner's short write transaction, after its fence check. Publishes the reference only when the current
     * reference is the expected previous state of the same input revision and the cursor advances. */
    public static Outcome commitReference(SQLiteDatabase db, Envelope next, String expectedPreviousStateHash, long fence, long now) {
        if (db == null || !db.inTransaction()) throw new IllegalStateException("R31_REPLAY_CHECKPOINT_TRANSACTION_REQUIRED");
        if (next == null || fence < 0) throw new IllegalArgumentException("R31_REPLAY_CHECKPOINT_COMMIT");
        String previous = expectedPreviousStateHash == null ? "" : expectedPreviousStateHash;
        String key = commitKey(next.owner, next.inputRevision, previous, next.nextIndex);
        try (Cursor receipt = db.rawQuery("SELECT state_hash FROM r31_replay_checkpoint_receipt WHERE commit_key=?", new String[]{key})) {
            if (receipt.moveToFirst()) {
                if (next.stateHash.equals(receipt.getString(0))) return Outcome.REPLAYED;
                throw new Rejected("INTEGRITY", "R31_REPLAY_CHECKPOINT_KEY_CONFLICT");
            }
        }
        String currentHash = "", currentRevision = null; long currentNext = -1, currentFence = -1;
        try (Cursor row = db.rawQuery("SELECT state_hash,input_revision,next_index,fence FROM r31_replay_checkpoint WHERE owner=?", new String[]{next.owner})) {
            if (row.moveToFirst()) { currentHash = row.getString(0); currentRevision = row.getString(1); currentNext = row.getLong(2); currentFence = row.getLong(3); }
        }
        if (currentRevision != null && !currentRevision.equals(next.inputRevision))
            throw new Rejected("DEPENDENCY", "R31_REPLAY_INPUT_REVISION_CHANGED");
        if (!currentHash.equals(previous)) throw new Rejected("STALE_OWNER", "R31_STALE_REPLAY_CHECKPOINT_PREVIOUS");
        if (currentFence > fence) throw new Rejected("STALE_OWNER", "R31_STALE_REPLAY_CHECKPOINT_FENCE");
        if (next.nextIndex <= currentNext) throw new Rejected("STALE_OWNER", "R31_STALE_REPLAY_CHECKPOINT_CURSOR");
        if (currentRevision == null)
            db.execSQL("INSERT INTO r31_replay_checkpoint(owner,input_revision,state_hash,next_index,model_steps,fence,commit_key,updated_at) VALUES(?,?,?,?,?,?,?,?)",
                    new Object[]{next.owner, next.inputRevision, next.stateHash, next.nextIndex, next.modelSteps, fence, key, now});
        else
            db.execSQL("UPDATE r31_replay_checkpoint SET state_hash=?,next_index=?,model_steps=?,fence=?,commit_key=?,updated_at=? WHERE owner=? AND state_hash=? AND input_revision=?",
                    new Object[]{next.stateHash, next.nextIndex, next.modelSteps, fence, key, now, next.owner, previous, next.inputRevision});
        try (Cursor changed = db.rawQuery("SELECT changes()", null)) {
            if (!changed.moveToFirst() || changed.getInt(0) != 1) throw new Rejected("STALE_OWNER", "R31_STALE_REPLAY_CHECKPOINT_CAS");
        }
        db.execSQL("INSERT INTO r31_replay_checkpoint_receipt(commit_key,owner,state_hash,previous_state_hash,next_index,model_steps,committed_at) VALUES(?,?,?,?,?,?,?)",
                new Object[]{key, next.owner, next.stateHash, previous, next.nextIndex, next.modelSteps, now});
        return Outcome.COMMITTED;
    }

    /** Reads the committed reference and resolves it through the owner's content-addressed store. */
    public interface Resolver { byte[] read(String stateHash) throws IOException; }
    public static Envelope restore(SQLiteDatabase db, String owner, Resolver resolver) throws IOException {
        String hash;
        try (Cursor row = db.rawQuery("SELECT state_hash FROM r31_replay_checkpoint WHERE owner=?", new String[]{owner})) {
            if (!row.moveToFirst()) return null;
            hash = row.getString(0);
        }
        return verify(resolver.read(hash), hash);
    }
    /** Every state hash still referenced by a committed checkpoint; anything else in the artifact store is an orphan. */
    public static Set<String> reachableStateHashes(SQLiteDatabase db) {
        Set<String> out = new TreeSet<>();
        try (Cursor rows = db.rawQuery("SELECT state_hash FROM r31_replay_checkpoint", null)) { while (rows.moveToNext()) out.add(rows.getString(0)); }
        return out;
    }

    /** R31-B06-C02#A3: replay performance receipt. Every field is required; HOST/JVM measurements never qualify. */
    public static final class PerformanceReceipt {
        public final double cpuSamplesPerSecond; public final long peakHeapBytes, realtimeP95Ms; public final String measuredOn, receiptId;
        public PerformanceReceipt(double cpuSamplesPerSecond, long peakHeapBytes, long realtimeP95Ms, String measuredOn, String receiptId) {
            this.cpuSamplesPerSecond = cpuSamplesPerSecond; this.peakHeapBytes = peakHeapBytes; this.realtimeP95Ms = realtimeP95Ms;
            this.measuredOn = measuredOn == null ? "" : measuredOn; this.receiptId = receiptId == null ? "" : receiptId;
        }
        public List<String> missingFields() {
            List<String> out = new ArrayList<>();
            if (!(cpuSamplesPerSecond > 0) || Double.isInfinite(cpuSamplesPerSecond)) out.add("cpuSamplesPerSecond");
            if (peakHeapBytes <= 0) out.add("peakHeapBytes");
            if (realtimeP95Ms < 0) out.add("realtimeP95Ms");
            if (measuredOn.isEmpty()) out.add("measuredOn");
            if (receiptId.isEmpty()) out.add("receiptId");
            return out;
        }
    }
    /** More family budget is enabled only by a complete, device-measured receipt that meets every gate value. */
    public static final class PerformanceGate {
        public final double minCpuSamplesPerSecond; public final long maxPeakHeapBytes, maxRealtimeP95Ms;
        public PerformanceGate(double minCpuSamplesPerSecond, long maxPeakHeapBytes, long maxRealtimeP95Ms) {
            if (!(minCpuSamplesPerSecond > 0) || maxPeakHeapBytes <= 0 || maxRealtimeP95Ms < 0) throw new IllegalArgumentException("R31_PERFORMANCE_GATE");
            this.minCpuSamplesPerSecond = minCpuSamplesPerSecond; this.maxPeakHeapBytes = maxPeakHeapBytes; this.maxRealtimeP95Ms = maxRealtimeP95Ms;
        }
        public Map<String, Object> decide(PerformanceReceipt receipt) {
            List<String> reasons = new ArrayList<>();
            if (receipt == null) reasons.add("RECEIPT_MISSING");
            else {
                for (String field : receipt.missingFields()) reasons.add("FIELD_MISSING:" + field);
                if (!"DEVICE".equals(receipt.measuredOn)) reasons.add("NOT_DEVICE_MEASURED:" + receipt.measuredOn);
                if (receipt.cpuSamplesPerSecond > 0 && receipt.cpuSamplesPerSecond < minCpuSamplesPerSecond) reasons.add("THROUGHPUT_BELOW_GATE");
                if (receipt.peakHeapBytes > maxPeakHeapBytes) reasons.add("PEAK_HEAP_ABOVE_GATE");
                if (receipt.realtimeP95Ms > maxRealtimeP95Ms) reasons.add("REALTIME_P95_ABOVE_GATE");
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("decision", reasons.isEmpty() ? "ALLOW_FAMILY_BUDGET_INCREASE" : "KEEP_FAMILY_BUDGET");
            out.put("reasons", reasons);
            out.put("receiptId", receipt == null ? null : receipt.receiptId);
            return out;
        }
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            return out.toString();
        } catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("R31_SHA256", unavailable); }
    }
}
