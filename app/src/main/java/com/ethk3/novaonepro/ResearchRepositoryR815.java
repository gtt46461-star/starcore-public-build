package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded durable lineage for shadow-only research candidates, evaluations and artifacts. */
public final class ResearchRepositoryR815 {
    public enum State {
        NEW,
        QUEUED,
        RUNNING,
        EVALUATED,
        REJECTED,
        ELIGIBLE_FOR_SHADOW_PROMOTION,
        FROZEN_CONFLICT
    }

    public static final class Candidate {
        public final String candidateId;
        public final String candidateType;
        public final String genomeSha256;
        public final String schemaSha256;
        public final String datasetSha256;
        public final String createdTargetIssue;
        public final State state;
        public final long createdAt;

        public Candidate(
                String candidateId,
                String candidateType,
                String genomeSha256,
                String schemaSha256,
                String datasetSha256,
                String createdTargetIssue,
                State state,
                long createdAt) {
            this.candidateId = safe(candidateId);
            this.candidateType = safe(candidateType);
            this.genomeSha256 = safe(genomeSha256);
            this.schemaSha256 = safe(schemaSha256);
            this.datasetSha256 = safe(datasetSha256);
            this.createdTargetIssue = safe(createdTargetIssue);
            this.state = state == null ? State.NEW : state;
            this.createdAt = createdAt > 0L ? createdAt : System.currentTimeMillis();
            validate();
        }

        void validate() {
            if (candidateId.length() < 8
                    || candidateId.length() > 128
                    || candidateType.length() == 0
                    || !hash(genomeSha256)
                    || !ResearchFeatureSchemaR815.schemaSha256().equals(schemaSha256)
                    || !hash(datasetSha256)
                    || !TargetIssueResolver.isValid(createdTargetIssue))
                throw new IllegalArgumentException("R815_RESEARCH_CANDIDATE_INVALID");
        }
    }

    public static final class Evaluation {
        public final String candidateId;
        public final String targetIssue;
        public final String artifactSha256;
        public final boolean selected;
        public final boolean hit;
        public final double selectedP;
        public final double brier;
        public final double logLoss;
        public final String blockId;
        public final String regimeId;
        public final long settledAt;
        public final String recordHash;

        public Evaluation(
                String candidateId,
                String targetIssue,
                String artifactSha256,
                boolean selected,
                boolean hit,
                double selectedP,
                double brier,
                double logLoss,
                String blockId,
                String regimeId,
                long settledAt) {
            this.candidateId = safe(candidateId);
            this.targetIssue = safe(targetIssue);
            this.artifactSha256 = safe(artifactSha256);
            this.selected = selected;
            this.hit = hit;
            this.selectedP = finite(selectedP);
            this.brier = finite(brier);
            this.logLoss = finite(logLoss);
            this.blockId = safe(blockId);
            this.regimeId = safe(regimeId);
            this.settledAt = settledAt > 0L ? settledAt : System.currentTimeMillis();
            if (this.candidateId.length() == 0
                    || !TargetIssueResolver.isValid(this.targetIssue)
                    || !hash(this.artifactSha256)
                    || this.blockId.length() == 0
                    || this.regimeId.length() == 0)
                throw new IllegalArgumentException("R815_RESEARCH_EVAL_INVALID");
            recordHash =
                    V51ChecksumEngine.sha256(
                            "R815_EVAL|"
                                    + this.candidateId
                                    + '|'
                                    + this.targetIssue
                                    + '|'
                                    + this.artifactSha256
                                    + '|'
                                    + (selected ? 1 : 0)
                                    + '|'
                                    + (hit ? 1 : 0)
                                    + '|'
                                    + this.selectedP
                                    + '|'
                                    + this.brier
                                    + '|'
                                    + this.logLoss
                                    + '|'
                                    + this.blockId
                                    + '|'
                                    + this.regimeId);
        }
    }

    public static final class Artifact {
        public final String artifactId;
        public final String candidateId;
        public final String trainedUntilIssue;
        public final String schemaSha256;
        public final String datasetSha256;
        public final String artifactSha256;
        public final String status;
        public final long createdAt;

