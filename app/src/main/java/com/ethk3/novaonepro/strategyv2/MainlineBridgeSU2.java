package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/**
 * League -> mainline (P03/P27/P30).
 *
 *  1. Every BLOCK periods the mainline choice is re-made from settled live records
 *     (MainlinePolicySU2, IPM): out-of-sample records only (periods >= joined_sequence),
 *     the user's original cross-intersection method is the floor, and a challenger must
 *     beat the current member on the same periods (Bonferroni z-test + 2 of 3 sub-blocks).
 *  2. Each period the chosen member's frozen decision is read: ABSTAIN means the
 *     mainline does not act (a selective strategy is allowed to wait for its window);
 *     ACT is forwarded to the existing single DecisionGate / settlement owner only if
 *     the member holds a valid selective certificate (SelectiveEvidenceSU2);
 *     otherwise the choice is shown as SHADOW with the failing gate named.
 *  The absolute per-period probability floors of DecisionGateEngine (0.39-0.42) do
 *  not apply to this lane: they are unreachable for any calibrated K3 (P04).
 */
public final class MainlineBridgeSU2 {
    private MainlineBridgeSU2() {}
    public static final int BLOCK = 250, WINDOW = 4000, MIN_ACTS = 600;
    public static final double ROOT_ALPHA = 0.05, BLOCK_FLOOR = 0.28, MIN_COVERAGE = 0.15;

    static boolean[][] su1Record(SQLiteDatabase db, String strategy, long from, long to) {
        int n = (int) (to - from); boolean[] act = new boolean[n], hit = new boolean[n];
        try (Cursor c = db.rawQuery("SELECT sequence_ordinal,hit FROM su1_live_shadow WHERE strategy_id=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL",
            new String[]{strategy, String.valueOf(from), String.valueOf(to)})) {
            while (c.moveToNext()) { int i = (int) (c.getLong(0) - from); act[i] = true; hit[i] = c.getInt(1) == 1; }
        }
        return new boolean[][]{act, hit};
    }

    static List<MainlinePolicySU2.Member> members(SQLiteDatabase db, long from, long to) {
        List<MainlinePolicySU2.Member> out = new ArrayList<>();
        for (String[] m : ModelStoreSU2.league(db)) {
            boolean[][] r = m[1].equals("SU2") || m[1].equals("XI_INCUMBENT") ? ModelStoreSU2.record(db, m[3], from, to) : su1Record(db, m[0].substring(m[0].indexOf(':') + 1), from, to);
            int birth = (int) Math.max(0, Math.min(to - from, Long.parseLong(m[4]) - from));   // out-of-sample from joining
            out.add(new MainlinePolicySU2.Member(m[0], m[2], m[1].equals("XI_INCUMBENT"), r[0], r[1], birth));
        }
        return out;
    }

    /** Called after settlement of sequence seq-1; returns the active member id. */
    public static String chooseIfDue(DbStore store, long seq) {
        SQLiteDatabase r = store.readable();
        if (!StrategySchemaSU2.ready(r)) return "";
        String current = ModelStoreSU2.currentChoice(r, seq);
        if (!current.isEmpty() && seq % BLOCK != 0) return current;
        long from = seq - WINDOW;
        List<MainlinePolicySU2.Member> league = members(r, from, seq);
        if (league.isEmpty()) return current;
        // indices are relative to `from`; the policy scores [max(birth, 0), WINDOW)
        final MainlinePolicySU2.Choice ch = MainlinePolicySU2.choose(league, current, WINDOW, WINDOW);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_MAINLINE_CHOICE", db -> { ModelStoreSU2.recordChoice(db, seq, ch, from, System.currentTimeMillis()); return 0; });
        return ch.member.id;
    }

    public static final class Intent {
        public final String memberId, genomeKey, gate; public final int position, mask; public final double phat; public final boolean real;
        Intent(String m, String g, int p, int mask, double phat, boolean real, String gate) { memberId = m; genomeKey = g; position = p; this.mask = mask; this.phat = phat; this.real = real; this.gate = gate; }
    }

    /**
     * The mainline intent for target `seq`, or null when the chosen strategy abstains
     * (or has no frozen decision - MISSING is never turned into an action).
     */
    public static Intent intent(SQLiteDatabase db, long seq) {
        String member = ModelStoreSU2.currentChoice(db, seq);
        // SU2 members and the XI incumbent use this lane (XI's frozen pick IS the mainline pick when IPM keeps it);
        // selective SU1 members keep their existing lane
        if (!member.startsWith("SU2:") && !member.startsWith("XI_INCUMBENT:")) return null;
        String genome = member.substring(member.indexOf(':') + 1);
        int position, mask; double phat; boolean act;
        try (Cursor c = db.rawQuery("SELECT position,number_mask,act,phat FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND hit IS NULL", new String[]{genome, String.valueOf(seq)})) {
            if (!c.moveToFirst()) return null;
            position = c.getInt(0); mask = c.getInt(1); act = c.getInt(2) == 1; phat = c.isNull(3) ? Double.NaN : c.getDouble(3);
        }
        if (!act) return null;
        // REAL needs a valid selective certificate (issued by CertificateSU2.maintain from live evidence)
        CertificateSU2.Authority cert = CertificateSU2.read(db, genome, System.currentTimeMillis());
        return new Intent(member, genome, position, mask, phat, cert != null, cert != null ? "CERTIFIED:" + cert.certificate : "NO_VALID_SU2_CERTIFICATE_SHADOW_ONLY");
    }
}
