package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Source-aware raw draw resolver. Conflicting permutations are quarantined, never last-write-wins. */
public final class DrawCanonicalizerR811 {
    private final Map<String, List<Observation>> byIssue = new HashMap<String, List<Observation>>();

    public synchronized Resolution observeRaw(String source, String rawPayload, Models.Draw draw) {
        if (draw == null || !draw.isValid()) return new Resolution("INVALID_RAW_DRAW", null, true, "");
        String issue = safe(draw.issue), drawHash = DrawHashR36.sha256V2(draw);
        String rawHash = V51ChecksumEngine.sha256(safe(rawPayload));
        List<Observation> rows = byIssue.get(issue); if (rows == null) { rows = new ArrayList<Observation>(); byIssue.put(issue, rows); }
        for (int i = 0; i < rows.size(); i++) {
            Observation old = rows.get(i);
            if (old.source.equals(safe(source)) && old.rawHash.equals(rawHash)) return resolve(issue);
        }
        rows.add(new Observation(safe(source), rawHash, drawHash, copy(draw), TimeSourceR811.wallNowMs()));
        return resolve(issue);
    }

    /** Durable overload used by API/draw storage. The raw payload itself is never duplicated. */
    public Resolution observeRaw(
            final DbStore store,
            String endpointId,
            String rawPayload,
            Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid())
            return new Resolution("INVALID_RAW_DRAW", null, true, "");
        final Models.Draw frozen = copy(draw);
        frozen.sourceLine = safe(endpointId);
        frozen.rawJson = safe(rawPayload);
        PersistenceDecision decision =
                store.coordinatorR36()
                        .runWrite(
                                "R811_DRAW_CANONICALIZE",
                                new DatabaseCoordinator.WriteCallable<PersistenceDecision>() {
                                    public PersistenceDecision call(SQLiteDatabase db) {
                                        return observeWithinWrite(db, frozen);
                                    }
                                },
                                PersistenceDecision.invalid("WRITE_FAILED"));
        return new Resolution(
                decision.status,
                decision.confirmed ? copy(frozen) : null,
                decision.conflict,
                decision.evidenceHash);
    }

    static PersistenceDecision observeWithinWrite(SQLiteDatabase db, Models.Draw draw) {
        if (db == null || draw == null || !draw.isValid())
            return PersistenceDecision.invalid("INVALID_RAW_DRAW");
        String issue = safe(draw.issue);
        String source = safe(draw.sourceLine);
        if (source.length() == 0) source = "UNKNOWN_ENDPOINT";
        String rawHash = V51ChecksumEngine.sha256(safe(draw.rawJson));
        String drawHash = DrawHashR36.sha256V2(draw);
        long observedAt = draw.fetchTime > 0L ? draw.fetchTime : TimeSourceR811.wallNowMs();
        persistExistingDrawProvenance(db, issue);
        String provenanceId =
                V51ChecksumEngine.sha256(
                        "R811_DRAW_PROVENANCE|"
                                + issue
                                + '|'
                                + source
                                + '|'
                                + rawHash
                                + '|'
                                + drawHash);
        ContentValues provenance = new ContentValues();
        provenance.put("provenance_id", provenanceId);
        provenance.put("endpoint_id", source);
        provenance.put("request_at", observedAt);
        provenance.put("response_at", observedAt);
        provenance.put("result_code", "PARSED_OK");
        provenance.put("raw_payload_sha256", rawHash);
        provenance.put("parsed_issue", issue);
        provenance.put("permutation", draw.numbersText());
        provenance.put("server_open_time", draw.openTime);
        provenance.put("parser_version", "R815_DRAW_CANONICALIZER_V2");
        provenance.put("source_line", source);
        provenance.put("draw_hash", drawHash);
        provenance.put("observed_at", observedAt);
        db.insertWithOnConflict(
                DbHelper.TABLE_R811_DRAW_PROVENANCE,
                null,
                provenance,
                SQLiteDatabase.CONFLICT_IGNORE);

        Cursor cursor = null;
        Set<String> hashes = new LinkedHashSet<String>();
        StringBuilder evidence = new StringBuilder();
        int sourceCount = 0;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT source_line,draw_hash,raw_payload_sha256,permutation FROM "
                                    + DbHelper.TABLE_R811_DRAW_PROVENANCE
                                    + " WHERE parsed_issue=? ORDER BY observed_at,source_line,provenance_id",
                            new String[] {issue});
            while (cursor.moveToNext()) {
                if (sourceCount++ > 0) evidence.append('|');
                String observedHash = hashFromPermutation(issue, cursor.getString(3));
                hashes.add(observedHash);
                evidence.append(safe(cursor.getString(0)))
                        .append(':')
                        .append(observedHash)
                        .append(':')
                        .append(safe(cursor.getString(2)));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        boolean conflict = hashes.size() > 1;
        String evidenceHash = V51ChecksumEngine.sha256(evidence.toString());
        ContentValues canonical = new ContentValues();
        canonical.put("target_issue", issue);
        canonical.put(
                "status",
                conflict ? "DRAW_CONFLICT_HARD_BLOCK" : "CONFIRMED_CANONICAL_DRAW");
        canonical.put("canonical_draw_hash", conflict ? "" : drawHash);
        canonical.put("evidence_hash", evidenceHash);
        canonical.put("source_count", sourceCount);
        canonical.put("conflict_count", Math.max(0, hashes.size() - 1));
        canonical.put("updated_at", TimeSourceR811.wallNowMs());
        // A repeated provider response is not a new canonical state. REPLACE
        // allocated a new rowid on every poll and invalidated retention generations
        // even when all provenance was unchanged. Keep the existing row identity;
        // updated_at now records an evidence/state change, not a polling heartbeat.
        boolean present=false,unchanged=false;
        try(Cursor current=db.rawQuery("SELECT status,canonical_draw_hash,evidence_hash,source_count,conflict_count FROM "
                +DbHelper.TABLE_R811_DRAW_CANONICAL+" WHERE target_issue=?",new String[]{issue})){
            if(current.moveToFirst()){
                present=true;
                unchanged=(conflict?"DRAW_CONFLICT_HARD_BLOCK":"CONFIRMED_CANONICAL_DRAW").equals(current.getString(0))
                        &&(conflict?"":drawHash).equals(current.getString(1))
                        &&evidenceHash.equals(current.getString(2))&&sourceCount==current.getInt(3)
                        &&Math.max(0,hashes.size()-1)==current.getInt(4);
            }
        }
        if(!present)db.insertOrThrow(DbHelper.TABLE_R811_DRAW_CANONICAL,null,canonical);
        else if(!unchanged&&db.update(DbHelper.TABLE_R811_DRAW_CANONICAL,canonical,
                "target_issue=?",new String[]{issue})!=1)
            throw new IllegalStateException("DRAW_CANONICAL_UPDATE_CARDINALITY");
        if (conflict) hardBlock(db, issue, evidenceHash);
        return new PersistenceDecision(
                !conflict,
                conflict,
                conflict ? "DRAW_CONFLICT_HARD_BLOCK" : "CONFIRMED_CANONICAL_DRAW",
                drawHash,
                evidenceHash,
                sourceCount,
                Math.max(0, hashes.size() - 1));
    }

    static boolean isConfirmedWithinRead(SQLiteDatabase db, Models.Draw draw) {
        if (db == null || draw == null || !draw.isValid()) return false;
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT status,canonical_draw_hash FROM "
                                    + DbHelper.TABLE_R811_DRAW_CANONICAL
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {draw.issue});
            return cursor.moveToFirst()
                    && "CONFIRMED_CANONICAL_DRAW".equals(safe(cursor.getString(0)))
                    && (DrawHashR36.sha256V2(draw).equals(safe(cursor.getString(1)))
                            || DrawHashR36.sha256(draw).equals(safe(cursor.getString(1))));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static void persistExistingDrawProvenance(SQLiteDatabase db, String issue) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT open_time,fetch_time,source_line,raw_json,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10"
                                    + " FROM "
                                    + DbHelper.TABLE_DRAWS
                                    + " WHERE issue=? LIMIT 1",
                            new String[] {issue});
            if (!cursor.moveToFirst()) return;
            Models.Draw existing = new Models.Draw();
            existing.issue = issue;
            existing.openTime = cursor.getLong(0);
            existing.fetchTime = cursor.getLong(1);
            existing.sourceLine = safe(cursor.getString(2));
            existing.rawJson = safe(cursor.getString(3));
            for (int i = 0; i < 10; i++) existing.numbers[i] = cursor.getInt(4 + i);
            if (!existing.isValid()) return;
            String source =
                    existing.sourceLine.length() == 0
                            ? "LEGACY_DB36"
                            : existing.sourceLine;
            String rawHash = V51ChecksumEngine.sha256(existing.rawJson);
            String drawHash = DrawHashR36.sha256V2(existing);
            long observedAt =
                    existing.fetchTime > 0L
                            ? existing.fetchTime
                            : TimeSourceR811.wallNowMs();
            String id =
                    V51ChecksumEngine.sha256(
                            "R811_DRAW_PROVENANCE|"
                                    + issue
                                    + '|'
                                    + source
                                    + '|'
                                    + rawHash
                                    + '|'
                                    + drawHash);
            ContentValues values = new ContentValues();
            values.put("provenance_id", id);
            values.put("endpoint_id", source);
            values.put("request_at", observedAt);
            values.put("response_at", observedAt);
            values.put("result_code", "LEGACY_DB_IMPORTED");
            values.put("raw_payload_sha256", rawHash);
            values.put("parsed_issue", issue);
            values.put("permutation", existing.numbersText());
            values.put("server_open_time", existing.openTime);
            values.put("parser_version", "R815_LEGACY_DB_IMPORT_V2");
            values.put("source_line", source);
            values.put("draw_hash", drawHash);
            values.put("observed_at", observedAt);
            db.insertWithOnConflict(
                    DbHelper.TABLE_R811_DRAW_PROVENANCE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE);
            ContentValues upgrade = new ContentValues();
            upgrade.put("draw_hash_v2", drawHash);
            upgrade.put("draw_hash_version", DrawHashR36.CURRENT_VERSION);
            upgrade.put(
                    "first_seen_at",
                    observedAt > 0L ? observedAt : TimeSourceR811.wallNowMs());
            db.update(
                    DbHelper.TABLE_DRAWS,
                    upgrade,
                    "issue=? AND (draw_hash_v2='' OR first_seen_at<=0)",
                    new String[] {issue});
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public synchronized Resolution resolve(String issue) {
        List<Observation> rows = byIssue.get(safe(issue));
        if (rows == null || rows.isEmpty()) return new Resolution("UNOBSERVED", null, false, "");
        String hash = rows.get(0).drawHash;
        for (int i = 1; i < rows.size(); i++) if (!hash.equals(rows.get(i).drawHash)) return new Resolution("DRAW_CONFLICT_QUARANTINED", null, true, conflict(issue));
        return new Resolution("CONFIRMED_CANONICAL_DRAW", copy(rows.get(0).draw), false, hash);
    }

    public synchronized String conflict(String issue) {
        List<Observation> rows = byIssue.get(safe(issue)); StringBuilder out = new StringBuilder();
        if (rows != null) for (int i = 0; i < rows.size(); i++) { if (i > 0) out.append('|'); out.append(rows.get(i).source).append(':').append(rows.get(i).drawHash).append(':').append(rows.get(i).rawHash); }
        return out.toString();
    }

    public synchronized Models.Draw confirmedDraw(String issue) { Resolution r = resolve(issue); return r.confirmed && !r.conflict ? copy(r.draw) : null; }

    private static void hardBlock(SQLiteDatabase db, String issue, String evidenceHash) {
        long now = TimeSourceR811.wallNowMs();
        ContentValues draw = new ContentValues();
        draw.put("dirty", 1);
        draw.put("dirty_reason", "DRAW_CONFLICT_HARD_BLOCK:" + evidenceHash);
        db.update(DbHelper.TABLE_DRAWS, draw, "issue=?", new String[] {issue});

        ContentValues task = new ContentValues();
        task.put("status", SettlementTaskStoreR36.INVALID_DRAW_TERMINAL);
        task.put("updated_at", now);
        task.put("completed_at", now);
        task.put("lease_owner", "");
        task.put("lease_until", 0L);
        task.put("next_attempt_at", 0L);
        task.put("last_error", "DRAW_CONFLICT_HARD_BLOCK:" + evidenceHash);
        task.put("terminal_code", "DRAW_CONFLICT_HARD_BLOCK");
        db.update(
                DbHelper.TABLE_R36_SETTLEMENT_TASK,
                task,
                "target_issue=?",
                new String[] {issue});

        ContentValues oos = new ContentValues();
        oos.put("strict_oos", 0);
        oos.put("error_code", "DRAW_CONFLICT_HARD_BLOCK");
        db.update(DbHelper.TABLE_R35_OOS_LABEL, oos, "target_issue=?", new String[] {issue});
    }

    private static Models.Draw copy(Models.Draw in) { if (in == null) return null; Models.Draw out = new Models.Draw(); out.issue = in.issue; out.openTime = in.openTime; out.fetchTime = in.fetchTime; out.sourceLine = in.sourceLine; out.rawJson = in.rawJson; out.dirty = in.dirty; out.dirtyReason = in.dirtyReason; out.numbers = in.numbers == null ? null : in.numbers.clone(); return out; }
    private static String hashFromPermutation(String issue, String permutation) {
        String[] tokens = safe(permutation).split(",");
        if (tokens.length != AppConfig.NUMBER_COUNT) return "INVALID_PERMUTATION";
        Models.Draw draw = new Models.Draw();
        draw.issue = safe(issue);
        for (int i = 0; i < tokens.length; i++) {
            try { draw.numbers[i] = Integer.parseInt(tokens[i].trim()); } catch (NumberFormatException failure) { return "INVALID_PERMUTATION"; }
        }
        return draw.isValid() && TargetIssueResolver.isValid(draw.issue)
                ? DrawHashR36.sha256V2(draw)
                : "INVALID_PERMUTATION";
    }
    private static String safe(String x) { return x == null ? "" : x.trim(); }
    private static final class Observation { final String source, rawHash, drawHash; final Models.Draw draw; final long observedAt; Observation(String s, String r, String h, Models.Draw d, long at) { source=s;rawHash=r;drawHash=h;draw=d;observedAt=at; } }
    public static final class Resolution { public final String status, evidence; public final Models.Draw draw; public final boolean conflict, confirmed; Resolution(String s, Models.Draw d, boolean c, String e) { status=s;draw=d;conflict=c;evidence=e;confirmed="CONFIRMED_CANONICAL_DRAW".equals(s); } }
    static final class PersistenceDecision { final boolean confirmed,conflict;final String status,drawHash,evidenceHash;final int sourceCount,conflictCount;PersistenceDecision(boolean confirmed,boolean conflict,String status,String drawHash,String evidenceHash,int sourceCount,int conflictCount){this.confirmed=confirmed;this.conflict=conflict;this.status=status;this.drawHash=drawHash;this.evidenceHash=evidenceHash;this.sourceCount=sourceCount;this.conflictCount=conflictCount;}static PersistenceDecision invalid(String code){return new PersistenceDecision(false,true,code,"","",0,0);} }
}
