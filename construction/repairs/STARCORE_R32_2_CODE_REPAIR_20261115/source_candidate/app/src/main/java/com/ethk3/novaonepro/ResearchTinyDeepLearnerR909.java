package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * R909 bounded online deep-learning challenger.
 *
 * <p>Architecture is deliberately small enough for an Android Research sidecar: 7 -> 16 -> 8 -> 1.
 * Training consumes only already-settled, pre-draw-frozen compact labels. An artifact trained
 * through issue t may only score t+1 or later. This class is Research-only and owns no Production
 * decision, REAL authorization or Production settlement path.
 *
 * <p>B09 binds each new neural artifact and LEARNED_EXACTLY_ONCE evidence in the same SQLite
 * transaction. A crash therefore cannot leave model state ahead of the immutable learning ledger or
 * the ledger ahead of model state.</p>
 */
public final class ResearchTinyDeepLearnerR909 {
    public static final String OWNER = "R909_RESEARCH_TINY_DEEP_ONLY";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    public static final String ARTIFACT_TYPE = "R909_TINY_DEEP_7x16x8x1";
    public static final String CANDIDATE_ID = "R909-TINY-DEEP-CHALLENGER";
    private static final String UPDATE_CONTRACT = "R909_TINY_DEEP_COMPACT_LABEL_UPDATE_V2";
    private static final String LABEL_VERSION = "R906_PRODUCTION_SELECTED_COMPACT_LABEL_V1";
    private static final int INPUT = 7, H1 = 16, H2 = 8;

    private ResearchTinyDeepLearnerR909() {}

    /** Advances exactly one chronological settled label. Never backfills into an earlier artifact. */
    public static Result updateOne(final DbStore store) {
        if(store==null)return Result.fail("STORE_NULL");
        // CP12: AST3 DevelopmentProduction/Director is the only active numerical owner.
        // Historical codecs and frozen shadow capture remain readable; no legacy SGD/artifact write.
        return Result.fail("RETIRED_CP12_MODERN_TRAINING_OWNER");
    }

