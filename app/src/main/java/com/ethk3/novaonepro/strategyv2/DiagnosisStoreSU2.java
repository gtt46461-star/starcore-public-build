package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * P50 on the phone: keeps the information probes running, folds every strategy's settled decisions into bounded
 * sufficient statistics, writes one upgrade diagnosis per 500 settled periods, turns genome-level prescriptions into
 * live shadow challengers and settles them, and applies the retention rules - all from the existing burst job, never in
 * the pre-draw or settlement path. The report export reads the latest diagnosis read-only (report()).
 *
 * Bounded by construction (P50 retention table, docs/18):
 *   su2_track_block      per member: the latest 40 closed 500-period blocks + one lifetime row (exact totals)
 *   su2_diag_state       one ledger text <= 256 KB (<= 256 issues, <= 512 generation-curve points)
 *   su2_diag_snapshot    the latest 8 snapshots, each <= 16 KB
 *   su2_diag_shadow      at most MAX_SHADOWS active shadow challengers
 *   su2_live_state       owner 'PROBE' (~10 MB raw, fixed); stale owners removed
 *   su2_live_daily       90 days; su2_mainline_binding 10,080 targets; su2_mainline_choice: every switch + latest 2,000;
 *   su2_certificate      30 days after expiry
 */
public final class DiagnosisStoreSU2 {
    private DiagnosisStoreSU2() {}
    public static final int BLOCK = 500, KEEP_BLOCKS = 40, MAX_SHADOWS = 4, DAILY_DAYS = 90, BINDINGS = 10080, CHOICES = 2000;
    public static final long PROBE_SLICE_NANOS = 2_000_000_000L, CERT_KEEP_MS = 30L * 86400000L;
    static final String PROBE_OWNER = "PROBE";             // literal 'PROBE' in the retention SQL

