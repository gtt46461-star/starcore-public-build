package com.ethk3.novaonepro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Versioned, durable, non-Serializable snapshot for exact 100/1200 materialization. */
public final class DeferredEvidenceSnapshotR36 {
    // Keep the R3.6.1 on-disk snapshot magic so R3.6.3 can resume already durable jobs.
    private static final int MAGIC = 0x52333631;

    private DeferredEvidenceSnapshotR36() {}

    /** Lightweight metadata needed by the atomic core commit. */
    public static final class Header {
        public final String targetIssue,
                baseIssue,
                historyHash,
                bundleHash,
                contractHash,
                actionSpaceHash,
                recordHash;

        Header(
                String target,
                String base,
                String history,
                String bundle,
                String contract,
                String actionSpace,
                String record) {
            targetIssue = safe(target);
            baseIssue = safe(base);
            historyHash = safe(history);
            bundleHash = safe(bundle);
            contractHash = safe(contract);
            actionSpaceHash = safe(actionSpace);
            recordHash = safe(record);
            validate();
        }

        private void validate() {
            if (!TargetIssueResolver.exact(baseIssue, targetIssue).valid)
                throw new IllegalStateException("R368_SNAPSHOT_HEADER_ISSUE");
            if (historyHash.length() != 64
                    || bundleHash.length() != 64
                    || contractHash.length() != 64
                    || actionSpaceHash.length() != 64
                    || recordHash.length() != 64)
                throw new IllegalStateException("R368_SNAPSHOT_HEADER_HASH");
        }
    }

    /**
     * Runtime-path snapshot result.  It keeps only the compressed payload plus seven core strings;
     * the 100/1200 object graph is decoded later by the deferred writer after live pressure clears.
     */
    public static final class EncodedSnapshot {
        public final Header header;
        public final byte[] bytes;
        public final String sha256;

        EncodedSnapshot(Header header, byte[] bytes) {
            this.header = header;
            this.bytes = bytes;
            this.sha256 = DeferredEvidenceSnapshotR36.sha256(bytes);
        }
    }

    public static Snapshot fromRuntimeResult(V51RuntimeResult result, RuntimeTraceR36 trace) {
        if (result == null
                || !result.success
                || result.context == null
                || result.numberModel == null
                || result.numberModel.matrix == null
                || result.states == null
                || result.actions == null)
            throw new IllegalArgumentException("R36_SNAPSHOT_RUNTIME_INVALID");
        if (result.states.length != 10) throw new IllegalStateException("R36_STATE_COUNT_NE_10");
        K3ActionSpaceEngine.validateOrThrow(result.actions);
        if (result.actions.length != 1200)
            throw new IllegalStateException("R36_ACTION_COUNT_NE_1200");

        String target = safe(result.context.targetIssue), base = safe(result.context.baseIssue);
        String historyHash = safe(result.context.historyHash);
        String expertHash = expertHash(result.numberModel.experts);
        ArrayList<NumberRow> numbers = new ArrayList<NumberRow>(100);
        for (int p = 0; p < 10; p++) {
            int[] ranks = rankRow(result.numberModel.matrix.probability[p]);
            for (int n = 0; n < 10; n++)
                numbers.add(
                        new NumberRow(
                                p + 1,
                                n + 1,
                                finite(result.numberModel.rawScore[p][n], 0.0d),
                                finite(result.numberModel.matrix.probability[p][n], 0.0d),
                                ranks[n],
                                safe(result.states[p].prototypeId),
                                safe(result.states[p].domainId),
                                expertHash));
        }
        String contractHash =
                result.selected == null
                        ? V51ChecksumEngine.sha256("NO_SELECTED|" + target)
                        : safe(result.selected.contractHash);
        String bundleHash =
                V51ChecksumEngine.sha256(
                        V51Repository.GAME_CODE
                                + '|'
                                + target
                                + '|'
                                + historyHash
                                + '|'
                                + safe(result.checksum)
                                + '|'
                                + contractHash);
        ArrayList<ActionRow> actions = new ArrayList<ActionRow>(1200);
        for (int i = 0; i < result.actions.length; i++) {
            V51Models.PositionAction a = result.actions[i];
            double selectedP =
                    a.rank == 1 && result.selected != null
                            ? finite(result.selected.selectedHitP, -1.0d)
                            : -1.0d;
            String record =
                    V51ChecksumEngine.sha256(
                            target
                                    + "|A1200|"
                                    + a.position
                                    + '|'
                                    + a.n1
                                    + '|'
                                    + a.n2
                                    + '|'
                                    + a.n3
                                    + '|'
                                    + safe(a.checksum));
            actions.add(
                    new ActionRow(
                            a.position,
                            a.n1,
                            a.n2,
                            a.n3,
                            a.n4,
                            finite(a.rawActionP, 0.0d),
                            finite(a.rankScore, 0.0d),
                            selectedP,
                            a.rank,
                            safe(a.fingerprint),
                            doubles(a.metaFeatures),
                            safe(a.checksum),
                            record));
        }
        String actionSpaceHash = K3ActionSpaceEngine.actionSpaceHash(result.actions);
        String recordHash =
                V51ChecksumEngine.sha256(
                        "R36_SNAPSHOT|"
                                + target
                                + '|'
                                + historyHash
                                + '|'
                                + bundleHash
                                + '|'
                                + contractHash
                                + '|'
                                + actionSpaceHash);
        ArrayList<String> proofs = runtimeProofs(result);
        return new Snapshot(
                target,
                base,
                historyHash,
                bundleHash,
                contractHash,
                actionSpaceHash,
                recordHash,
                matrix(result.numberModel.matrix.probability),
                safe(result.numberModel.matrix.checksum),
                result.numberModel.matrix.maxRowError,
                result.numberModel.matrix.maxColumnError,
                result.numberModel.matrix.iterations,
                result.numberModel.matrix.converged,
                result.numberModel.matrix.usedFallback,
                ints(result.numberModel.matrix.mapPermutation),
                numbers,
                actions,
                proofs,
                trace == null ? Collections.<RuntimeTraceR36.Span>emptyList() : trace.spans());
    }