        public Artifact(
                String artifactId,
                String candidateId,
                String trainedUntilIssue,
                String schemaSha256,
                String datasetSha256,
                String artifactSha256,
                String status,
                long createdAt) {
            this.artifactId = safe(artifactId);
            this.candidateId = safe(candidateId);
            this.trainedUntilIssue = safe(trainedUntilIssue);
            this.schemaSha256 = safe(schemaSha256);
            this.datasetSha256 = safe(datasetSha256);
            this.artifactSha256 = safe(artifactSha256);
            this.status = safe(status);
            this.createdAt = createdAt > 0L ? createdAt : System.currentTimeMillis();
            if (artifactId.length() < 8
                    || candidateId.length() == 0
                    || !TargetIssueResolver.isValid(trainedUntilIssue)
                    || !ResearchFeatureSchemaR815.schemaSha256().equals(schemaSha256)
                    || !hash(datasetSha256)
                    || !hash(artifactSha256)
                    || status.length() == 0)
                throw new IllegalArgumentException("R815_RESEARCH_ARTIFACT_INVALID");
        }
    }

    public static final class Snapshot {
        public final List<Candidate> candidates;
        public final int evaluationCount;
        public final int artifactCount;
        public final String checksum;

        Snapshot(List<Candidate> candidates, int evaluationCount, int artifactCount) {
            this.candidates = Collections.unmodifiableList(new ArrayList<Candidate>(candidates));
            this.evaluationCount = Math.max(0, evaluationCount);
            this.artifactCount = Math.max(0, artifactCount);
            StringBuilder canonical = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                canonical.append('|').append(c.candidateId).append(':').append(c.state.name());
            }
            checksum = V51ChecksumEngine.sha256(canonical + "|" + evaluationCount + "|" + artifactCount);
        }
    }

    private final DbStore store;

    public ResearchRepositoryR815(DbStore store) {
        if (store == null) throw new IllegalArgumentException("R815_RESEARCH_STORE_NULL");
        this.store = store;
    }

    public boolean insertCandidate(final Candidate candidate) {
        if (candidate == null) return false;
        Boolean result =
                store.coordinatorR36()
                        .runWrite(
                                "R815_RESEARCH_CANDIDATE_INSERT",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        ContentValues values = candidateValues(candidate, candidate.createdAt);
                                        long inserted =
                                                db.insertWithOnConflict(
                                                        DbHelper.TABLE_R815_RESEARCH_CANDIDATE,
                                                        null,
                                                        values,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        if (inserted >= 0L) return Boolean.TRUE;
                                        Cursor cursor = null;
                                        try {
                                            cursor =
                                                    db.rawQuery(
                                                            "SELECT genome_sha256,schema_sha256,dataset_sha256,game_code,created_target_issue FROM "
                                                                    + DbHelper.TABLE_R815_RESEARCH_CANDIDATE
                                                                    + " WHERE candidate_id=? LIMIT 1",
                                                            new String[] {candidate.candidateId});
                                            return Boolean.valueOf(
                                                    cursor.moveToFirst()
                                                            && candidate.genomeSha256.equals(safe(cursor.getString(0)))
                                                            && candidate.schemaSha256.equals(safe(cursor.getString(1)))
                                                            && candidate.datasetSha256.equals(safe(cursor.getString(2)))
                                                            && ReleaseIdentityR815.GAME_CODE.equals(safe(cursor.getString(3)))
                                                            && candidate.createdTargetIssue.equals(safe(cursor.getString(4))));
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                    }
                                },
                                Boolean.FALSE);
        return result != null && result.booleanValue();
    }

    public boolean transition(final String candidateId, final State to, final String detail) {
        if (safe(candidateId).length() == 0 || to == null) return false;
        Boolean result =
                store.coordinatorR36()
                        .runWrite(
                                "R815_RESEARCH_CANDIDATE_TRANSITION",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        Cursor cursor = null;
                                        try {
                                            cursor =
                                                    db.rawQuery(
                                                            "SELECT state FROM "
                                                                    + DbHelper.TABLE_R815_RESEARCH_CANDIDATE
                                                                    + " WHERE candidate_id=? LIMIT 1",
                                                            new String[] {candidateId});
                                            if (!cursor.moveToFirst()) return Boolean.FALSE;
                                            State from = parseState(cursor.getString(0));
                                            if (!allowed(from, to)) return Boolean.FALSE;
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                        ContentValues values = new ContentValues();
                                        values.put("state", to.name());
                                        values.put("updated_at", System.currentTimeMillis());
                                        values.put("detail", bounded(detail, 512));
                                        return Boolean.valueOf(
                                                db.update(
                                                                DbHelper.TABLE_R815_RESEARCH_CANDIDATE,
                                                                values,
                                                                "candidate_id=?",
                                                                new String[] {candidateId})
                                                        == 1);
                                    }
                                },
                                Boolean.FALSE);
        return result != null && result.booleanValue();
    }

    public boolean saveEval(final Evaluation evaluation) {
        if (evaluation == null) return false;
        Boolean result =
                store.coordinatorR36()
                        .runWrite(
                                "R815_RESEARCH_EVAL_SAVE",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        ContentValues values = new ContentValues();
                                        values.put("candidate_id", evaluation.candidateId);
                                        values.put("target_issue", evaluation.targetIssue);
                                        values.put("artifact_sha256", evaluation.artifactSha256);
                                        values.put("selected", evaluation.selected ? 1 : 0);
                                        values.put("hit", evaluation.hit ? 1 : 0);
                                        values.put("selected_p", evaluation.selectedP);
                                        values.put("brier", evaluation.brier);
                                        values.put("log_loss", evaluation.logLoss);
                                        values.put("block_id", evaluation.blockId);
                                        values.put("regime_id", evaluation.regimeId);
                                        values.put("settled_at", evaluation.settledAt);
                                        values.put("record_hash", evaluation.recordHash);
                                        long inserted =
                                                db.insertWithOnConflict(
                                                        DbHelper.TABLE_R815_RESEARCH_EVAL,
                                                        null,
                                                        values,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        if (inserted >= 0L) return Boolean.TRUE;
                                        Cursor cursor = null;
                                        try {
                                            cursor =
                                                    db.rawQuery(
                                                            "SELECT record_hash FROM "
                                                                    + DbHelper.TABLE_R815_RESEARCH_EVAL
                                                                    + " WHERE candidate_id=? AND target_issue=? LIMIT 1",
                                                            new String[] {evaluation.candidateId, evaluation.targetIssue});
                                            return Boolean.valueOf(
                                                    cursor.moveToFirst()
                                                            && evaluation.recordHash.equals(safe(cursor.getString(0))));
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                    }
                                },
                                Boolean.FALSE);
        return result != null && result.booleanValue();
    }

    public boolean saveArtifact(final Artifact artifact) {
        if (artifact == null) return false;
        Boolean result =
                store.coordinatorR36()
                        .runWrite(
                                "R815_RESEARCH_ARTIFACT_SAVE",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        ContentValues values = new ContentValues();
                                        values.put("artifact_id", artifact.artifactId);
                                        values.put("candidate_id", artifact.candidateId);
                                        values.put("trained_until_issue", artifact.trainedUntilIssue);
                                        values.put("schema_sha256", artifact.schemaSha256);
                                        values.put("dataset_sha256", artifact.datasetSha256);
                                        values.put("artifact_sha256", artifact.artifactSha256);
                                        values.put("status", artifact.status);
                                        values.put("created_at", artifact.createdAt);
                                        values.put("detail", "");
                                        long inserted =
                                                db.insertWithOnConflict(
                                                        DbHelper.TABLE_R815_RESEARCH_ARTIFACT,
                                                        null,
                                                        values,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        if (inserted >= 0L) return Boolean.TRUE;
                                        Cursor cursor = null;
                                        try {
                                            cursor =
                                                    db.rawQuery(
                                                            "SELECT artifact_sha256 FROM "
                                                                    + DbHelper.TABLE_R815_RESEARCH_ARTIFACT
                                                                    + " WHERE artifact_id=? LIMIT 1",
                                                            new String[] {artifact.artifactId});
                                            return Boolean.valueOf(
                                                    cursor.moveToFirst()
                                                            && artifact.artifactSha256.equals(safe(cursor.getString(0))));
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                    }
                                },
                                Boolean.FALSE);
        return result != null && result.booleanValue();
    }

    public int pruneRejected(final long olderThan) {
        Integer result =
                store.coordinatorR36()
                        .runWrite(
                                "R815_RESEARCH_PRUNE_REJECTED",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        return Integer.valueOf(
                                                db.delete(
                                                        DbHelper.TABLE_R815_RESEARCH_CANDIDATE,
                                                        "state=? AND updated_at<? AND candidate_id NOT IN (SELECT candidate_id FROM "
                                                                + DbHelper.TABLE_R815_RESEARCH_ARTIFACT
                                                                + ")",
                                                        new String[] {State.REJECTED.name(), String.valueOf(olderThan)}));
                                    }
                                },
                                Integer.valueOf(0));
        return result == null ? 0 : Math.max(0, result.intValue());
    }

    public Snapshot snapshot(int limit) {
        int capped = Math.max(1, Math.min(64, limit));
        ArrayList<Candidate> candidates = new ArrayList<Candidate>();
        Cursor cursor = null;
        int evaluations = 0;
        int artifacts = 0;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT candidate_id,candidate_type,genome_sha256,schema_sha256,dataset_sha256,created_target_issue,state,created_at"
                                            + " FROM "
                                            + DbHelper.TABLE_R815_RESEARCH_CANDIDATE
                                            + " ORDER BY updated_at DESC LIMIT "
                                            + capped,
                                    null);
            while (cursor.moveToNext())
                candidates.add(
                        new Candidate(
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                                cursor.getString(4),
                                cursor.getString(5),
                                parseState(cursor.getString(6)),
                                cursor.getLong(7)));
        } finally {
            if (cursor != null) cursor.close();
        }
        evaluations = count(DbHelper.TABLE_R815_RESEARCH_EVAL);
        artifacts = count(DbHelper.TABLE_R815_RESEARCH_ARTIFACT);
        return new Snapshot(candidates, evaluations, artifacts);
    }

    public Candidate firstRunnable(String targetIssue) {
        Snapshot snapshot = snapshot(64);
        for (int i = 0; i < snapshot.candidates.size(); i++) {
            Candidate candidate = snapshot.candidates.get(i);
            if (candidate.createdTargetIssue.equals(targetIssue)
                    && (candidate.state == State.NEW || candidate.state == State.QUEUED)) return candidate;
        }
        return null;
    }

    private int count(String table) {
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery("SELECT COUNT(*) FROM " + table, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static ContentValues candidateValues(Candidate candidate, long now) {
        ContentValues values = new ContentValues();
        values.put("candidate_id", candidate.candidateId);
        values.put("candidate_type", candidate.candidateType);
        values.put("genome_sha256", candidate.genomeSha256);
        values.put("schema_sha256", candidate.schemaSha256);
        values.put("dataset_sha256", candidate.datasetSha256);
        values.put("game_code", ReleaseIdentityR815.GAME_CODE);
        values.put("state", candidate.state.name());
        values.put("created_target_issue", candidate.createdTargetIssue);
        values.put("created_at", candidate.createdAt);
        values.put("updated_at", now);
        values.put("detail", "");
        return values;
    }

    private static boolean allowed(State from, State to) {
        if (from == to) return true;
        if (from == State.FROZEN_CONFLICT || from == State.REJECTED || from == State.ELIGIBLE_FOR_SHADOW_PROMOTION)
            return false;
        if (to == State.FROZEN_CONFLICT || to == State.REJECTED) return true;
        if (from == State.NEW) return to == State.QUEUED;
        if (from == State.QUEUED) return to == State.RUNNING;
        if (from == State.RUNNING) return to == State.EVALUATED;
        return from == State.EVALUATED && to == State.ELIGIBLE_FOR_SHADOW_PROMOTION;
    }

    private static State parseState(String value) {
        try {
            return State.valueOf(safe(value));
        } catch (IllegalArgumentException failure) {
            return State.FROZEN_CONFLICT;
        }
    }

    private static double finite(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            throw new IllegalArgumentException("R815_RESEARCH_NUMBER_INVALID");
        return value;
    }

    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String bounded(String value, int max) {
        String clean = safe(value);
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
