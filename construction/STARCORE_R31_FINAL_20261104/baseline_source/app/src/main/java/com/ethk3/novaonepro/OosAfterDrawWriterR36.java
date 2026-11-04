package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Strict post-draw labeler. It can be entered only by SettlementEngine. */
public final class OosAfterDrawWriterR36 {
    public static final int MODEL_INVOCATIONS = 0;

    private OosAfterDrawWriterR36() {}

    static LabelResult settleFrozenWithinTransaction(
            SQLiteDatabase db, SettlementEngine.OwnerToken token, Models.Draw draw) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R36_OOS_TX_REQUIRED");
        if (draw == null || !draw.isValid()) return fail("", "DRAW_INVALID");
        String target = safe(draw.issue);
        String drawHash = DrawHashR36.sha256V2(draw);
        if (!SettlementEngine.isOwnerTokenR36(token, target))
            throw new SecurityException("R36_SETTLEMENT_OWNER_REQUIRED");
        Cursor c = null;
        String base;
        int position, n1, n2, n3, preDraw, alreadySettled;
        double probability;
        long predictedAt, commitAt;
        String frozenHash, historyHash, snapshotHash, actionSpaceHash, gameCode, historyMax;
        try {
            c =
                    db.rawQuery(
                            "SELECT"
                                + " base_issue,position,n1,n2,n3,selected_hit_p,created_at,commit_at,"
                                + "record_hash,pre_draw_frozen,model_proven,settled,history_hash,snapshot_hash,action_space_hash,game_code,history_max_issue"
                                + " FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            if (!c.moveToFirst())
                return fail(
                        target,
                        "FROZEN_OBSERVATION_MISSING",
                        "",
                        0L,
                        draw.openTime,
                        "",
                        "",
                        drawHash);
            base = safe(c.getString(0));
            position = c.getInt(1);
            n1 = c.getInt(2);
            n2 = c.getInt(3);
            n3 = c.getInt(4);
            probability = c.getDouble(5);
            predictedAt = c.getLong(6);
            commitAt = c.getLong(7);
            frozenHash = safe(c.getString(8));
            preDraw = c.getInt(9);
            // MODEL_PROVEN is intentionally not an OOS eligibility condition. A SHADOW-only
            // release must still accumulate rigorously frozen labels.
            alreadySettled = c.getInt(11);
            historyHash = safe(c.getString(12));
            snapshotHash = safe(c.getString(13));
            actionSpaceHash = safe(c.getString(14));
            gameCode = safe(c.getString(15));
            historyMax = safe(c.getString(16));
        } finally {
            if (c != null) c.close();
        }
        boolean exactUncalibrated = probability == -1.0d
                && (uncalibratedShadowObservation(db,target,frozenHash,snapshotHash)
                    || exactUncalibratedReceipt(db,target,position,n1,n2,n3,frozenHash,snapshotHash));
        if(!exactUncalibrated && (!Double.isFinite(probability)||probability<0.0d||probability>1.0d))
            return fail(target,"FROZEN_PROBABILITY_INVALID",base,commitAt,draw.openTime,historyHash,frozenHash,drawHash);
        long frozenAt = commitAt; // Missing durable commit time is not reconstructed from created_at.
        if (preDraw != 1 || !V51Repository.GAME_CODE.equals(gameCode)
                || !base.equals(historyMax) || !TargetIssueResolver.exact(base, target).valid)
            return fail(
                    target,
                    "FROZEN_CONTRACT_INVALID",
                    base,
                    frozenAt,
                    draw.openTime,
                    historyHash,
                    frozenHash,
                    drawHash);
        if (!Batch2ContractCoreR926.isSha256(historyHash)
                || !Batch2ContractCoreR926.isSha256(snapshotHash)
                || !Batch2ContractCoreR926.isSha256(actionSpaceHash)
                || !Batch2ContractCoreR926.isSha256(frozenHash))
            return fail(
                    target,
                    "FROZEN_PROOF_HASH_INVALID",
                    base,
                    frozenAt,
                    draw.openTime,
                    historyHash,
                    frozenHash,
                    drawHash);
        long firstSeenAt = firstSeenAt(db, target);
        if (firstSeenAt <= 0L)
            return fail(
                    target,
                    "DRAW_FIRST_SEEN_MISSING",
                    base,
                    frozenAt,
                    draw.openTime,
                    historyHash,
                    frozenHash,
                    drawHash);
        if (frozenAt <= 0L || draw.openTime <= 0L
                || frozenAt >= firstSeenAt || frozenAt >= draw.openTime)
            return fail(
                    target,
                    "PIT_FREEZE_BEFORE_OPEN_AND_FIRST_SEEN_REQUIRED",
                    base,
                    frozenAt,
                    draw.openTime,
                    historyHash,
                    frozenHash,
                    drawHash);
        if (position < 1 || position > 10 || !DecisionGateEngine.b12K3(n1, n2, n3))
            return fail(
                    target,
                    "FROZEN_K3_INVALID",
                    base,
                    frozenAt,
                    draw.openTime,
                    historyHash,
                    frozenHash,
                    drawHash);
        int actual = draw.getNumberAtPosition(position);
        if (!AppConfig.isValidNumber(actual))
            return fail(
                    target,
                    "ACTUAL_POSITION_NUMBER_INVALID",
                    base,
                    frozenAt,
                    draw.openTime,
                    historyHash,
                    frozenHash,
                    drawHash);
        int hit = actual == n1 || actual == n2 || actual == n3 ? 1 : 0;
        long now = System.currentTimeMillis();
        String provenance =
                V51ChecksumEngine.sha256(
                        "R36_OOS_PROVENANCE|"
                                + frozenHash
                                + '|'
                                + snapshotHash
                                + '|'
                                + actionSpaceHash
                                + '|'
                                + historyHash
                                + '|'
                                + DrawHashR36.sha256V2(draw)
                                + '|'
                                + firstSeenAt);
        String record = V51ChecksumEngine.sha256(provenance + '|' + actual + '|' + hit);
        double brier =
                probability >= 0.0d && probability <= 1.0d
                        ? (probability - hit) * (probability - hit)
                        : -1.0d;
        ContentValues label = new ContentValues();
        label.put("target_issue", target);
        label.put("game_code", V51Repository.GAME_CODE);
        label.put("base_issue", base);
        label.put("position", position);
        label.put("n1", n1);
        label.put("n2", n2);
        label.put("n3", n3);
        label.put("selected_hit_p", probability);
        label.put("actual_number", actual);
        label.put("hit_label", hit);
        label.put("strict_oos", 1);
        label.put("pre_draw_frozen", 1);
        label.put("prediction_created_at", frozenAt);
        label.put("draw_open_time", draw.openTime);
        label.put("settled_at", now);
        label.put("brier", brier);
        label.put("provenance_hash", provenance);
        label.put("record_hash", record);
        label.put("error_code", "");
        long inserted =
                db.insertWithOnConflict(
                        DbHelper.TABLE_R35_OOS_LABEL, null, label, SQLiteDatabase.CONFLICT_IGNORE);
        String effectiveRecord = record;
        if (inserted < 0L) {
            ExistingLabel existing = existingBusinessLabel(db, target);
            if (existing == null
                    || !existing.matches(base, position, n1, n2, n3, actual, hit)
                    || !record.equals(existing.recordHash)) {
                throw new IllegalStateException("R36_OOS_FIRST_WRITE_CONFLICT");
            }
            // Frozen prediction/draw provenance is part of replay identity. Legacy labels
            // remain immutable evidence; a hash-only difference cannot authorize a new replay.
            effectiveRecord = existing.recordHash;
        }
        ContentValues shadow = new ContentValues();
        shadow.put("settled", 1);
        shadow.put("actual_number", actual);
        shadow.put("hit_label", hit);
        shadow.put("settled_at", now);
        shadow.put("settlement_hash", effectiveRecord);
        shadow.put("error_code", "");
        int changed =
                db.update(
                        DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                        shadow,
                        "target_issue=? AND settled=0",
                        new String[] {target});
        if (changed == 0 && !shadowBusinessMatches(db, target, actual, hit))
            throw new IllegalStateException("R36_SHADOW_SETTLEMENT_CONFLICT");
        boolean replay = inserted < 0L || alreadySettled == 1 || changed == 0;
        return new LabelResult(
                true,
                replay,
                target,
                hit,
                probability,
                brier,
                replay ? "REPLAY_IDEMPOTENT_BUSINESS_FACTS"
                        : exactUncalibrated ? "STRICT_ACTION_OOS_UNCALIBRATED" : "STRICT_OOS_COMMITTED",
                effectiveRecord,
                base,
                frozenAt,
                draw.openTime,
                historyHash,
                frozenHash,
                drawHash);
    }

