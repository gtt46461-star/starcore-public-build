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
        boolean currentQualified=false;long now=System.currentTimeMillis();
        if(current.startsWith("SU2:")||current.startsWith("XI_INCUMBENT:"))currentQualified=CertificateSU2.read(r,current.substring(current.indexOf(':')+1),now)!=null;
        if (currentQualified && seq % BLOCK != 0) return current;
        long from = seq - WINDOW;
        List<MainlinePolicySU2.Member> league = members(r, from, seq);
        if (league.isEmpty()) return current;
        List<MainlinePolicySU2.Member> qualified=new ArrayList<>();for(MainlinePolicySU2.Member m:league)if((m.id.startsWith("SU2:")||m.id.startsWith("XI_INCUMBENT:"))&&CertificateSU2.read(r,m.id.substring(m.id.indexOf(':')+1),now)!=null)qualified.add(m);
        if(!qualified.isEmpty())league=qualified;
        else if(!current.isEmpty()&&seq%BLOCK!=0)return current;
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
        // R27-042: an R27 shadow with an ACTIVE certificate (FUTURE e-process + unchanged quality gates) is the
        // first candidate, chosen by logE × calibration quality. The intent is still only a request: the single
        // DecisionGateEngine decides REAL under its unchanged rules (≤1 REAL per issue, no chase/doubling).
        Intent r27 = r27Intent(db, seq);
        if (r27 != null) return r27;
        if (!StrategySchemaSU2.ready(db)) return null;
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

    /** The R27 request query (also executed by tools/r27/test_r27_migration.py, H15): only a FROZEN, acting,
     * unsettled row of a shadow with an unexpired ACTIVE certificate and a QUALIFIED FUTURE e-process qualifies;
     * the (policy_hash, target_ordinal) key allows at most one row per period. */
    public static final String R27_INTENT_SQL = "SELECT f.position,f.n1,f.n2,f.n3,f.act,f.probabilities,f.status FROM r27_live_frozen f JOIN r27_certificate k ON k.policy_hash=f.policy_hash JOIN r27_live_shadow s ON s.policy_hash=f.policy_hash JOIN r27_eprocess e ON e.policy_hash=f.policy_hash"
            + " WHERE f.policy_hash=? AND f.target_ordinal=? AND f.hit=-1 AND k.status='ACTIVE' AND k.expires_at>? AND s.status='ACTIVE' AND e.state=? AND k.gate_version='"+CertificateGateR27.GATE_VERSION+"'";

    /** Read-only projection for the R30 explanation. Absent sources remain unknown, separate from failed gates. */
    public static final class ProjectionR30 {
        public String member="",candidate="",modelHash="",certificate="";
        public Boolean certificateActive,freezeComplete,modelAvailable,selectorConfirmed,actRuleFired;
        public final Map<String,Object> sources=new LinkedHashMap<>();
    }
    public static ProjectionR30 projectionR30(SQLiteDatabase db,long seq,long now) {
        return projectionR30(db,seq,now,null);
    }
    public static ProjectionR30 projectionR30(SQLiteDatabase db,long seq,long now,String actualCandidate) {
        ProjectionR30 out=new ProjectionR30();
        String choice=null;
        try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'",null)){
            if(c.moveToFirst()){Object x=((Map<?,?>)JsonR27.parse(c.getString(0))).get("choice");if(x instanceof String&&!((String)x).startsWith("PRODUCTION_FALLBACK"))choice=(String)x;}
        }catch(RuntimeException missing){out.sources.put("r27ChoiceSource","UNKNOWN:"+missing.getClass().getSimpleName());}
        if(choice!=null&&!choice.isEmpty()&&(actualCandidate==null||actualCandidate.equals(choice)||actualCandidate.equals("R27:"+choice))) {
            out.member="R27:"+choice;out.candidate=choice;out.sources.put("lane","R27_REQUEST_ONLY");
            out.sources.put("authorizationState","AWAITING_EXISTING_AUTHORIZATION_CHAIN");
            // This original lane records a request; it has not acquired a typed mainline authorization proof.
            out.selectorConfirmed=false;
            try(Cursor c=db.rawQuery("SELECT k.status,k.gate_version,k.issued_at,k.expires_at,s.base_policy,s.spec_json,s.semantic_abi,s.status,e.state FROM r27_live_shadow s LEFT JOIN r27_certificate k ON k.policy_hash=s.policy_hash LEFT JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE s.policy_hash=?",new String[]{choice})) {
                if(c.moveToFirst()) {
                    out.certificate=c.isNull(0)?"":choice;out.sources.put("certificateStatus",c.getString(0));out.sources.put("gateVersion",c.getString(1));out.sources.put("expiresAt",c.isNull(3)?null:c.getLong(3));
                    out.certificateActive="ACTIVE".equals(c.getString(0))&&CertificateGateR27.GATE_VERSION.equals(c.getString(1))&&c.getLong(2)<=now&&c.getLong(3)>now&&"ACTIVE".equals(c.getString(7))&&SequentialQualificationR27.QUALIFIED.equals(c.getString(8));
                    PolicySpecR27 spec=PolicySpecR27.parse(c.getString(5));out.modelHash=spec.policyHash();
                    boolean fingerprint=false;try(Cursor a=db.rawQuery("SELECT 1 FROM r27_semantic_fingerprint WHERE policy_hash=? AND build=? AND length(fingerprint)>0 AND length(operator_abi)>0",new String[]{choice,BuildConfig.SOURCE_COMMIT_R26})){fingerprint=a.moveToFirst();}
                    out.modelAvailable=out.modelHash.equals(c.getString(4))&&!c.isNull(6)&&!c.getString(6).isEmpty()&&fingerprint;
                    out.sources.put("modelReferenceKind","R27_POLICY_HASH_AND_CURRENT_SEMANTIC_FINGERPRINT");
                }else{out.certificateActive=false;out.modelAvailable=false;}
                try(Cursor f=db.rawQuery("SELECT status,act,frozen_at,deadline_at,hit FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{choice,String.valueOf(seq)})){
                    if(f.moveToFirst()){out.actRuleFired=f.getInt(1)==1;out.freezeComplete="FROZEN".equals(f.getString(0))&&f.getLong(3)-f.getLong(2)>=LiveShadowsR27.FREEZE_MARGIN_MS&&f.getLong(3)>now&&f.getInt(4)==-1;out.sources.put("frozenAt",f.getLong(2));out.sources.put("deadlineAt",f.getLong(3));}
                    else{out.freezeComplete=false;out.actRuleFired=null;out.sources.put("freezeReason","MISSING_ORIGINAL_FREEZE");}
                }
            }catch(RuntimeException missing){if(Boolean.TRUE.equals(out.freezeComplete))out.freezeComplete=null;out.sources.put("r27ProjectionError",missing.getClass().getSimpleName());}
            return out;
        }
        try {
            if(!StrategySchemaSU2.ready(db)){out.sources.put("su2Projection","UNKNOWN_SCHEMA_NOT_READY");return out;}
            String member=ModelStoreSU2.currentChoice(db,seq);out.member=member;
            if(!member.startsWith("SU2:")&&!member.startsWith("XI_INCUMBENT:")){out.certificateActive=false;out.selectorConfirmed=false;out.sources.put("su2Projection","NO_CHOSEN_MEMBER");return out;}
            String genome=member.substring(member.indexOf(':')+1);out.candidate=CertificateSU2.PREFIX+genome;out.sources.put("lane","EXISTING_SU2_CERTIFIED_LANE");
            CertificateSU2.Authority a=CertificateSU2.read(db,genome,now);out.certificateActive=a!=null;out.selectorConfirmed=a!=null;
            if(a!=null){out.certificate=a.certificate;out.modelHash=a.program;out.modelAvailable=true;out.sources.put("modelReferenceKind","SU2_CANONICAL_PROGRAM_HASH");out.sources.put("expiresAt",a.expires);}
            else out.modelAvailable=null;
            try(Cursor c=db.rawQuery("SELECT act,frozen_at,target_open_time,hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=?",new String[]{genome,String.valueOf(seq)})){
                if(!c.moveToFirst()){out.freezeComplete=false;out.sources.put("freezeReason","MISSING_ORIGINAL_FREEZE");return out;}
                out.actRuleFired=c.getInt(0)==1;boolean timely=c.getLong(2)>now&&c.getLong(2)-c.getLong(1)>=LiveShadowsR27.FREEZE_MARGIN_MS&&c.isNull(3);
                boolean table=member.startsWith("XI_INCUMBENT:")?IncumbentLoopSU2.pendingTable(db,seq)!=null:LivePoolR26.frozenTable(db,genome,seq)!=null;
                out.freezeComplete=timely&&table;out.sources.put("frozenAt",c.getLong(1));out.sources.put("deadlineAt",c.getLong(2));
                if(!member.startsWith("XI_INCUMBENT:"))try(Cursor f=db.rawQuery("SELECT source_generation,input_through FROM r26_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{genome,String.valueOf(seq)})){
                    out.freezeComplete=out.freezeComplete&&f.moveToFirst()&&f.getLong(0)==EventFrameBuilderSU1.generation(db)&&f.getLong(1)==seq-1;
                }
            }
        }catch(RuntimeException missing){if(Boolean.TRUE.equals(out.freezeComplete))out.freezeComplete=null;out.sources.put("su2ProjectionError",missing.getClass().getSimpleName());}
        return out;
    }

    static Intent r27Intent(SQLiteDatabase db, long seq) {
        String choice;
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'", null)) {
            if (!c.moveToFirst()) return null; Object v = ((Map<?, ?>) JsonR27.parse(c.getString(0))).get("choice"); choice = v == null ? null : v.toString();
        } catch (RuntimeException schemaPending) { return null; }
        if (choice == null || choice.startsWith("PRODUCTION_FALLBACK")) return null;
        long now = System.currentTimeMillis();
        try (Cursor c = db.rawQuery(R27_INTENT_SQL, new String[] {choice, String.valueOf(seq), String.valueOf(now), SequentialQualificationR27.QUALIFIED})) {
            if (!c.moveToFirst() || !"FROZEN".equals(c.getString(6)) || c.getInt(4) != 1) return null; // MISSING / abstain is never turned into an action
            int position = c.getInt(0), n1 = c.getInt(1), n2 = c.getInt(2), n3 = c.getInt(3); double[] p = StoreR27.unpack(c.getBlob(5));
            int mask = (1 << (n1 - 1)) | (1 << (n2 - 1)) | (1 << (n3 - 1));
            return new Intent("R27:" + choice, choice, position, mask, p[n1 - 1] + p[n2 - 1] + p[n3 - 1], true, "R27_CERTIFIED:" + choice);
        } catch (RuntimeException schemaPending) { return null; }
    }
}
