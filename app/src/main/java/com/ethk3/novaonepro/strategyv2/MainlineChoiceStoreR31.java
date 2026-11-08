package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/**
 * R31-B10-C01: the one typed authority for the R27 mainline choice.
 *
 * <p>The certificate transaction (LiveShadowsR27.certify) is the only writer: in the same transaction it writes the
 * legacy chart projection (r27_chart_cache mainline/choice) and a versioned typed row bound to policy, experiment,
 * gate contract, certificate digest, model state, source revision, effective period and selection fence. A failure
 * rolls both back. Reads go through {@link #authorizedChoice}: while the authority mode is LEGACY_READ the legacy
 * projection still authorises, but only when the typed row agrees; a divergence authorises nothing (WAIT). After the
 * switch to TYPED_READ only the typed row authorises and the chart cache is a rebuildable projection; before the
 * switch, deleting the legacy projection is refused by a trigger. A stale fence and a conflicting choice for the same
 * effective period are refused; revocation appends a record and stops new freezes without rewriting history.</p>
 */
public final class MainlineChoiceStoreR31 {
    private MainlineChoiceStoreR31() {}
    public static final String DOMAIN = "RESEARCH_R31_MAINLINE_CHOICE", FALLBACK = "PRODUCTION_FALLBACK|NO_VALID_CERTIFICATE";
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r31_mainline_choice(choice_id TEXT PRIMARY KEY,policy_hash TEXT NOT NULL,experiment_id TEXT NOT NULL,gate_contract TEXT NOT NULL,certificate_digest TEXT NOT NULL,model_state_hash TEXT NOT NULL,source_revision INTEGER NOT NULL,effective_from_ordinal INTEGER NOT NULL,selection_fence INTEGER NOT NULL CHECK(selection_fence>=0),status TEXT NOT NULL CHECK(status IN ('ACTIVE','FALLBACK','SUPERSEDED','REVOKED')),created_at INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS r31_mainline_choice_one_active ON r31_mainline_choice(status) WHERE status='ACTIVE'",
        "CREATE TABLE IF NOT EXISTS r31_mainline_revocation(revocation_id TEXT PRIMARY KEY,choice_id TEXT NOT NULL,reason TEXT NOT NULL,source_revision INTEGER NOT NULL,revoked_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_mainline_authority(singleton INTEGER PRIMARY KEY CHECK(singleton=1),mode TEXT NOT NULL CHECK(mode IN ('LEGACY_READ','TYPED_READ')),switched_at INTEGER NOT NULL,evidence TEXT NOT NULL)",
        "INSERT OR IGNORE INTO r31_mainline_authority VALUES(1,'LEGACY_READ',0,'INITIAL')",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_choice_stale_fence BEFORE INSERT ON r31_mainline_choice WHEN NEW.selection_fence<(SELECT coalesce(max(selection_fence),0) FROM r31_mainline_choice) BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_STALE_FENCE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_choice_conflict BEFORE INSERT ON r31_mainline_choice WHEN EXISTS(SELECT 1 FROM r31_mainline_choice WHERE status IN ('ACTIVE','FALLBACK') AND effective_from_ordinal=NEW.effective_from_ordinal AND selection_fence=NEW.selection_fence AND policy_hash<>NEW.policy_hash) BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_CONFLICTING_CHOICE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_choice_history BEFORE UPDATE ON r31_mainline_choice WHEN NOT(OLD.status IN ('ACTIVE','FALLBACK') AND NEW.status IN ('SUPERSEDED','REVOKED') AND NEW.choice_id=OLD.choice_id AND NEW.policy_hash=OLD.policy_hash AND NEW.certificate_digest=OLD.certificate_digest AND NEW.selection_fence=OLD.selection_fence) BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_HISTORY_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_choice_no_delete BEFORE DELETE ON r31_mainline_choice BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_HISTORY_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_revocation_no_change BEFORE UPDATE ON r31_mainline_revocation BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_REVOCATION_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_legacy_projection_protected BEFORE DELETE ON r27_chart_cache WHEN OLD.panel_id='mainline' AND OLD.key='choice' AND (SELECT mode FROM r31_mainline_authority WHERE singleton=1)='LEGACY_READ' BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_LEGACY_PROJECTION_IS_STILL_AUTHORITY'); END",
    };
    public static final String[] OBJECTS = {"r31_mainline_choice", "r31_mainline_choice_one_active", "r31_mainline_revocation", "r31_mainline_authority",
            "r31_mainline_choice_stale_fence", "r31_mainline_choice_conflict", "r31_mainline_choice_history", "r31_mainline_choice_no_delete",
            "r31_mainline_revocation_no_change", "r31_mainline_legacy_projection_protected"};