    /**
     * Low-peak encoder used by the sole live orchestrator.  The on-disk schema and canonical bytes
     * are identical to {@code encode(fromRuntimeResult(...))}; only the temporary allocation graph
     * is different.
     */
    public static EncodedSnapshot encodeRuntimeResult(
            V51RuntimeResult result, RuntimeTraceR36 trace, String validatedActionSpaceHash) {
        validateRuntimeResult(result);
        if (safe(validatedActionSpaceHash).length() != 64)
            throw new IllegalArgumentException("R368_ACTION_SPACE_HASH_REQUIRED");
        verifyRuntimeSurfaceLightweight(result.actions);

        String target = safe(result.context.targetIssue);
        String base = safe(result.context.baseIssue);
        String historyHash = safe(result.context.historyHash);
        String expertHash = expertHash(result.numberModel.experts);
        String contractHash =
                result.selected == null
                        ? V51ChecksumEngine.sha256("NO_SELECTED|" + target)
                        : safe(result.selected.contractHash);
        String bundleHash =
                V51ChecksumEngine.sha256(
                        V51Repository.GAME_CODE
                                + '|'
                                + target
                                + '|'
                                + historyHash
                                + '|'
                                + safe(result.checksum)
                                + '|'
                                + contractHash);
        String recordHash =
                V51ChecksumEngine.sha256(
                        "R36_SNAPSHOT|"
                                + target
                                + '|'
                                + historyHash
                                + '|'
                                + bundleHash
                                + '|'
                                + contractHash
                                + '|'
                                + validatedActionSpaceHash);
        Header header =
                new Header(
                        target,
                        base,
                        historyHash,
                        bundleHash,
                        contractHash,
                        validatedActionSpaceHash,
                        recordHash);
        List<RuntimeTraceR36.Span> spans =
                trace == null
                        ? Collections.<RuntimeTraceR36.Span>emptyList()
                        : trace.spans();
        try {
            /* 64 KiB avoids the former unconditional 512 KiB reservation on a nearly full heap. */
            ByteArrayOutputStream raw = new ByteArrayOutputStream(64 * 1024);
            GZIPOutputStream gzip = new GZIPOutputStream(raw, 32 * 1024);
            DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(gzip, 64 * 1024));
            out.writeInt(MAGIC);
            out.writeInt(AppConfig.R36_SNAPSHOT_SCHEMA);
            writeUtf(out, header.targetIssue);
            writeUtf(out, header.baseIssue);
            writeUtf(out, header.historyHash);
            writeUtf(out, header.bundleHash);
            writeUtf(out, header.contractHash);
            writeUtf(out, header.actionSpaceHash);
            writeUtf(out, header.recordHash);
            writeUtf(out, matrix(result.numberModel.matrix.probability));
            writeUtf(out, safe(result.numberModel.matrix.checksum));
            out.writeDouble(result.numberModel.matrix.maxRowError);
            out.writeDouble(result.numberModel.matrix.maxColumnError);
            out.writeInt(result.numberModel.matrix.iterations);
            out.writeBoolean(result.numberModel.matrix.converged);
            out.writeBoolean(result.numberModel.matrix.usedFallback);
            writeUtf(out, ints(result.numberModel.matrix.mapPermutation));

            out.writeInt(100);
            for (int p = 0; p < 10; p++) {
                int[] ranks = rankRow(result.numberModel.matrix.probability[p]);
                for (int n = 0; n < 10; n++) {
                    out.writeInt(p + 1);
                    out.writeInt(n + 1);
                    out.writeDouble(finite(result.numberModel.rawScore[p][n], 0.0d));
                    out.writeDouble(
                            finite(result.numberModel.matrix.probability[p][n], 0.0d));
                    out.writeInt(ranks[n]);
                    writeUtf(out, safe(result.states[p].prototypeId));
                    writeUtf(out, safe(result.states[p].domainId));
                    writeUtf(out, expertHash);
                }
            }

            out.writeInt(1200);
            for (int i = 0; i < result.actions.length; i++) {
                V51Models.PositionAction action = result.actions[i];
                double selectedP =
                        action.rank == 1 && result.selected != null
                                ? finite(result.selected.selectedHitP, -1.0d)
                                : -1.0d;
                String actionRecordHash =
                        V51ChecksumEngine.sha256(
                                target
                                        + "|A1200|"
                                        + action.position
                                        + '|'
                                        + action.n1
                                        + '|'
                                        + action.n2
                                        + '|'
                                        + action.n3
                                        + '|'
                                        + safe(action.checksum));
                out.writeInt(action.position);
                out.writeInt(action.n1);
                out.writeInt(action.n2);
                out.writeInt(action.n3);
                out.writeInt(action.n4);
                out.writeDouble(finite(action.rawActionP, 0.0d));
                out.writeDouble(finite(action.rankScore, 0.0d));
                out.writeDouble(selectedP);
                out.writeInt(action.rank);
                writeUtf(out, safe(action.fingerprint));
                writeUtf(out, doubles(action.metaFeatures));
                writeUtf(out, safe(action.checksum));
                writeUtf(out, actionRecordHash);
            }

            ArrayList<String> proofs=runtimeProofs(result);
            out.writeInt(proofs.size());
            for(String proof:proofs)writeUtf(out,proof);
            out.writeInt(spans.size());
            for (int i = 0; i < spans.size(); i++) {
                RuntimeTraceR36.Span span = spans.get(i);
                writeUtf(out, span.stage);
                out.writeLong(span.startedAt);
                out.writeLong(span.elapsedMs);
                out.writeBoolean(span.success);
                writeUtf(out, span.detail);
            }
            out.flush();
            gzip.finish();
            out.close();
            byte[] bytes = raw.toByteArray();
            if (bytes.length > AppConfig.R36_SNAPSHOT_MAX_BYTES)
                throw new IllegalStateException("SNAPSHOT_TOO_LARGE:" + bytes.length);
            return new EncodedSnapshot(header, bytes);
        } catch (RuntimeException error) {
            throw error;
        } catch (Throwable error) {
            throw new IllegalStateException("R368_STREAM_SNAPSHOT_ENCODE_FAILED", error);
        }
    }

    private static ArrayList<String> runtimeProofs(V51RuntimeResult result){
        ArrayList<String> proofs = new ArrayList<String>();
        proofs.add("GAME=" + V51Repository.GAME_CODE);
        proofs.add("FORMAL_ACTIONS=1200");
        proofs.add("K=3");
        proofs.add("MODEL_PROVEN=" + V51BuildManifest.MODEL_PROVEN);
        proofs.add("SHADOW_ONLY=" + V51BuildManifest.SHADOW_ONLY);
        proofs.add("REAL_UNLOCK=" + AppConfig.R36_REAL_BET_UNLOCK);
        RawTrainingInputR932.append(proofs,result);
        return proofs;
    }

    private static void validateRuntimeResult(V51RuntimeResult result) {
        if (result == null
                || !result.success
                || result.context == null
                || result.numberModel == null
                || result.numberModel.matrix == null
                || result.numberModel.rawScore == null
                || result.states == null
                || result.states.length != 10
                || result.actions == null
                || result.actions.length != 1200)
            throw new IllegalArgumentException("R368_STREAM_SNAPSHOT_RUNTIME_INVALID");
    }

    /** Allocation-bounded cardinality/rank validation; the full payload hash was frozen upstream. */
    private static void verifyRuntimeSurfaceLightweight(V51Models.PositionAction[] actions) {
        boolean[] identity = new boolean[1200];
        boolean[] rank = new boolean[1201];
        int[] perPosition = new int[10];
        for (int i = 0; i < actions.length; i++) {
            V51Models.PositionAction action = actions[i];
            if (action == null
                    || !K3ActionSpaceEngine.validFourthBoundary(
                            action.n1, action.n2, action.n3, action.n4)
                    || action.rank < 1
                    || action.rank > 1200)
                throw new IllegalStateException("R368_STREAM_ACTION_INVALID_" + i);
            int ordinal =
                    K3ActionSpaceEngine.globalOrdinal(
                            action.position, action.n1, action.n2, action.n3);
            if (ordinal < 0 || identity[ordinal] || rank[action.rank])
                throw new IllegalStateException("R368_STREAM_ACTION_DUPLICATE_" + i);
            identity[ordinal] = true;
            rank[action.rank] = true;
            perPosition[action.position - 1]++;
        }
        for (int p = 0; p < 10; p++)
            if (perPosition[p] != 120)
                throw new IllegalStateException("R368_STREAM_POSITION_COUNT_" + (p + 1));
    }

    public static byte[] encode(Snapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("R36_SNAPSHOT_NULL");
        snapshot.validate();
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream(512 * 1024);
            GZIPOutputStream gzip = new GZIPOutputStream(raw);
            DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(gzip, 64 * 1024));
            out.writeInt(MAGIC);
            out.writeInt(AppConfig.R36_SNAPSHOT_SCHEMA);
            writeUtf(out, snapshot.targetIssue);
            writeUtf(out, snapshot.baseIssue);
            writeUtf(out, snapshot.historyHash);
            writeUtf(out, snapshot.bundleHash);
            writeUtf(out, snapshot.contractHash);
            writeUtf(out, snapshot.actionSpaceHash);
            writeUtf(out, snapshot.recordHash);
            writeUtf(out, snapshot.permutationBlob);
            writeUtf(out, snapshot.permutationHash);
            out.writeDouble(snapshot.permutationRowError);
            out.writeDouble(snapshot.permutationColumnError);
            out.writeInt(snapshot.permutationIterations);
            out.writeBoolean(snapshot.permutationConverged);
            out.writeBoolean(snapshot.permutationUsedFallback);
            writeUtf(out, snapshot.permutationMapBlob);
            out.writeInt(snapshot.numbers.size());
            for (int i = 0; i < snapshot.numbers.size(); i++) snapshot.numbers.get(i).write(out);
            out.writeInt(snapshot.actions.size());
            for (int i = 0; i < snapshot.actions.size(); i++) snapshot.actions.get(i).write(out);
            out.writeInt(snapshot.proofs.size());
            for (int i = 0; i < snapshot.proofs.size(); i++) writeUtf(out, snapshot.proofs.get(i));
            out.writeInt(snapshot.spans.size());
            for (int i = 0; i < snapshot.spans.size(); i++) {
                RuntimeTraceR36.Span s = snapshot.spans.get(i);
                writeUtf(out, s.stage);
                out.writeLong(s.startedAt);
                out.writeLong(s.elapsedMs);
                out.writeBoolean(s.success);
                writeUtf(out, s.detail);
            }
            out.flush();
            gzip.finish();
            out.close();
            byte[] bytes = raw.toByteArray();
            if (bytes.length > AppConfig.R36_SNAPSHOT_MAX_BYTES)
                throw new IllegalStateException("SNAPSHOT_TOO_LARGE:" + bytes.length);
            return bytes;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("R36_SNAPSHOT_ENCODE_FAILED", e);
        }
    }

    private static final java.util.LinkedHashMap<String,Snapshot> DECODE_CACHE =
            new java.util.LinkedHashMap<String,Snapshot>(3,.75f,true);
    /** At most two immutable evidence snapshots: live research and oldest deferred progress. */
    public static Snapshot decodeCached(byte[] bytes,String expectedHash) {
        if(expectedHash==null||!expectedHash.equals(sha256(bytes)))throw new IllegalArgumentException("SNAPSHOT_HASH_MISMATCH");
        synchronized(DECODE_CACHE){Snapshot cached=DECODE_CACHE.get(expectedHash);if(cached!=null)return cached;}
        Snapshot parsed=decode(bytes);
        synchronized(DECODE_CACHE){DECODE_CACHE.put(expectedHash,parsed);while(DECODE_CACHE.size()>2)DECODE_CACHE.remove(DECODE_CACHE.keySet().iterator().next());}
        return parsed;
    }

    public static Snapshot decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > AppConfig.R36_SNAPSHOT_MAX_BYTES)
            throw new IllegalArgumentException("R36_SNAPSHOT_BYTES_INVALID");
        try {
            DataInputStream in =
                    new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)), 64 * 1024));
            if (in.readInt() != MAGIC)
                throw new IllegalStateException("R36_SNAPSHOT_MAGIC_MISMATCH");
            int schema = in.readInt();
            if (schema != AppConfig.R36_SNAPSHOT_SCHEMA)
                throw new IllegalStateException("R36_SNAPSHOT_SCHEMA_" + schema);
            String target = readUtf(in),
                    base = readUtf(in),
                    history = readUtf(in),
                    bundle = readUtf(in),
                    contract = readUtf(in);
            String actionHash = readUtf(in),
                    record = readUtf(in),
                    permutation = readUtf(in),
                    permutationHash = readUtf(in);
            double rowError = in.readDouble(), columnError = in.readDouble();
            int iterations = in.readInt();
            boolean converged = in.readBoolean(), usedFallback = in.readBoolean();
            String mapBlob = readUtf(in);
            int numberCount = in.readInt();
            if (numberCount != 100)
                throw new IllegalStateException("R36_NUMBER_COUNT_" + numberCount);
            ArrayList<NumberRow> numbers = new ArrayList<NumberRow>(100);
            for (int i = 0; i < numberCount; i++) numbers.add(NumberRow.read(in));
            int actionCount = in.readInt();
            if (actionCount != 1200)
                throw new IllegalStateException("R36_ACTION_COUNT_" + actionCount);
            ArrayList<ActionRow> actions = new ArrayList<ActionRow>(1200);
            for (int i = 0; i < actionCount; i++) actions.add(ActionRow.read(in));
            int proofCount = in.readInt();
            if (proofCount < 0 || proofCount > 128)
                throw new IllegalStateException("R36_PROOF_COUNT");
            ArrayList<String> proofs = new ArrayList<String>();
            for (int i = 0; i < proofCount; i++) proofs.add(readUtf(in));
            int spanCount = in.readInt();
            if (spanCount < 0 || spanCount > 512) throw new IllegalStateException("R36_SPAN_COUNT");
            ArrayList<RuntimeTraceR36.Span> spans = new ArrayList<RuntimeTraceR36.Span>();
            for (int i = 0; i < spanCount; i++)
                spans.add(
                        new RuntimeTraceR36.Span(
                                readUtf(in),
                                in.readLong(),
                                in.readLong(),
                                in.readBoolean(),
                                readUtf(in)));
            Snapshot snapshot =
                    new Snapshot(
                            target,
                            base,
                            history,
                            bundle,
                            contract,
                            actionHash,
                            record,
                            permutation,
                            permutationHash,
                            rowError,
                            columnError,
                            iterations,
                            converged,
                            usedFallback,
                            mapBlob,
                            numbers,
                            actions,
                            proofs,
                            spans);
            snapshot.validate();
            in.close();
            return snapshot;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("R36_SNAPSHOT_DECODE_FAILED", e);
        }
    }

    public static String sha256(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("R36_HASH_BYTES_NULL");
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Throwable e) {
            throw new IllegalStateException("R36_SHA256_UNAVAILABLE", e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder x = new StringBuilder(b.length * 2);
        char[] h = "0123456789abcdef".toCharArray();
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 255;
            x.append(h[v >>> 4]).append(h[v & 15]);
        }
        return x.toString();
    }

    private static void writeUtf(DataOutputStream out, String s) throws Exception {
        byte[] b = safe(s).getBytes("UTF-8");
        if (b.length > 1048576) throw new IllegalStateException("R36_UTF_TOO_LARGE");
        out.writeInt(b.length);
        out.write(b);
    }

    private static String readUtf(DataInputStream in) throws Exception {
        int n = in.readInt();
        if (n < 0 || n > 1048576) throw new IllegalStateException("R36_UTF_LENGTH");
        byte[] b = new byte[n];
        in.readFully(b);
        return new String(b, "UTF-8");
    }

    public static final class Snapshot {
        public final String targetIssue,
                baseIssue,
                historyHash,
                bundleHash,
                contractHash,
                actionSpaceHash,
                recordHash,
                permutationBlob,
                permutationHash;
        public final double permutationRowError, permutationColumnError;
        public final int permutationIterations;
        public final boolean permutationConverged, permutationUsedFallback;
        public final String permutationMapBlob;
        public final List<NumberRow> numbers;
        public final List<ActionRow> actions;
        public final List<String> proofs;
        public final List<RuntimeTraceR36.Span> spans;

        Snapshot(
                String target,
                String base,
                String history,
                String bundle,
                String contract,
                String actionHash,
                String record,
                String permutation,
                String permutationHash,
                double rowError,
                double columnError,
                int iterations,
                boolean converged,
                boolean usedFallback,
                String mapBlob,
                List<NumberRow> numbers,
                List<ActionRow> actions,
                List<String> proofs,
                List<RuntimeTraceR36.Span> spans) {
            targetIssue = safe(target);
            baseIssue = safe(base);
            historyHash = safe(history);
            bundleHash = safe(bundle);
            contractHash = safe(contract);
            actionSpaceHash = safe(actionHash);
            recordHash = safe(record);
            permutationBlob = safe(permutation);
            this.permutationHash = safe(permutationHash);
            permutationRowError = Math.max(0.0d, finite(rowError, 0.0d));
            permutationColumnError = Math.max(0.0d, finite(columnError, 0.0d));
            permutationIterations = Math.max(0, iterations);
            permutationConverged = converged;
            permutationUsedFallback = usedFallback;
            permutationMapBlob = safe(mapBlob);
            this.numbers = Collections.unmodifiableList(new ArrayList<NumberRow>(numbers));
            this.actions = Collections.unmodifiableList(new ArrayList<ActionRow>(actions));
            this.proofs = Collections.unmodifiableList(new ArrayList<String>(proofs));
            this.spans = Collections.unmodifiableList(new ArrayList<RuntimeTraceR36.Span>(spans));
        }

        void validate() {
            if (!TargetIssueResolver.exact(baseIssue, targetIssue).valid)
                throw new IllegalStateException("R36_SNAPSHOT_ISSUE_MISMATCH");
            if (historyHash.length() != 64
                    || bundleHash.length() != 64
                    || contractHash.length() != 64
                    || actionSpaceHash.length() != 64
                    || recordHash.length() != 64)
                throw new IllegalStateException("R36_SNAPSHOT_HASH_INVALID");
            if (numbers.size() != 100 || actions.size() != 1200)
                throw new IllegalStateException("R36_SNAPSHOT_CARDINALITY");
            boolean[][] numberSeen = new boolean[10][10];
            int[] numberCount = new int[10];
            for (int i = 0; i < numbers.size(); i++) {
                NumberRow n = numbers.get(i);
                n.validate();
                if (numberSeen[n.position - 1][n.number - 1])
                    throw new IllegalStateException("R36_NUMBER_DUPLICATE");
                numberSeen[n.position - 1][n.number - 1] = true;
                numberCount[n.position - 1]++;
            }
            for (int i = 0; i < 10; i++)
                if (numberCount[i] != 10)
                    throw new IllegalStateException(
                            "R36_POSITION_NUMBER_COUNT_" + (i + 1) + '_' + numberCount[i]);
            boolean[][][][] seen = new boolean[10][10][10][10];
            int[] count = new int[10];
            boolean[] rankSeen = new boolean[1201];
            StringBuilder actionPayload = new StringBuilder(1200 * 196);
            for (int i = 0; i < actions.size(); i++) {
                ActionRow a = actions.get(i);
                if (a == null)
                    throw new IllegalStateException("R366_SNAPSHOT_ACTION_ROW_NULL|index=" + i);
                try {
                    a.validate();
                } catch (RuntimeException invalid) {
                    throw new IllegalStateException(
                            "R366_SNAPSHOT_ACTION_ROW_INVALID|index="
                                    + i
                                    + "|position="
                                    + a.position
                                    + "|k3="
                                    + a.n1
                                    + ','
                                    + a.n2
                                    + ','
                                    + a.n3
                                    + "|n4="
                                    + a.n4
                                    + "|rank="
                                    + a.rank
                                    + "|cause="
                                    + safe(invalid.getMessage()),
                            invalid);
                }
                count[a.position - 1]++;
                int key1 = a.n1 - 1, key2 = a.n2 - 1, key3 = a.n3 - 1;
                if (seen[a.position - 1][key1][key2][key3])
                    throw new IllegalStateException("R366_ACTION_DUPLICATE|index=" + i);
                seen[a.position - 1][key1][key2][key3] = true;
                if (rankSeen[a.rank])
                    throw new IllegalStateException(
                            "R366_ACTION_RANK_DUPLICATE|index=" + i + "|rank=" + a.rank);
                rankSeen[a.rank] = true;
                actionPayload.append(
                        K3ActionSpaceEngine.canonicalPayloadRow(
                                a.position,
                                a.n1,
                                a.n2,
                                a.n3,
                                a.n4,
                                a.rawActionP,
                                a.rankScore,
                                a.rank,
                                a.fingerprint,
                                a.actionHash));
            }
            for (int i = 0; i < 10; i++)
                if (count[i] != 120)
                    throw new IllegalStateException(
                            "R36_POSITION_ACTION_COUNT_" + (i + 1) + '_' + count[i]);
            for (int rank = 1; rank <= 1200; rank++)
                if (!rankSeen[rank])
                    throw new IllegalStateException("R366_ACTION_RANK_MISSING|rank=" + rank);
            String recomputedActionSpaceHash =
                    K3ActionSpaceEngine.actionSpaceHashFromPayloadBody(
                            actionPayload.toString(), actions.size());
            if (!actionSpaceHash.equals(recomputedActionSpaceHash))
                throw new IllegalStateException(
                        "R366_ACTION_SPACE_HASH_MISMATCH|stored="
                                + actionSpaceHash
                                + "|recomputed="
                                + recomputedActionSpaceHash);
            String expectedRecord =
                    V51ChecksumEngine.sha256(
                            "R36_SNAPSHOT|"
                                    + targetIssue
                                    + '|'
                                    + historyHash
                                    + '|'
                                    + bundleHash
                                    + '|'
                                    + contractHash
                                    + '|'
                                    + actionSpaceHash);
            if (!recordHash.equals(expectedRecord))
                throw new IllegalStateException("R36_SNAPSHOT_RECORD_HASH_MISMATCH");
        }

        public Header header() {
            return new Header(
                    targetIssue,
                    baseIssue,
                    historyHash,
                    bundleHash,
                    contractHash,
                    actionSpaceHash,
                    recordHash);
        }

        /**
         * Rebuilds the exact immutable 1200-action surface for Research-only replay. The returned
         * array is indexed by the canonical global ordinal; it never invokes a model or reads a draw.
         */
        public V51Models.PositionAction[] positionActions() {
            V51Models.PositionAction[] out =
                    new V51Models.PositionAction[K3ActionSpaceEngine.TOTAL_ACTIONS];
            for (int i = 0; i < actions.size(); i++) {
                ActionRow row = actions.get(i);
                int ordinal = K3ActionSpaceEngine.globalOrdinal(
                        row.position, row.n1, row.n2, row.n3);
                if (ordinal < 0 || ordinal >= out.length || out[ordinal] != null)
                    throw new IllegalStateException("R914_SNAPSHOT_ACTION_ORDINAL_CONFLICT_" + ordinal);
                double[] meta = parseDoubles(row.metaBlob);
                out[ordinal] = new V51Models.PositionAction(
                        row.position,
                        row.n1,
                        row.n2,
                        row.n3,
                        row.n4,
                        row.rawActionP,
                        row.rankScore,
                        value(meta, 0),
                        value(meta, 1),
                        value(meta, 2),
                        value(meta, 3),
                        meta,
                        row.fingerprint,
                        row.rank,
                        row.actionHash);
            }
            for (int i = 0; i < out.length; i++)
                if (out[i] == null)
                    throw new IllegalStateException("R914_SNAPSHOT_ACTION_ORDINAL_MISSING_" + i);
            return out;
        }
    }

    public static final class NumberRow {
        public final int position, number, rank;
        public final double rawScore, probability;
        public final String stateId, domainId, expertHash;

        NumberRow(
                int p,
                int n,
                double raw,
                double probability,
                int rank,
                String state,
                String domain,
                String expert) {
            position = p;
            number = n;
            rawScore = raw;
            this.probability = probability;
            this.rank = rank;
            stateId = state;
            domainId = domain;
            expertHash = expert;
        }

        void validate() {
            if (position < 1
                    || position > 10
                    || number < 1
                    || number > 10
                    || rank < 1
                    || rank > 10
                    || Double.isNaN(rawScore)
                    || Double.isInfinite(rawScore)
                    || Double.isNaN(probability)
                    || Double.isInfinite(probability)
                    || probability < 0.0d
                    || probability > 1.0d
                    || expertHash.length() != 64)
                throw new IllegalStateException("R36_NUMBER_INVALID");
        }

        void write(DataOutputStream o) throws Exception {
            o.writeInt(position);
            o.writeInt(number);
            o.writeDouble(rawScore);
            o.writeDouble(probability);
            o.writeInt(rank);
            writeUtf(o, stateId);
            writeUtf(o, domainId);
            writeUtf(o, expertHash);
        }

        static NumberRow read(DataInputStream i) throws Exception {
            return new NumberRow(
                    i.readInt(),
                    i.readInt(),
                    i.readDouble(),
                    i.readDouble(),
                    i.readInt(),
                    readUtf(i),
                    readUtf(i),
                    readUtf(i));
        }
    }

    public static final class ActionRow {
        public final int position, n1, n2, n3, n4, rank;
        public final double rawActionP, rankScore, selectedHitP;
        public final String fingerprint, metaBlob, actionHash, recordHash;

        ActionRow(
                int p,
                int n1,
                int n2,
                int n3,
                int n4,
                double raw,
                double score,
                double selected,
                int rank,
                String fp,
                String meta,
                String hash,
                String record) {
            position = p;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.n4 = n4;
            rawActionP = raw;
            rankScore = score;
            selectedHitP = selected;
            this.rank = rank;
            fingerprint = fp;
            metaBlob = meta;
            actionHash = hash;
            recordHash = record;
        }

        void validate() {
            if (position < 1 || position > 10)
                throw new IllegalStateException("R366_ACTION_POSITION_INVALID|" + position);
            if (!K3ActionSpaceEngine.validTriple(n1, n2, n3))
                throw new IllegalStateException(
                        "R366_ACTION_K3_INVALID|" + n1 + ',' + n2 + ',' + n3);
            if (!K3ActionSpaceEngine.validFourthBoundary(n1, n2, n3, n4))
                throw new IllegalStateException(
                        "R366_ACTION_BOUNDARY_N4_INVALID|k3="
                                + n1
                                + ','
                                + n2
                                + ','
                                + n3
                                + "|n4="
                                + n4);
            if (rank < 1 || rank > 1200)
                throw new IllegalStateException("R366_ACTION_RANK_INVALID|" + rank);
            if (Double.isNaN(rawActionP)
                    || Double.isInfinite(rawActionP)
                    || rawActionP < 0.0d
                    || rawActionP > 1.0d)
                throw new IllegalStateException("R366_ACTION_RAW_P_INVALID|" + rawActionP);
            if (Double.isNaN(rankScore) || Double.isInfinite(rankScore))
                throw new IllegalStateException("R366_ACTION_RANK_SCORE_INVALID|" + rankScore);
            if (Double.isNaN(selectedHitP)
                    || Double.isInfinite(selectedHitP)
                    || (selectedHitP != -1.0d
                            && (selectedHitP < 0.0d || selectedHitP > 1.0d)))
                throw new IllegalStateException(
                        "R366_ACTION_SELECTED_HIT_P_INVALID|" + selectedHitP);
            if (fingerprint.length() != 64)
                throw new IllegalStateException(
                        "R366_ACTION_FINGERPRINT_INVALID|length=" + fingerprint.length());
            if (actionHash.length() != 64)
                throw new IllegalStateException(
                        "R366_ACTION_HASH_INVALID|length=" + actionHash.length());
            if (recordHash.length() != 64)
                throw new IllegalStateException(
                        "R366_ACTION_RECORD_HASH_INVALID|length=" + recordHash.length());
        }

        void write(DataOutputStream o) throws Exception {
            o.writeInt(position);
            o.writeInt(n1);
            o.writeInt(n2);
            o.writeInt(n3);
            o.writeInt(n4);
            o.writeDouble(rawActionP);
            o.writeDouble(rankScore);
            o.writeDouble(selectedHitP);
            o.writeInt(rank);
            writeUtf(o, fingerprint);
            writeUtf(o, metaBlob);
            writeUtf(o, actionHash);
            writeUtf(o, recordHash);
        }

        static ActionRow read(DataInputStream i) throws Exception {
            return new ActionRow(
                    i.readInt(),
                    i.readInt(),
                    i.readInt(),
                    i.readInt(),
                    i.readInt(),
                    i.readDouble(),
                    i.readDouble(),
                    i.readDouble(),
                    i.readInt(),
                    readUtf(i),
                    readUtf(i),
                    readUtf(i),
                    readUtf(i));
        }
    }

    private static double[] parseDoubles(String text) {
        String value = safe(text);
        if (value.length() == 0) return new double[0];
        String[] fields = value.split(",", -1);
        double[] out = new double[fields.length];
        for (int i = 0; i < fields.length; i++) {
            try {
                out[i] = finite(Double.parseDouble(fields[i]), 0.0d);
            } catch (Throwable ignored) {
                out[i] = 0.0d;
            }
        }
        return out;
    }

    private static double value(double[] values, int index) {
        return values != null && index >= 0 && index < values.length
                ? finite(values[index], 0.0d)
                : 0.0d;
    }

    private static int[] rankRow(double[] p) {
        int[] rank = new int[10];
        for (int n = 0; n < 10; n++) {
            int r = 1;
            for (int m = 0; m < 10; m++) if (p[m] > p[n] || (p[m] == p[n] && m < n)) r++;
            rank[n] = r;
        }
        return rank;
    }

    private static String expertHash(V51Models.ExpertEvidence[] x) {
        StringBuilder b = new StringBuilder();
        if (x != null) for (int i = 0; i < x.length; i++) if (x[i] != null) b.append(x[i].checksum);
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static String doubles(double[] x) {
        StringBuilder b = new StringBuilder();
        if (x != null)
            for (int i = 0; i < x.length; i++) {
                if (i > 0) b.append(',');
                b.append(Double.toString(finite(x[i], 0.0d)));
            }
        return b.toString();
    }

    private static String ints(int[] x) {
        StringBuilder b = new StringBuilder();
        if (x != null)
            for (int i = 0; i < x.length; i++) {
                if (i > 0) b.append(',');
                b.append(x[i]);
            }
        return b.toString();
    }

    private static String matrix(double[][] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            if (i > 0) b.append(';');
            b.append(doubles(x[i]));
        }
        return b.toString();
    }

    private static double finite(double v, double fallback) {
        return Double.isNaN(v) || Double.isInfinite(v) ? fallback : v;
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }
}