    /** Action labels do not require a calibrated probability. The explicit -1 sentinel is
     * eligible only for an exact frozen SHADOW row with no REAL trade. All PIT/K3/provenance
     * checks below still apply; this predicate has no REAL authorization authority. */
    static boolean uncalibratedShadowObservation(SQLiteDatabase db,String target,
            String canonicalHash,String snapshotHash) {
        try(Cursor c=db.rawQuery("SELECT a.decision_id,s.contract_hash,s.authorization_hash,s.commit_at FROM "
                +DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s JOIN "+V51Repository.AUTH+" a"
                +" ON a.target_issue=s.target_issue AND a.authorization_hash=s.authorization_hash"
                +" AND a.contract_hash=s.contract_hash AND a.bundle_hash=s.bundle_hash"
                +" WHERE s.target_issue=? AND s.decision_action='SHADOW' AND s.lane='SHADOW'"
                +" AND a.game_code=s.game_code AND s.game_code=?"
                +" AND EXISTS(SELECT 1 FROM "+V51Repository.SELECTED+" v WHERE v.target_issue=s.target_issue"
                +" AND v.game_code=s.game_code AND v.position=s.position AND v.n1=s.n1 AND v.n2=s.n2 AND v.n3=s.n3"
                +" AND v.decision_action='SHADOW' AND v.lane='SHADOW' AND v.selected_hit_p=-1"
                +" AND v.contract_hash=s.contract_hash AND v.bundle_hash=s.bundle_hash"
                +" AND v.authorization_hash=s.authorization_hash AND v.decision_id=a.decision_id)"
                +" AND s.pre_draw_frozen=1 AND s.selected_hit_p=-1"
                +" AND s.record_hash=? AND s.snapshot_hash=? AND NOT EXISTS(SELECT 1 FROM "
                +DbHelper.TABLE_TRADES+" t WHERE t.target_issue=s.target_issue AND t.action='BET'"
                +" AND t.stat_scope='REAL_TRADE')",
                new String[]{target,V51Repository.GAME_CODE,canonicalHash,snapshotHash})) {
            if(!c.moveToFirst()) return false;
            String[] fields=safe(c.getString(2)).split("\\|",-1);
            boolean exact=fields.length==8 && "V51AUTH".equals(fields[0])
                    && safe(c.getString(0)).equals(fields[1])
                    && safe(c.getString(1)).equals(fields[2])
                    && Batch2ContractCoreR926.isSha256(fields[1])
                    && Batch2ContractCoreR926.isSha256(fields[2])
                    && Batch2ContractCoreR926.isSha256(fields[3])
                    && "SHADOW".equals(fields[4])
                    && V51AuthorizationCore.authorizationTime(c.getString(2))>0L
                    && c.getLong(3)>0L && V51AuthorizationCore.authorizationTime(c.getString(2))<=c.getLong(3)
                    && Batch2ContractCoreR926.isSha256(fields[7]);
            return exact && !c.moveToNext();
        }
    }