    /** Migration owner only (SchemaDomainCoordinatorR926.migrateR31MainlineChoice). */
    public static void ensureSchemaR31(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MAINLINE_MIGRATION_TRANSACTION_REQUIRED");
        for (String sql : DDL) db.execSQL(sql);
    }
    public static boolean ready(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE name IN ('r31_mainline_choice','r31_mainline_authority')", null)) { return c.moveToFirst() && c.getInt(0) == 2; }
    }

    public static final class Choice {
        public final String choiceId, policyHash, experimentId, gateContract, certificateDigest, modelStateHash, status; public final long sourceRevision, effectiveFromOrdinal, selectionFence;
        Choice(String id, String policy, String experiment, String gate, String digest, String model, long revision, long effective, long fence, String status) {
            choiceId = id; policyHash = policy; experimentId = experiment; gateContract = gate; certificateDigest = digest; modelStateHash = model; sourceRevision = revision; effectiveFromOrdinal = effective; selectionFence = fence; this.status = status;
        }
        public boolean fallback() { return policyHash.isEmpty(); }
    }
    public static String choiceId(String policy, String digest, long effective, long fence) { return StatsR27.sha256("R31_MAINLINE_CHOICE_V1|" + policy + "|" + digest + "|" + effective + "|" + fence); }