    /** Freezes one future prediction from the latest strictly-prior neural artifact. */
    public static String capturePredraw(final DbStore store, final V51RuntimeResult runtime) {
        if (store == null
                || runtime == null
                || !runtime.success
                || runtime.context == null
                || runtime.actions == null
                || runtime.actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return "INPUT_INVALID";
        return captureSurface(
                store,
                runtime.context.targetIssue,
                runtime.context.baseIssue,
                runtime.context.historyHash,
                runtime.checksum,
                runtime.actions);
    }

    /** Crash-durable neural scoring from the immutable canonical action surface. */
    public static String capturePredraw(
            final DbStore store, final DeferredEvidenceSnapshotR36.Snapshot snapshot) {
        if (store == null || snapshot == null) return "R914_SNAPSHOT_INPUT_INVALID";
        return captureSurface(
                store,
                snapshot.targetIssue,
                snapshot.baseIssue,
                snapshot.historyHash,
                snapshot.recordHash,
                snapshot.positionActions());
    }

    private static String captureSurface(
            final DbStore store,
            final String targetIssue,
            final String baseIssue,
            final String historyHash,
            final String surfaceHash,
            final V51Models.PositionAction[] actions) {
        final String target = safe(targetIssue);
        if (store == null
                || !TargetIssueResolver.exact(safe(baseIssue), target).valid
                || safe(historyHash).length() != 64
                || safe(surfaceHash).length() != 64
                || actions == null
                || actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return "R914_SURFACE_INPUT_INVALID";
        if (AppConfig.R36_REAL_BET_UNLOCK || MAY_AUTHORIZE_REAL)
            throw new IllegalStateException("R909_TINY_DEEP_REAL_BOUNDARY");
        if (store.getDrawByIssue(target) != null) return "TARGET_ALREADY_DRAWN";
        GlobalResearchScheduleR932.prepareConsumer(store,target,surfaceHash);
        try{GlobalResearchScheduleR932.proxy(store,target,"tiny:"+CANDIDATE_ID);}catch(IllegalStateException unscheduled){return "GLOBAL_ROSTER_NOT_SELECTED";}
        String existing = existingPredrawHash(store, target);
        if (existing.length() == 64) {
            final String existingHash=existing;
            boolean reconciled=store.coordinatorR36().runAtomicWrite(
                    "R952_TINY_DEEP_CAPTURE_LEDGER_RECONCILE",
                    new DatabaseCoordinator.WriteCallable<Boolean>() {
                        @Override public Boolean call(SQLiteDatabase db) {
                            if (drawExists(db,target)) return Boolean.FALSE;
                            ResearchCaptureLedgerR932.record(db,target,"tiny:"+CANDIDATE_ID,"FROZEN",
                                    "R952_RECONCILED_EXISTING_FULL1200",existingHash,1L,true);
                            return Boolean.TRUE;
                        }
                    }, Boolean.FALSE);
            if(!reconciled)return "CAPTURE_LEDGER_RECONCILE_FAILED";
            setState(store, "r909_tiny_deep_predraw_status", "ALREADY_FROZEN_FIRST_WRITE_LEDGER_RECONCILED");
            setState(store, "r909_tiny_deep_predraw_target", target);
            return "ALREADY_FROZEN_FIRST_WRITE_LEDGER_RECONCILED";
        }
        ArtifactRow row = loadLatest(store, target);
        if (row == null) return "NO_PRIOR_ARTIFACT";
        ResearchCaptureLedgerR932.record(store,target,"tiny:"+CANDIDATE_ID,"EXECUTION_STARTED","FULL1200",0L);
        final long scoringStarted=TimeSourceR811.monoNowMs();
        final Network network = Network.from(row.artifact);
        V51Models.PositionAction best = null;
        int bestOrdinal = -1;
        double bestP = -1.0d;
        for (int i = 0; i < actions.length; i++) {
            ResearchWorkBudgetR927.checkpoint();
            V51Models.PositionAction a = actions[i];
            if (a == null) continue;
            int ord = K3ActionSpaceEngine.globalOrdinal(a.position, a.n1, a.n2, a.n3);
            if (ord != i) return "ACTION_ORDINAL_MISMATCH_" + i;
            double p = network.predict(ResearchCompiledGraphRuntimeR906.canonicalVector(a));
            if (best == null || p > bestP || (p == bestP && ord < bestOrdinal)) {
                best = a;
                bestOrdinal = ord;
                bestP = p;
            }
        }
        if (best == null) return "NO_SELECTION";
        final V51Models.PositionAction selected = best;
        final int ordinal = bestOrdinal;
        final double probability = clamp01(bestP);
        final long scoringCostMs=Math.max(1L,TimeSourceR811.monoNowMs()-scoringStarted);
        final String artifactHash = row.hash;
        final String trainedUntil = row.artifact.trainedUntilIssue;
        final long frozenAt = System.currentTimeMillis();
        final String inputHash =
                V51ChecksumEngine.sha256(
                        "R909_TINY_DEEP_INPUT|"
                                + target
                                + '|'
                                + safe(historyHash)
                                + '|'
                                + safe(surfaceHash)
                                + '|'
                                + artifactHash
                                + '|'
                                + trainedUntil);
        final String recordHash =
                V51ChecksumEngine.sha256(
                        "R909_TINY_DEEP_PREDRAW|"
                                + CANDIDATE_ID
                                + '|'
                                + target
                                + '|'
                                + ordinal
                                + '|'
                                + Double.toString(probability)
                                + '|'
                                + artifactHash
                                + '|'
                                + inputHash);
        Boolean ok =
                store.coordinatorR36()
                        .runAtomicWrite(
                                "R952_RESEARCH_TINY_DEEP_PREDRAW_ATOMIC",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        if (drawExists(db, target)) return Boolean.FALSE;
                                        ContentValues v = new ContentValues();
                                        v.put("candidate_id", CANDIDATE_ID);
                                        v.put("target_issue", target);
                                        v.put("phase", "R909_TINY_DEEP_FUTURE_SHADOW");
                                        v.put("position", selected.position);
                                        v.put("n1", selected.n1);
                                        v.put("n2", selected.n2);
                                        v.put("n3", selected.n3);
                                        v.put("selected_ordinal", ordinal);
                                        v.put("score", probability);
                                        v.put("probability", probability);
                                        v.put("formula_probability", probability);
                                        v.put("adaptive_probability", -1.0d);
                                        v.put("adaptive_artifact_hash", artifactHash);
                                        v.put("adaptive_trained_until", trainedUntil);
                                        v.put("adaptive_updates", network.updates);
                                        v.put("adaptive_weight", 1.0d);
                                        v.put("formula_hash", artifactHash);
                                        v.put("input_hash", inputHash);
                                        v.put("frozen_at", frozenAt);
                                        v.put("record_hash", recordHash);
                                        long n =
                                                db.insertWithOnConflict(
                                                        DbHelper.TABLE_R817_RESEARCH_PREDRAW,
                                                        null,
                                                        v,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        boolean durable=n>0L;
                                        if(!durable){
                                            Cursor c = null;
                                            try {
                                                c =
                                                        db.rawQuery(
                                                                "SELECT record_hash FROM "
                                                                        + DbHelper
                                                                                .TABLE_R817_RESEARCH_PREDRAW
                                                                        + " WHERE candidate_id=? AND target_issue=? LIMIT 1",
                                                                new String[] {CANDIDATE_ID, target});
                                                durable=c.moveToFirst() && recordHash.equals(safe(c.getString(0)));
                                            } finally {
                                                if (c != null) c.close();
                                            }
                                        }
                                        if(!durable)return Boolean.FALSE;
                                        ResearchCaptureLedgerR932.record(db,target,"tiny:"+CANDIDATE_ID,"FROZEN",
                                                "RAW_SCORE_NOT_CALIBRATED",recordHash,scoringCostMs,true);
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        setState(
                store,
                "r909_tiny_deep_predraw_status",
                ok.booleanValue() ? "FROZEN_SHADOW_ONLY" : "WRITE_FAILED");
        setState(store, "r909_tiny_deep_predraw_target", target);
        return ok.booleanValue() ? "FROZEN_SHADOW_ONLY" : "WRITE_FAILED";
    }

    private static ArtifactRow loadLatest(DbStore store, String targetExclusive) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT artifact_blob,artifact_hash,trained_until_issue FROM v57_research_artifact_r811"
                                            + " WHERE artifact_type=? AND trained_until_issue<? AND status='SHADOW_RESEARCH_ONLY'"
                                            + " ORDER BY trained_until_issue DESC,created_at DESC LIMIT 1",
                                    new String[] {ARTIFACT_TYPE, safe(targetExclusive)});
            if (!c.moveToFirst()) return null;
            byte[] blob = c.getBlob(0);
            String hash = safe(c.getString(1));
            if (!ResearchArtifactCodecR811.verify(blob, hash)) throw new IllegalStateException("R948_NEURAL_ARTIFACT_HASH_INVALID");
            ResearchArtifactCodecR811.Artifact a = ResearchArtifactCodecR811.decode(blob);
            if (!ARTIFACT_TYPE.equals(a.type)) throw new IllegalStateException("R948_NEURAL_ARTIFACT_TYPE_INVALID");
            return new ArtifactRow(a, hash);
        } catch (ResearchWorkBudgetR927.Yield yielded) {
            throw yielded;
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R948_NEURAL_ARTIFACT_UNREADABLE",failure);
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean artifactMatches(SQLiteDatabase db, String artifactId, String hash) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT artifact_hash,artifact_type,trained_until_issue,status FROM v57_research_artifact_r811 WHERE artifact_id=? LIMIT 1",
                            new String[] {artifactId});
            return c.moveToFirst()
                    && hash.equals(safe(c.getString(0)))
                    && ARTIFACT_TYPE.equals(safe(c.getString(1)))
                    && safe(c.getString(2)).length() > 0
                    && "SHADOW_RESEARCH_ONLY".equals(safe(c.getString(3)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static String existingPredrawHash(DbStore store, String target) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT record_hash FROM "
                                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                            + " WHERE candidate_id=? AND target_issue=? LIMIT 1",
                                    new String[] {CANDIDATE_ID, target});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (Throwable ignored) {
            return "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static Label nextLabel(DbStore store, String trainedUntil) {
        Cursor c = null;
        // R943: old decoder swallowed cooperative Yield as a malformed feature and advanced
        // this cursor. Revisit untrained rows once; never rewind the committed model itself.
        if (!"1".equals(store.getState("r943_tiny_cursor_repaired", ""))) {
            java.util.LinkedHashMap<String,String> repair = new java.util.LinkedHashMap<String,String>();
            repair.put("r924_tiny_deep_invalid_cursor", "");
            repair.put("r943_tiny_cursor_repaired", "1");
            store.setStates(repair);
        }
        String skipped = store.getState("r924_tiny_deep_invalid_cursor", "");
        String after =
                skipped.compareTo(safe(trainedUntil)) > 0 ? skipped : safe(trainedUntil);
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT target_issue,feature_blob,hit_label FROM "
                                            + DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL
                                            + " WHERE asset_id='PRODUCTION_SELECTED' AND target_issue>? AND hit_label IN (0,1)"
                                            + " ORDER BY target_issue ASC LIMIT 128",
                                    new String[] {after});
            int invalid = 0;
            while (c.moveToNext()) {
            ResearchWorkBudgetR927.checkpoint();
                String issue = safe(c.getString(0));
                double[] x = decodeFeatures(c.getBlob(1));
                if (!TargetIssueResolver.isValid(issue) || x == null) {
                    invalid++;
                    store.setState("r924_tiny_deep_invalid_cursor", issue);
                    continue;
                }
                if (invalid > 0)
                    store.setState(
                            "r924_tiny_deep_invalid_rows_skipped", String.valueOf(invalid));
                return new Label(issue, x, c.getInt(2));
            }
            if (invalid > 0)
                store.setState("r924_tiny_deep_invalid_rows_skipped", String.valueOf(invalid));
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String learningInputHash(Label label) {
        StringBuilder b =
                new StringBuilder("R909_TINY_DEEP_LEARNING_INPUT|")
                        .append(safe(label == null ? "" : label.issue))
                        .append('|')
                        .append(label == null ? -1 : label.y);
        if (label != null && label.x != null)
            for (double v : label.x) b.append('|').append(Double.toString(finite(v)));
        return V51ChecksumEngine.sha256(b.toString());
    }

    static double[] decodeFeatures(byte[] blob) {
        if (blob == null) return null;
        try {
            String[] parts = new String(blob, StandardCharsets.UTF_8).split(",", -1);
            if (parts.length != INPUT) return null;
            double[] x = new double[INPUT];
            for (int i = 0; i < INPUT; i++) {
            ResearchWorkBudgetR927.checkpoint();
                x[i] = Double.parseDouble(parts[i]);
                if (Double.isNaN(x[i]) || Double.isInfinite(x[i])) return null;
            }
            return x;
        } catch (ResearchWorkBudgetR927.Yield yielded) {
            throw yielded;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    static final class Network {
        final double[] w1 = new double[INPUT * H1],
                b1 = new double[H1],
                w2 = new double[H1 * H2],
                b2 = new double[H2],
                w3 = new double[H2],
                b3 = new double[1];
        long updates;
        double lossEma;

        static Network initial() {
            Network n = new Network();
            Random r = new Random(0x5239303954494e59L);
            double s1 = Math.sqrt(6.0d / (INPUT + H1)),
                    s2 = Math.sqrt(6.0d / (H1 + H2)),
                    s3 = Math.sqrt(6.0d / (H2 + 1));
            fill(n.w1, r, s1);
            fill(n.w2, r, s2);
            fill(n.w3, r, s3);
            return n;
        }

        static void fill(double[] a, Random r, double scale) {
            for (int i = 0; i < a.length; i++) a[i] = (r.nextDouble() * 2.0d - 1.0d) * scale;
        }

        static Network from(ResearchArtifactCodecR811.Artifact a) {
            Network n = new Network();
            copy(a, "w1", n.w1);
            copy(a, "b1", n.b1);
            copy(a, "w2", n.w2);
            copy(a, "b2", n.b2);
            copy(a, "w3", n.w3);
            copy(a, "b3", n.b3);
            double[] m = a.fields.get("meta");
            if (m != null && m.length >= 2) {
                n.updates = Math.max(0L, (long) m[0]);
                n.lossEma = finite(m[1]);
            }
            return n;
        }

        ResearchArtifactCodecR811.Artifact toArtifact(String issue) {
            LinkedHashMap<String, double[]> f = new LinkedHashMap<String, double[]>();
            f.put("b1", b1.clone());
            f.put("b2", b2.clone());
            f.put("b3", b3.clone());
            f.put("meta", new double[] {updates, lossEma});
            f.put("w1", w1.clone());
            f.put("w2", w2.clone());
            f.put("w3", w3.clone());
            return new ResearchArtifactCodecR811.Artifact(ARTIFACT_TYPE, issue, f);
        }

        double predict(double[] raw) {
            double[] x = normalize(raw), h1 = new double[H1], h2 = new double[H2];
            forward(x, h1, h2);
            double z = b3[0];
            for (int j = 0; j < H2; j++) z += w3[j] * h2[j];
            return sigmoid(z);
        }

        double update(double[] raw, int y) {
            double[] x = normalize(raw), h1 = new double[H1], h2 = new double[H2];
            forward(x, h1, h2);
            double z = b3[0];
            for (int j = 0; j < H2; j++) z += w3[j] * h2[j];
            double p = sigmoid(z);
            double dz = clip(p - y, -5, 5);
            double lr = 0.025d / Math.sqrt(1.0d + updates / 50.0d), l2 = 1e-4;
            double[] dh2 = new double[H2];
            for (int j = 0; j < H2; j++) dh2[j] = dz * w3[j] * (1 - h2[j] * h2[j]);
            double[] dh1 = new double[H1];
            for (int i = 0; i < H1; i++) {
            ResearchWorkBudgetR927.checkpoint();
                double ss = 0;
                for (int j = 0; j < H2; j++) ss += dh2[j] * w2[i * H2 + j];
                dh1[i] = ss * (1 - h1[i] * h1[i]);
            }
            for (int j = 0; j < H2; j++)
                w3[j] -= lr * clip(dz * h2[j] + l2 * w3[j], -5, 5);
            b3[0] -= lr * dz;
            for (int i = 0; i < H1; i++)
                for (int j = 0; j < H2; j++) {
            ResearchWorkBudgetR927.checkpoint();
                    int k = i * H2 + j;
                    w2[k] -= lr * clip(dh2[j] * h1[i] + l2 * w2[k], -5, 5);
                }
            for (int j = 0; j < H2; j++) b2[j] -= lr * clip(dh2[j], -5, 5);
            for (int k = 0; k < INPUT; k++)
                for (int i = 0; i < H1; i++) {
            ResearchWorkBudgetR927.checkpoint();
                    int q = k * H1 + i;
                    w1[q] -= lr * clip(dh1[i] * x[k] + l2 * w1[q], -5, 5);
                }
            for (int i = 0; i < H1; i++) b1[i] -= lr * clip(dh1[i], -5, 5);
            updates++;
            double loss =
                    -(y * Math.log(Math.max(1e-9, p))
                            + (1 - y) * Math.log(Math.max(1e-9, 1 - p)));
            lossEma = updates == 1 ? loss : 0.97d * lossEma + 0.03d * loss;
            return loss;
        }

        void forward(double[] x, double[] h1, double[] h2) {
            for (int i = 0; i < H1; i++) {
            ResearchWorkBudgetR927.checkpoint();
                double z = b1[i];
                for (int k = 0; k < INPUT; k++) z += w1[k * H1 + i] * x[k];
                h1[i] = Math.tanh(z);
            }
            for (int j = 0; j < H2; j++) {
            ResearchWorkBudgetR927.checkpoint();
                double z = b2[j];
                for (int i = 0; i < H1; i++) z += w2[i * H2 + j] * h1[i];
                h2[j] = Math.tanh(z);
            }
        }

        static void copy(ResearchArtifactCodecR811.Artifact a, String key, double[] dst) {
            double[] ss = a.fields.get(key);
            if (ss == null || ss.length != dst.length)
                throw new IllegalStateException("R909_TINY_DEEP_ARTIFACT_" + key);
            System.arraycopy(ss, 0, dst, 0, dst.length);
        }
    }

    private static double[] normalize(double[] raw) {
        double[] x = new double[INPUT];
        for (int i = 0; i < INPUT; i++) {
            ResearchWorkBudgetR927.checkpoint();
            double v = raw != null && i < raw.length ? finite(raw[i]) : 0.0d;
            if (i == 0 || i >= 3) x[i] = clamp01(v) * 2 - 1;
            else if (i == 2) x[i] = Math.tanh(v * 10.0d);
            else x[i] = Math.tanh(v / 4.0d);
        }
        return x;
    }

    private static double sigmoid(double z) {
        if (z >= 0) {
            double e = Math.exp(-Math.min(50, z));
            return 1 / (1 + e);
        }
        double e = Math.exp(Math.max(-50, z));
        return e / (1 + e);
    }

    private static boolean drawExists(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT 1 FROM " + DbHelper.TABLE_DRAWS + " WHERE issue=? LIMIT 1",
                            new String[] {target});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    private static double clamp01(double x) {
        return clip(finite(x), 0, 1);
    }

    private static double clip(double x, double lo, double hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }

    private static double finite(double x) {
        return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x;
    }

    private static void setState(DbStore s, String k, String v) {
        try {
            s.setState(k, v);
        } catch (Throwable ignore) {
        }
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

    private static final class ArtifactRow {
        final ResearchArtifactCodecR811.Artifact artifact;
        final String hash;

        ArtifactRow(ResearchArtifactCodecR811.Artifact a, String h) {
            artifact = a;
            hash = h;
        }
    }

    private static final class Label {
        final String issue;
        final double[] x;
        final int y;

        Label(String i, double[] x, int y) {
            issue = i;
            this.x = x;
            this.y = y;
        }
    }

    public static final class Result {
        public final boolean success;
        public final String status, trainedUntil, artifactHash;
        public final long updates;
        public final double loss;

        Result(boolean s, String st, String t, String h, long u, double l) {
            success = s;
            status = st;
            trainedUntil = t;
            artifactHash = h;
            updates = u;
            loss = l;
        }

        static Result fail(String s) {
            return new Result(false, s, "", "", 0, -1);
        }
    }
}