    /** Called from the burst job after retention. Never throws into the scheduler. */
    public static String maintain(DbStore store, String target) {
        try {
            SQLiteDatabase r = store.readable();
            if (!StrategySchemaSU2.ready(r)) return "SU2_DIAG_SCHEMA_PENDING";
            EventFrameSU1 frame = EventFrameBuilderSU1.capture(store, target);
            final int[][] h = frame.history();
            final long first = TargetIssueResolver.sequenceOrdinalSU1(String.valueOf(frame.issue(0)));
            final long seq = first + h.length;                                   // next target; everything below is settled
            // 1. information probes: continue from the checkpoint, at most PROBE_SLICE_NANOS per call
            long[] through = new long[1]; byte[] blob = ModelStoreSU2.liveBlob(r, PROBE_OWNER, through);
            InformationProbeSU2 bank = new InformationProbeSU2(); long done = first - 1;
            if (blob != null && through[0] >= first - 1 && through[0] < seq) {
                try { bank.read(new DataInputStream(new ByteArrayInputStream(ModelStoreSU2.gunzip(blob)))); done = through[0]; }
                catch (IOException | RuntimeException unusable) { bank = new InformationProbeSU2(); done = first - 1; }
            }
            long started = System.nanoTime();
            while (done < seq - 1 && System.nanoTime() - started < PROBE_SLICE_NANOS) { done++; bank.step(h[(int) (done - first)]); }
            final InformationProbeSU2 fb = bank; final long fdone = done; final long now = System.currentTimeMillis();
            ByteArrayOutputStream bo = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(bo); fb.write(o); o.flush();
            final byte[] probeBlob = ModelStoreSU2.gzip(bo.toByteArray());
            final ModelEvolutionSU2.State evo = ModelStoreSU2.loadEvolution(r);
            return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_DIAGNOSIS", db -> {
                ModelStoreSU2.saveLive(db, PROBE_OWNER, fdone, probeBlob, now);
                foldTrackBlocks(db, seq);
                settleShadows(db);
                retention(db, seq, now);
                if (fdone < seq - 1) return "SU2_DIAG_PROBES_CATCHING_UP:" + fdone;
                long last = diagnosedSequence(db);
                if (last >= 0 && seq / BLOCK == last / BLOCK) return "SU2_DIAG_UP_TO_DATE";
                String snap = snapshot(db, seq, fb, evo);
                UpgradeDiagnosisSU2.Snapshot S = UpgradeDiagnosisSU2.parse(snap);
                UpgradeDiagnosisSU2.Diagnosis D = UpgradeDiagnosisSU2.diagnose(S);
                UpgradeDiagnosisSU2.Ledger L = loadLedger(db);
                L.update(D.findings, seq);
                openShadows(db, D, L, seq);
                saveLedger(db, L, seq, now);
                saveSnapshot(db, seq, snap, now);
                return "SU2_DIAG_DONE:" + D.verdict + ":" + D.findings.size();
            });
        } catch (IOException | RuntimeException failure) {
            return "SU2_DIAG_FAILED:" + failure.getClass().getSimpleName() + ":" + failure.getMessage();
        }
    }

    /** The report export (read-only snapshot connection): {UPGRADE_DIAGNOSIS.md, UPGRADE_DIAGNOSIS.json}. */
    public static String[] report(SQLiteDatabase db) {
        try {
            String snap = null;
            try (Cursor c = db.rawQuery("SELECT snapshot FROM su2_diag_snapshot ORDER BY report_sequence DESC LIMIT 1", null)) { if (c.moveToFirst()) snap = c.getString(0); }
            if (snap == null) return UpgradeDiagnosisSU2.unavailableR30("INSUFFICIENT","su2_diag_snapshot 可读但没有已存快照；原信息探针尚未产生诊断");
            UpgradeDiagnosisSU2.Diagnosis D = UpgradeDiagnosisSU2.diagnose(UpgradeDiagnosisSU2.parse(snap));
            UpgradeDiagnosisSU2.Ledger L = loadLedger(db);
            return new String[]{UpgradeDiagnosisSU2.renderR30(D, L), UpgradeDiagnosisSU2.toJsonR30(D, L)};
        } catch (RuntimeException failure) {
            return UpgradeDiagnosisSU2.unavailableR30("UNKNOWN","诊断来源读取失败："+failure.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------------------------------------ sufficient statistics
    static void foldTrackBlocks(SQLiteDatabase db, long seq) {
        long hi = (seq / BLOCK) * BLOCK;
        db.execSQL("INSERT OR IGNORE INTO su2_track_block(member_id,block_index,periods,actions,hits,phat_sum,phat_n) "
            + "SELECT genome_key,sequence_ordinal/500,count(*),sum(act),sum(CASE WHEN act=1 THEN hit ELSE 0 END),coalesce(sum(CASE WHEN act=1 AND phat IS NOT NULL THEN phat END),0),"
            + "sum(CASE WHEN act=1 AND phat IS NOT NULL THEN 1 ELSE 0 END) FROM su2_live_decision d WHERE hit IS NOT NULL AND sequence_ordinal<? "
            + "AND sequence_ordinal/500>coalesce((SELECT folded_through FROM su2_track_cursor c WHERE c.member_id=d.genome_key),-1) "
            + "GROUP BY genome_key,sequence_ordinal/500", new Object[]{hi});
        db.execSQL("INSERT OR IGNORE INTO su2_track_block(member_id,block_index,periods,actions,hits,phat_sum,phat_n) "
            + "SELECT 'MAINLINE',d.sequence_ordinal/500,count(*),sum(d.act),sum(CASE WHEN d.act=1 THEN d.hit ELSE 0 END),coalesce(sum(CASE WHEN d.act=1 AND d.phat IS NOT NULL THEN d.phat END),0),"
            + "sum(CASE WHEN d.act=1 AND d.phat IS NOT NULL THEN 1 ELSE 0 END) FROM su2_live_decision d JOIN su2_league l ON l.genome_key=d.genome_key "
            + "WHERE d.hit IS NOT NULL AND d.sequence_ordinal<? AND d.sequence_ordinal/500>coalesce((SELECT folded_through FROM su2_track_cursor WHERE member_id='MAINLINE'),-1) "
            + "AND l.member_id=(SELECT c.member_id FROM su2_mainline_choice c WHERE c.from_sequence<=d.sequence_ordinal ORDER BY c.from_sequence DESC LIMIT 1) "
            + "GROUP BY d.sequence_ordinal/500", new Object[]{hi});
        long cut = seq / BLOCK - KEEP_BLOCKS;              // blocks below cut leave into the lifetime row (exact totals)
        db.execSQL("INSERT OR IGNORE INTO su2_track_block(member_id,block_index,periods,actions,hits,phat_sum,phat_n) "
            + "SELECT DISTINCT member_id,-1,0,0,0,0,0 FROM su2_track_block WHERE block_index>=0 AND block_index<?", new Object[]{cut});
        db.execSQL("UPDATE su2_track_block SET "
            + "periods=periods+(SELECT coalesce(sum(b.periods),0) FROM su2_track_block b WHERE b.member_id=su2_track_block.member_id AND b.block_index>=0 AND b.block_index<?),"
            + "actions=actions+(SELECT coalesce(sum(b.actions),0) FROM su2_track_block b WHERE b.member_id=su2_track_block.member_id AND b.block_index>=0 AND b.block_index<?),"
            + "hits=hits+(SELECT coalesce(sum(b.hits),0) FROM su2_track_block b WHERE b.member_id=su2_track_block.member_id AND b.block_index>=0 AND b.block_index<?),"
            + "phat_sum=phat_sum+(SELECT coalesce(sum(b.phat_sum),0) FROM su2_track_block b WHERE b.member_id=su2_track_block.member_id AND b.block_index>=0 AND b.block_index<?),"
            + "phat_n=phat_n+(SELECT coalesce(sum(b.phat_n),0) FROM su2_track_block b WHERE b.member_id=su2_track_block.member_id AND b.block_index>=0 AND b.block_index<?) "
            + "WHERE block_index=-1", new Object[]{cut, cut, cut, cut, cut});
        db.execSQL("INSERT OR REPLACE INTO su2_track_cursor(member_id,folded_through) SELECT member_id,? FROM su2_track_block WHERE block_index>=0 AND block_index<? GROUP BY member_id", new Object[]{cut-1,cut});
        db.execSQL("DELETE FROM su2_track_block WHERE block_index>=0 AND block_index<?", new Object[]{cut});
        // statistics of genomes that no longer exist at all (pruned research genomes) leave with them
        db.execSQL("DELETE FROM su2_track_block WHERE member_id<>'MAINLINE' AND member_id NOT IN (SELECT genome_key FROM su2_model_genome)");
        db.execSQL("DELETE FROM su2_track_cursor WHERE member_id<>'MAINLINE' AND member_id NOT IN (SELECT genome_key FROM su2_model_genome)");
    }

    /** P50 retention for the per-period tables and live states (each statement bounded, short transaction). */
    static void retention(SQLiteDatabase db, long seq, long now) {
        db.execSQL("DELETE FROM su2_live_daily WHERE day_key<?", new Object[]{seq / 1440 - DAILY_DAYS});
        db.execSQL("DELETE FROM su2_mainline_binding WHERE target_issue<(SELECT max(target_issue) FROM su2_mainline_binding)-?", new Object[]{BINDINGS});
        db.execSQL("DELETE FROM su2_mainline_choice WHERE from_sequence<(SELECT min(from_sequence) FROM (SELECT from_sequence FROM su2_mainline_choice ORDER BY from_sequence DESC LIMIT ?)) "
            + "AND member_id=(SELECT p.member_id FROM su2_mainline_choice p WHERE p.from_sequence<su2_mainline_choice.from_sequence ORDER BY p.from_sequence DESC LIMIT 1)", new Object[]{CHOICES});
        db.execSQL("DELETE FROM su2_certificate WHERE expires_at<?", new Object[]{now - CERT_KEEP_MS});
        db.execSQL("DELETE FROM su2_live_state WHERE owner NOT IN ('HUB','PROBE') AND substr(owner,1,4)<>'CAL:' "
            + "AND owner NOT IN (SELECT genome_key FROM su2_model_genome WHERE state IN ('LEAGUE','HALL_OF_FAME'))");
        db.execSQL("DELETE FROM su2_live_state WHERE substr(owner,1,4)='CAL:' AND through_sequence<(SELECT through_sequence FROM su2_live_state WHERE owner='HUB')");
        db.execSQL("DELETE FROM su2_diag_snapshot WHERE report_sequence NOT IN (SELECT report_sequence FROM su2_diag_snapshot ORDER BY report_sequence DESC LIMIT ?)", new Object[]{UpgradeDiagnosisSU2.SNAP_KEEP});
    }

    // ------------------------------------------------------------------------------------------------ snapshot from the database
    static String snapshot(SQLiteDatabase db, long seq, InformationProbeSU2 probes, ModelEvolutionSU2.State evo) {
        StringBuilder o = new StringBuilder("SU2DIAG\t1\ndataset\tLIVE\nseq\t").append(seq).append('\n');
        track(db, o, "MAINLINE", "MAINLINE");
        track(db, o, "XI", IncumbentLoopSU2.KEY);
        Map<String, String> champs = new LinkedHashMap<>();
        for (String tier : new String[]{"F15", "F30", "F100"}) {
            try (Cursor c = db.rawQuery("SELECT g.genome_key,g.formula FROM su2_model_genome g JOIN su2_league l ON l.genome_key=g.genome_key WHERE g.state='LEAGUE' AND g.tier=? "
                + "AND g.origin_op NOT IN ('INCUMBENT','STAGE') AND l.state='ACTIVE' AND l.name NOT LIKE '影子%' ORDER BY l.joined_sequence DESC LIMIT 1", new String[]{tier})) {
                if (c.moveToFirst()) { champs.put(tier, c.getString(0)); track(db, o, tier, c.getString(0)); o.append("formula\t").append(tier).append('\t').append(clean(c.getString(1), 240)).append('\n'); }
            }
        }
        o.append(StageStoreSU2.diagnosisLines(db));
        xiShare(db, o, seq);
        if (probes != null) {
            o.append(probes.snapshotLines());
            int refits = (int) Math.max(0, seq / BLOCK - (InformationProbeSU2.START + BLOCK) / BLOCK + 1);
            o.append(probes.contmatLine(refits, previousContmatMax(db)));
        }
        generations(db, o, seq);
        if (evo != null) {
            for (Map.Entry<String, double[]> e : evo.opStats.entrySet()) if (e.getValue()[0] > 0) o.append("op\t").append(e.getKey()).append('\t').append(e.getValue()[1] / e.getValue()[0]).append('\n');
            for (Map.Entry<String, double[]> e : evo.kindStats.entrySet()) if (e.getValue()[0] > 0) o.append("fam\t").append(e.getKey()).append('\t').append(e.getValue()[1] / e.getValue()[0]).append('\n');
            int n = 0, k = 0;
            for (String note : evo.repairNotes) {
                int a = note.indexOf("|parentDev="), b = note.indexOf("|childDev=");
                if (a < 0 || b < 0) continue;
                n++; if (Double.parseDouble(note.substring(b + 10)) > Double.parseDouble(note.substring(a + 11, b))) k++;
            }
            if (n > 0) o.append("repair\t").append(n).append('\t').append(k).append('\n');
        }
        try (Cursor c = db.rawQuery("SELECT category,measured_bytes,cap_bytes FROM su2_storage_budget WHERE measured_at>0 ORDER BY category", null)) {
            while (c.moveToNext()) o.append("storage\t").append(c.getString(0)).append('\t').append(c.getLong(1)).append('\t').append(c.getLong(2)).append('\n');
        }
        String s = o.toString();
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length > UpgradeDiagnosisSU2.SNAP_MAX_BYTES) { int nl = UpgradeDiagnosisSU2.lastNewline(b, UpgradeDiagnosisSU2.SNAP_MAX_BYTES); s = new String(b, 0, nl + 1, StandardCharsets.UTF_8); }
        return s;
    }

    static String clean(String s, int n) { s = s.replace('\t', ' ').replace('\n', ' '); return s.length() > n ? s.substring(0, n) : s; }

    static void track(SQLiteDatabase db, StringBuilder o, String name, String member) {
        long periods = 0; StringBuilder bl = new StringBuilder();
        try (Cursor c = db.rawQuery("SELECT periods,actions,hits FROM (SELECT block_index,periods,actions,hits FROM su2_track_block WHERE member_id=? AND block_index>=0 ORDER BY block_index DESC LIMIT "
            + KEEP_BLOCKS + ") ORDER BY block_index", new String[]{member})) {
            while (c.moveToNext()) { periods += c.getLong(0); if (bl.length() > 0) bl.append(','); bl.append(c.getLong(1)).append(':').append(c.getLong(2)); }
        }
        if (periods > 0) o.append("track\t").append(name).append('\t').append(periods).append('\t').append(bl).append('\n');
    }

    /** Share of the window's periods on XI, and the one-sided test "mainline worse than XI" (normal approximation). */
    static void xiShare(SQLiteDatabase db, StringBuilder o, long seq) {
        long from = Math.max(0, (seq / BLOCK - KEEP_BLOCKS) * BLOCK), onXi = 0, total = 0, cur = from; String member = null;
        try (Cursor c = db.rawQuery("SELECT member_id FROM su2_mainline_choice WHERE from_sequence<=? ORDER BY from_sequence DESC LIMIT 1", new String[]{String.valueOf(from)})) { if (c.moveToFirst()) member = c.getString(0); }
        try (Cursor c = db.rawQuery("SELECT from_sequence,member_id FROM su2_mainline_choice WHERE from_sequence>? AND from_sequence<? ORDER BY from_sequence", new String[]{String.valueOf(from), String.valueOf(seq)})) {
            while (c.moveToNext()) { long s = c.getLong(0); if (member != null) { total += s - cur; if (member.equals(IncumbentLoopSU2.MEMBER)) onXi += s - cur; } cur = s; member = c.getString(1); }
        }
        if (member != null) { total += seq - cur; if (member.equals(IncumbentLoopSU2.MEMBER)) onXi += seq - cur; }
        long[] m = totals(db, "MAINLINE"), x = totals(db, IncumbentLoopSU2.KEY);
        if (total <= 0 || m[0] == 0 || x[0] == 0) return;
        double rm = (double) m[1] / m[0], rx = (double) x[1] / x[0], p = (double) (m[1] + x[1]) / (m[0] + x[0]);
        double se = Math.sqrt(Math.max(1e-12, p * (1 - p) * (1.0 / m[0] + 1.0 / x[0])));
        o.append("xishare\t").append(onXi).append('\t').append(total).append('\t').append(phi((rm - rx) / se)).append('\n');
    }

    static long[] totals(SQLiteDatabase db, String member) {
        try (Cursor c = db.rawQuery("SELECT coalesce(sum(actions),0),coalesce(sum(hits),0) FROM su2_track_block WHERE member_id=? AND block_index>=0", new String[]{member})) {
            return c.moveToFirst() ? new long[]{c.getLong(0), c.getLong(1)} : new long[]{0, 0};
        }
    }

    /** Standard normal CDF (Abramowitz-Stegun 7.1.26, |error| < 1.5e-7). */
    static double phi(double z) {
        double x = Math.abs(z) / Math.sqrt(2), t = 1 / (1 + 0.3275911 * x);
        double erf = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return z >= 0 ? 0.5 * (1 + erf) : 0.5 * (1 - erf);
    }

    /** "一代比一代强": each new tier champion against the one it replaced, on the same live periods since it joined. */
    static void generations(SQLiteDatabase db, StringBuilder o, long seq) {
        for (String tier : new String[]{"F15", "F30"}) {
            List<long[]> js = new ArrayList<>(); List<String> keys = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT l.genome_key,l.joined_sequence FROM su2_league l JOIN su2_model_genome g ON g.genome_key=l.genome_key WHERE g.tier=? "
                + "AND g.origin_op<>'INCUMBENT' AND l.name NOT LIKE '影子%' ORDER BY l.joined_sequence", new String[]{tier})) {
                while (c.moveToNext()) { keys.add(c.getString(0)); js.add(new long[]{c.getLong(1)}); }
            }
            for (int i = Math.max(1, keys.size() - 6); i < keys.size(); i++) {
                long a = js.get(i)[0], b = i + 1 < keys.size() ? js.get(i + 1)[0] : seq;
                if (b - a < 100) continue;
                long[] old = rec(db, keys.get(i - 1), a, b), nu = rec(db, keys.get(i), a, b);
                o.append("gen\t").append(a).append('\t').append(tier).append('\t').append(old[0] > 0 ? String.valueOf((double) old[1] / old[0]) : "-").append('\t').append(old[0])
                 .append('\t').append(nu[0] > 0 ? String.valueOf((double) nu[1] / nu[0]) : "-").append('\t').append(nu[0]).append('\t').append(ModelFoundryJobSU2.GENERATIONS - 1).append('\n');
            }
        }
    }

    static long[] rec(SQLiteDatabase db, String genome, long a, long b) {
        try (Cursor c = db.rawQuery("SELECT coalesce(sum(act),0),coalesce(sum(CASE WHEN act=1 THEN hit ELSE 0 END),0) FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL",
            new String[]{genome, String.valueOf(a), String.valueOf(b)})) { return c.moveToFirst() ? new long[]{c.getLong(0), c.getLong(1)} : new long[]{0, 0}; }
    }

    static int previousContmatMax(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT snapshot FROM su2_diag_snapshot ORDER BY report_sequence DESC LIMIT 1", null)) {
            if (!c.moveToFirst()) return 0;
            for (String l : c.getString(0).split("\n")) if (l.startsWith("contmat\t")) return Integer.parseInt(l.split("\t", -1)[3]);
        }
        return 0;
    }

    // ------------------------------------------------------------------------------------------------ ledger, snapshots, shadows
    static long diagnosedSequence(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT diagnosed_sequence FROM su2_diag_state WHERE singleton=1", null)) { return c.moveToFirst() ? c.getLong(0) : -1; }
    }
    static UpgradeDiagnosisSU2.Ledger loadLedger(SQLiteDatabase db) {
        String text = "";
        try (Cursor c = db.rawQuery("SELECT ledger_text FROM su2_diag_state WHERE singleton=1", null)) { if (c.moveToFirst()) text = c.getString(0); }
        List<Object[]> snaps = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT report_sequence,snapshot FROM su2_diag_snapshot ORDER BY report_sequence", null)) { while (c.moveToNext()) snaps.add(new Object[]{c.getLong(0), c.getString(1)}); }
        return UpgradeDiagnosisSU2.Ledger.fromText(text, snaps);
    }
    static void saveLedger(SQLiteDatabase db, UpgradeDiagnosisSU2.Ledger L, long seq, long now) {
        db.execSQL("INSERT OR REPLACE INTO su2_diag_state(singleton,ledger_text,diagnosed_sequence,updated_at) VALUES(1,?,?,?)", new Object[]{L.text(), seq, now});
    }
    static void saveSnapshot(SQLiteDatabase db, long seq, String snap, long now) {
        db.execSQL("INSERT OR REPLACE INTO su2_diag_snapshot(report_sequence,snapshot,created_at) VALUES(?,?,?)", new Object[]{seq, snap, now});
        db.execSQL("DELETE FROM su2_diag_snapshot WHERE report_sequence NOT IN (SELECT report_sequence FROM su2_diag_snapshot ORDER BY report_sequence DESC LIMIT ?)", new Object[]{UpgradeDiagnosisSU2.SNAP_KEEP});
    }

    /** New genome-level prescriptions become shadow challengers (frozen, settled live like any league member). */
    static void openShadows(SQLiteDatabase db, UpgradeDiagnosisSU2.Diagnosis D, UpgradeDiagnosisSU2.Ledger L, long seq) {
        int active;
        try (Cursor c = db.rawQuery("SELECT count(*) FROM su2_diag_shadow WHERE state='ACTIVE'", null)) { active = c.moveToFirst() ? c.getInt(0) : 0; }
        for (UpgradeDiagnosisSU2.Finding f : D.findings) {
            if (active >= MAX_SHADOWS) break;
            UpgradeDiagnosisSU2.Entry e = L.entries.get(f.code + "|" + f.scope);
            if (e == null || e.since != seq || !"PENDING".equals(e.outcome)) continue;
            String tier = f.presc.startsWith("TIER_COVERAGE") ? f.presc.split(" ")[1] : "F15";
            String baseKey = null, baseText = null;
            try (Cursor c = db.rawQuery("SELECT g.genome_key,g.canonical_text FROM su2_model_genome g JOIN su2_league l ON l.genome_key=g.genome_key WHERE g.state='LEAGUE' AND g.tier=? "
                + "AND g.origin_op<>'STAGE' AND l.state='ACTIVE' AND l.name NOT LIKE '影子%' ORDER BY l.joined_sequence DESC LIMIT 1", new String[]{tier})) { if (c.moveToFirst()) { baseKey = c.getString(0); baseText = c.getString(1); } }
            String shadow = UpgradeDiagnosisSU2.shadowText(f.presc, baseText);
            if (shadow == null) continue;
            CommitteeSU2 sc = CommitteeSU2.parse(shadow);
            ModelStoreSU2.saveCommittee(db, sc, tier, "SHADOW", seq, -9, System.currentTimeMillis());
            ModelStoreSU2.joinLeague(db, "SU2:" + sc.key(), "SU2", "影子·" + f.code + (f.scope.isEmpty() ? "" : "·" + f.scope), sc.key(), seq);
            db.execSQL("INSERT OR REPLACE INTO su2_diag_shadow(issue_key,shadow_key,base_key,since_sequence,state) VALUES(?,?,?,?,'ACTIVE')", new Object[]{e.key(), sc.key(), baseKey, seq});
            active++;
        }
    }

    /** Shadow vs its base on the same settled periods; a harmful shadow is retired, an effective one stays for IPM. */
    static void settleShadows(SQLiteDatabase db) {
        List<String[]> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT issue_key,shadow_key,base_key,since_sequence FROM su2_diag_shadow WHERE state='ACTIVE'", null)) { while (c.moveToNext()) rows.add(new String[]{c.getString(0), c.getString(1), c.getString(2), String.valueOf(c.getLong(3))}); }
        if (rows.isEmpty()) return;
        UpgradeDiagnosisSU2.Ledger L = loadLedger(db); long seq = diagnosedSequence(db);
        for (String[] r : rows) {
            long since = Long.parseLong(r[3]);
            long[] s = rec(db, r[1], since, Long.MAX_VALUE), b = rec(db, r[2], since, Long.MAX_VALUE);
            UpgradeDiagnosisSU2.Entry e = L.entries.get(r[0]); if (e == null) continue;
            e.sacts = 0; e.shits = 0; e.bacts = 0; e.bhits = 0; L.shadow(r[0], s[0], s[1], b[0], b[1]);
            if (e.outcome.equals("HARMFUL") || e.outcome.equals("NO_EFFECT")) {     // judged: free the live slot (EFFECTIVE stays for IPM)
                db.execSQL("UPDATE su2_diag_shadow SET state='RETIRED' WHERE issue_key=?", new Object[]{r[0]});
                db.execSQL("UPDATE su2_league SET state='RETIRED' WHERE genome_key=?", new Object[]{r[1]});
                db.execSQL("UPDATE su2_model_genome SET state='RETIRED' WHERE genome_key=?", new Object[]{r[1]});
            }
        }
        saveLedger(db, L, seq, System.currentTimeMillis());
    }
}