    /** Inside the certificate transaction: supersede the current row and append the new one (idempotent for an unchanged choice). */
    public static Choice appendChoice(SQLiteDatabase db, String policy, String experiment, String gateContract, String certificateDigest, String modelStateHash, long sourceRevision, long effectiveFrom, long fence, long now) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MAINLINE_WRITER_REQUIRED");
        String p = policy == null ? "" : policy, digest = certificateDigest == null ? "" : certificateDigest;
        Choice current = current(db);
        if (current != null && current.policyHash.equals(p) && current.certificateDigest.equals(digest) && current.sourceRevision == sourceRevision) return current;
        String id = choiceId(p, digest, effectiveFrom, fence), status = p.isEmpty() ? "FALLBACK" : "ACTIVE";
        if (current != null && current.selectionFence > fence) throw new IllegalStateException("R31_MAINLINE_STALE_FENCE");
        if (current != null && current.effectiveFromOrdinal == effectiveFrom && current.selectionFence == fence && !current.policyHash.equals(p))
            throw new IllegalStateException("R31_MAINLINE_CONFLICTING_CHOICE");
        if (current != null) db.execSQL("UPDATE r31_mainline_choice SET status='SUPERSEDED' WHERE choice_id=?", new Object[]{current.choiceId});
        db.execSQL("INSERT INTO r31_mainline_choice VALUES(?,?,?,?,?,?,?,?,?,?,?)", new Object[]{id, p, experiment == null ? "" : experiment, gateContract, digest, modelStateHash == null ? "" : modelStateHash, sourceRevision, effectiveFrom, fence, status, now});
        return new Choice(id, p, experiment, gateContract, digest, modelStateHash, sourceRevision, effectiveFrom, fence, status);
    }
    /** The current ACTIVE or FALLBACK row, or null before the first typed choice. */
    public static Choice current(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT choice_id,policy_hash,experiment_id,gate_contract,certificate_digest,model_state_hash,source_revision,effective_from_ordinal,selection_fence,status FROM r31_mainline_choice WHERE status IN ('ACTIVE','FALLBACK') ORDER BY selection_fence DESC,created_at DESC LIMIT 1", null)) {
            if (!c.moveToFirst()) return null;
            return new Choice(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getLong(6), c.getLong(7), c.getLong(8), c.getString(9));
        }
    }
    /** ACTIVE choice whose certificate is still the same ACTIVE, unexpired, unrevoked certificate; otherwise null. */
    public static Choice effectiveChoice(SQLiteDatabase db, long now) {
        Choice c = current(db);
        if (c == null || c.fallback() || !"ACTIVE".equals(c.status)) return null;
        try (Cursor q = db.rawQuery("SELECT evidence_digest FROM r27_certificate WHERE policy_hash=? AND status='ACTIVE' AND gate_version=? AND issued_at<=? AND expires_at>?",
                new String[]{c.policyHash, CertificateGateR27.GATE_VERSION, String.valueOf(now), String.valueOf(now)})) {
            if (!q.moveToFirst() || !c.certificateDigest.equals(q.getString(0))) return null;
        }
        return c;
    }
    /** Appends a revocation and retires the choice; frozen history is untouched. */
    public static void revoke(SQLiteDatabase db, String choiceId, String reason, long sourceRevision, long now) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MAINLINE_WRITER_REQUIRED");
        db.execSQL("INSERT OR IGNORE INTO r31_mainline_revocation VALUES(?,?,?,?,?)", new Object[]{StatsR27.sha256(choiceId + "|" + reason + "|" + sourceRevision), choiceId, reason, sourceRevision, now});
        db.execSQL("UPDATE r31_mainline_choice SET status='REVOKED' WHERE choice_id=? AND status IN ('ACTIVE','FALLBACK')", new Object[]{choiceId});
    }

    public static String legacyChoice(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'", null)) {
            if (!c.moveToFirst()) return null; Object v = ((Map<?, ?>) JsonR27.parse(c.getString(0))).get("choice"); return v == null ? null : v.toString();
        }
    }
    /** MATCH, LEGACY_ONLY, TYPED_ONLY, ABSENT or DIVERGED:&lt;legacy&gt;/&lt;typed&gt;. */
    public static String compareLegacyProjection(SQLiteDatabase db) {
        String legacy = legacyChoice(db); Choice typed = current(db);
        String t = typed == null ? null : typed.fallback() ? FALLBACK : typed.policyHash;
        if (legacy == null && t == null) return "ABSENT";
        if (t == null) return "LEGACY_ONLY";
        if (legacy == null) return "TYPED_ONLY";
        return legacy.equals(t) ? "MATCH" : "DIVERGED:" + legacy + "/" + t;
    }
    public static String authorityMode(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT mode FROM r31_mainline_authority WHERE singleton=1", null)) { return c.moveToFirst() ? c.getString(0) : "LEGACY_READ"; }
    }
    /** One authority switch after the double read agrees; afterwards the chart cache is only a projection. */
    public static void switchToTypedRead(SQLiteDatabase db, long now) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MAINLINE_WRITER_REQUIRED");
        String compared = compareLegacyProjection(db);
        if (!"MATCH".equals(compared)) throw new IllegalStateException("R31_MAINLINE_SWITCH_REQUIRES_MATCH:" + compared);
        db.execSQL("UPDATE r31_mainline_authority SET mode='TYPED_READ',switched_at=?,evidence=? WHERE singleton=1", new Object[]{now, "DOUBLE_READ_MATCH:" + current(db).choiceId});
    }

    /** Result of the single read path: an authorised policy or a specific WAIT reason. */
    public static final class Authorization {
        public final String policyHash, reason, mode; Authorization(String policy, String reason, String mode) { policyHash = policy; this.reason = reason; this.mode = mode; }
        public boolean authorized() { return policyHash != null; }
    }
    public static Authorization authorizedChoice(SQLiteDatabase db, long now) {
        if (!ready(db)) { String legacy = legacyChoice(db); return legacy == null || legacy.startsWith("PRODUCTION_FALLBACK") ? new Authorization(null, "WAIT_NO_VALID_CERTIFICATE", "LEGACY_ONLY_SCHEMA_PENDING") : new Authorization(legacy, "LEGACY_ONLY_SCHEMA_PENDING", "LEGACY_ONLY_SCHEMA_PENDING"); }
        String mode = authorityMode(db);
        if ("TYPED_READ".equals(mode)) {
            Choice c = effectiveChoice(db, now);
            if (c == null) { Choice cur = current(db); return new Authorization(null, cur == null ? "WAIT_NO_TYPED_CHOICE" : cur.fallback() ? "WAIT_NO_VALID_CERTIFICATE" : "WAIT_CERTIFICATE_EXPIRED_REVOKED_OR_CHANGED", mode); }
            return new Authorization(c.policyHash, "TYPED_AUTHORITY:" + c.choiceId, mode);
        }
        String compared = compareLegacyProjection(db), legacy = legacyChoice(db);
        if (compared.startsWith("DIVERGED")) return new Authorization(null, "WAIT_MAINLINE_DIVERGED:" + compared, mode); // never pick one of two authorities
        if (legacy == null || legacy.startsWith("PRODUCTION_FALLBACK")) return new Authorization(null, "WAIT_NO_VALID_CERTIFICATE", mode);
        return new Authorization(legacy, "LEGACY_AUTHORITY_" + compared, mode);
    }
}