    /** Read-only eligibility for a corrected SHADOW failure. No model/REAL writes. */
    static boolean recoverableUncalibratedShadow(SQLiteDatabase db,String target) {
        return recoverableFrozenObservationR81(db,target,false);
    }
    /** Read-only reopening admission. Calibrated rows are permitted only for an
     * explicitly transient failure; the sole settlement owner still revalidates
     * the original frozen contract and authorization before writing any label.
     * The corrected -1 SHADOW path retains its stricter authorization predicate. */
    static boolean recoverableTransientFrozenR81(SQLiteDatabase db,String target) {
        return recoverableFrozenObservationR81(db,target,true);
    }
    private static boolean recoverableFrozenObservationR81(SQLiteDatabase db,String target,boolean allowCalibrated) {
        try(Cursor c=db.rawQuery("SELECT s.record_hash,s.snapshot_hash,s.game_code,s.base_issue,"
                +"s.history_max_issue,s.pre_draw_frozen,s.settled,s.position,s.n1,s.n2,s.n3,"
                +"s.commit_at,s.history_hash,s.action_space_hash,"
                +"d.open_time,d.first_seen_at,d.dirty,d.draw_hash_v2,t.draw_hash,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10,s.selected_hit_p"
                +" FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s JOIN "+DrawDependencyInputCP12.SOURCE
                +" d ON d.issue=s.target_issue JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" t ON t.target_issue=s.target_issue AND t.draw_issue=s.target_issue"
                +" WHERE s.target_issue=?",new String[]{target})) {
            if(!c.moveToFirst())return false;
            String canonical=safe(c.getString(0)),snapshot=safe(c.getString(1)),base=safe(c.getString(3));
            long frozenAt=c.getLong(11),open=c.getLong(14),seen=c.getLong(15);
            boolean valid=V51Repository.GAME_CODE.equals(c.getString(2))&&base.equals(c.getString(4))
                    &&TargetIssueResolver.exact(base,target).valid&&c.getInt(5)==1&&c.getInt(6)==0
                    &&AppConfig.isValidPosition(c.getInt(7))&&DecisionGateEngine.b12K3(c.getInt(8),c.getInt(9),c.getInt(10))
                    &&Batch2ContractCoreR926.isSha256(canonical)&&Batch2ContractCoreR926.isSha256(snapshot)
                    &&Batch2ContractCoreR926.isSha256(c.getString(12))&&Batch2ContractCoreR926.isSha256(c.getString(13))
                    &&frozenAt>0L&&open>frozenAt&&seen>frozenAt&&c.getInt(16)==0
                    &&Batch2ContractCoreR926.isSha256(c.getString(17))&&c.getString(17).equals(c.getString(18));
            Models.Draw persisted=new Models.Draw();persisted.issue=target;persisted.openTime=open;persisted.dirty=c.getInt(16);
            for(int i=0;i<10;i++)persisted.numbers[i]=c.getInt(19+i);
            valid=valid&&persisted.isValid()&&DrawHashR36.sha256V2(persisted).equals(c.getString(17));
            boolean calibrated=allowCalibrated&&!c.isNull(29)&&Double.isFinite(c.getDouble(29))
                    &&c.getDouble(29)>=0.0d&&c.getDouble(29)<=1.0d;
            boolean originalUnknown=!c.isNull(29)&&c.getDouble(29)==-1.0d
                    &&uncalibratedShadowObservation(db,target,canonical,snapshot);
            return valid&&!c.moveToNext()&&(calibrated||originalUnknown)
                    &&durableSnapshotHashMatches(db,target,snapshot);
        }
    }
    private static boolean durableSnapshotHashMatches(SQLiteDatabase db,String target,String expected) {
        try(Cursor c=db.rawQuery("SELECT snapshot_hash,snapshot_blob FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                +" WHERE target_issue=?",new String[]{target})) {
            if(!c.moveToFirst()||!expected.equals(c.getString(0))||c.isNull(1))return false;
            byte[] bytes=c.getBlob(1);
            return bytes!=null&&bytes.length>0&&bytes.length<=AppConfig.R36_SNAPSHOT_MAX_BYTES
                    &&expected.equals(DeferredEvidenceSnapshotR36.sha256(bytes))&&!c.moveToNext();
        }
    }

    /** REAL action facts require their original immutable authorization receipt. */
    private static boolean exactUncalibratedReceipt(SQLiteDatabase db,String target,
            int position,int n1,int n2,int n3,String canonicalHash,String snapshotHash) {
        try(Cursor c=db.rawQuery(
                "SELECT e.model_hash,e.certificate_hash,e.proof_hash,e.recipe_hash,e.action_hash,e.authorization_hash,g.payload_text,g.payload_hash"
                +" FROM r974_exact_real_receipt e JOIN "+DbHelper.TABLE_TRADES+" t ON t._id=e.trade_id AND t.target_issue=e.target_issue"
                +" JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s ON s.target_issue=e.target_issue"
                +" JOIN "+V51Repository.AUTH+" a ON a.target_issue=e.target_issue AND a.authorization_hash=e.authorization_hash"
                +" JOIN "+RuntimeEventLedgerR926.TABLE_EVENT+" g ON g.target_issue=e.target_issue AND g.event_type='GATE_EVIDENCE' AND g.source='Batch2GateEvidenceR926'"
                +" WHERE e.target_issue=? AND t.action='BET' AND t.stat_scope='REAL_TRADE'"
                +" AND t.authorization_id=e.authorization_hash AND s.authorization_hash=e.authorization_hash"
                +" AND s.decision_action='BET' AND s.pre_draw_frozen=1 AND s.selected_hit_p=-1"
                +" AND s.record_hash=? AND s.snapshot_hash=? AND a.contract_hash=s.contract_hash AND a.bundle_hash=s.bundle_hash"
                +" AND t.position=? AND t.n1=? AND t.n2=? AND t.n3=?",
                new String[]{target,canonicalHash,snapshotHash,String.valueOf(position),String.valueOf(n1),String.valueOf(n2),String.valueOf(n3)})) {
            if(!c.moveToFirst())return false;
            for(int i=0;i<5;i++)if(!Batch2ContractCoreR926.isSha256(c.getString(i)))return false;
            String payload=c.getString(6);
            boolean valid=V51AuthorizationCore.isBetEnvelope(c.getString(5))&&payload!=null
                    &&V51ChecksumEngine.sha256(payload).equals(c.getString(7))
                    &&payload.contains("|canonical_hash="+canonicalHash+"|")
                    &&payload.contains("|snapshot_hash="+snapshotHash+"|")
                    &&payload.contains("|certificate_mode=R974_EXACT_CERTIFICATE_AND_CANONICAL_RECEIPT_BOUND|");
            return valid&&!c.moveToNext();
        }
    }

    private static ExistingLabel existingBusinessLabel(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT record_hash,base_issue,position,n1,n2,n3,actual_number,hit_label"
                            + " FROM " + DbHelper.TABLE_R35_OOS_LABEL
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {target});
            if (!c.moveToFirst()) return null;
            return new ExistingLabel(
                    safe(c.getString(0)), safe(c.getString(1)), c.getInt(2),
                    c.getInt(3), c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7));
        } finally {
            if (c != null) c.close();
        }
    }

    private static final class ExistingLabel {
        final String recordHash, baseIssue;
        final int position, n1, n2, n3, actual, hit;
        ExistingLabel(String r, String b, int p, int a, int bb, int cc, int actual, int hit) {
            recordHash=r; baseIssue=b; position=p; n1=a; n2=bb; n3=cc;
            this.actual=actual; this.hit=hit;
        }
        boolean matches(String b, int p, int a, int bb, int cc, int actual, int hit) {
            return baseIssue.equals(safe(b)) && position==p && n1==a && n2==bb && n3==cc
                    && this.actual==actual && this.hit==hit;
        }
    }

    private static long firstSeenAt(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT first_seen_at FROM "
                                    + DrawDependencyInputCP12.SOURCE
                                    + " WHERE issue=? LIMIT 1",
                            new String[] {target});
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean shadowBusinessMatches(
            SQLiteDatabase db, String target, int actual, int hit) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT actual_number,hit_label,settled FROM "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {target});
            return c.moveToFirst()
                    && actual == c.getInt(0)
                    && hit == c.getInt(1)
                    && c.getInt(2) == 1;
        } finally {
            if (c != null) c.close();
        }
    }

    private static LabelResult fail(String target, String status) {
        return new LabelResult(false, false, target, -1, -1.0d, -1.0d, status, "");
    }

    private static LabelResult fail(
            String target,
            String status,
            String base,
            long frozenAt,
            long drawOpenTime,
            String historyHash,
            String canonicalHash,
            String drawHash) {
        return new LabelResult(
                false,
                false,
                target,
                -1,
                -1.0d,
                -1.0d,
                status,
                "",
                base,
                frozenAt,
                drawOpenTime,
                historyHash,
                canonicalHash,
                drawHash);
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

    public static final class LabelResult {
        public final boolean success, replay;
        public final String targetIssue,
                status,
                recordHash,
                baseIssue,
                historyHash,
                canonicalHash,
                drawHash;
        public final int hitLabel;
        public final double selectedHitP, brier;
        public final long frozenAt, drawOpenTime;

        LabelResult(
                boolean ok,
                boolean replay,
                String target,
                int hit,
                double p,
                double brier,
                String status,
                String hash) {
            this(
                    ok,
                    replay,
                    target,
                    hit,
                    p,
                    brier,
                    status,
                    hash,
                    "",
                    0L,
                    0L,
                    "",
                    "",
                    "");
        }

        LabelResult(
                boolean ok,
                boolean replay,
                String target,
                int hit,
                double p,
                double brier,
                String status,
                String hash,
                String base,
                long frozenAt,
                long drawOpenTime,
                String historyHash,
                String canonicalHash,
                String drawHash) {
            success = ok;
            this.replay = replay;
            targetIssue = safe(target);
            hitLabel = hit;
            selectedHitP = p;
            this.brier = brier;
            this.status = safe(status);
            recordHash = safe(hash);
            baseIssue = safe(base);
            this.frozenAt = frozenAt;
            this.drawOpenTime = drawOpenTime;
            this.historyHash = safe(historyHash);
            this.canonicalHash = safe(canonicalHash);
            this.drawHash = safe(drawHash);
        }

        public String summary() {
            return status
                    + "|target="
                    + targetIssue
                    + "|hit="
                    + hitLabel
                    + "|p="
                    + selectedHitP
                    + "|brier="
                    + brier
                    + "|replay="
                    + replay
                    + "|base="
                    + baseIssue
                    + "|frozenAt="
                    + frozenAt
                    + "|drawOpenTime="
                    + drawOpenTime
                    + "|historyHash="
                    + historyHash
                    + "|canonicalHash="
                    + canonicalHash
                    + "|drawHash="
                    + drawHash
                    + "|errorCode="
                    + (success ? "" : status);
        }
    }
}
