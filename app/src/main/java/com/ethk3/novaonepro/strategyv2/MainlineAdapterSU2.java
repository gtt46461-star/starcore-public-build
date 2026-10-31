package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;
import java.util.*;

/**
 * Exact SU2 projection into the existing R974 certified lane (P30). Mirrors
 * MainlineAdapterSU1.prepare: the chosen league member's frozen ACT decision is the
 * rank-one action; the other 1,199 K3 actions are ranked by the same pooled table
 * that produced the decision; the shown probability is the certificate's calibrated
 * value. ABSTAIN or a missing table returns null: the mainline simply does not act.
 */
public final class MainlineAdapterSU2 {
    private MainlineAdapterSU2() {}
    public static final String CONTRACT = HashingSU1.text("SU2_MAINLINE_V1|LEAGUE_CHOICE|FROZEN_ACT_ONLY|SELECTIVE_CERT|PLATT_CAL|ONE_REAL_PER_PERIOD");

    /** Read-only final validation for the existing canonical REAL writer. */
    public static boolean verifyFrozen(DbStore store,android.database.sqlite.SQLiteDatabase db,CertifiedPredictionR974.Proof proof,V51Models.PositionAction action){
        return proof!=null&&action!=null&&matchesFrozenR30(store,db,proof.candidate,proof.certificate,
                proof.structure,proof.modelHash,proof.selectionHash,proof.calibrationHash,proof.target,
                proof.deadlineAt,proof.selectedHitP,action.position,action.n1,action.n2,action.n3);
    }
    static boolean matchesFrozenR30(DbStore store,android.database.sqlite.SQLiteDatabase db,String candidate,String certificate,
            String program,String model,String selector,String calibration,String target,long deadline,double probability,
            int position,int n1,int n2,int n3){
        try{
            if(!RealAuthoritySU2.allows(store,candidate,certificate,target,deadline))return false;
            String genome=candidate.substring(CertificateSU2.PREFIX.length());
            CertificateSU2.Authority a=CertificateSU2.read(db,genome,System.currentTimeMillis());
            if(a==null||!a.certificate.equals(certificate)||!a.program.equals(program)||!a.program.equals(model)||!CONTRACT.equals(selector))return false;
            String currentCalibration=V51ChecksumEngine.sha256("SU2_PLATT|"+Double.toHexString(a.a)+"|"+Double.toHexString(a.b));
            if(!currentCalibration.equals(calibration)||position<1||position>10||n1<1||n1>10||n2<1||n2>10||n3<1||n3>10||n1==n2||n1==n3||n2==n3)return false;
            int mask=(1<<(n1-1))|(1<<(n2-1))|(1<<(n3-1));
            try(Cursor c=db.rawQuery("SELECT position,number_mask,act,phat FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND hit IS NULL",new String[]{genome,String.valueOf(TargetIssueResolver.sequenceOrdinalSU1(target))})){
                if(!c.moveToFirst()||c.getInt(0)!=position||c.getInt(1)!=mask||c.getInt(2)!=1)return false;
                double phat=c.isNull(3)?Double.NaN:c.getDouble(3);
                double currentProbability=Double.isNaN(phat)?.30:new SelectiveCalibrationSU2.Platt(a.a,a.b).apply(EvidenceBlockR26.quantized(phat));
                return Double.compare(probability,currentProbability)==0;
            }
        }catch(RuntimeException unavailable){
            if(unavailable instanceof ResearchWorkBudgetR927.Yield)throw unavailable;
            return false;
        }
    }

    public static final class Prepared {
        public final CertificateSU2.Authority authority; public final V51Models.PositionAction[] actions; public final V51Models.PositionAction rankOne; public final String recipe; public final double selectedHitP;
        Prepared(CertificateSU2.Authority a, V51Models.PositionAction[] x, V51Models.PositionAction r, String recipe, double p) { authority = a; actions = x; rankOne = r; this.recipe = recipe; selectedHitP = p; }
    }

    public static Prepared prepare(DbStore store, V51Models.PredictionContext context, V51Models.PositionAction[] actions) {
        K3ActionSpaceEngine.validateOrThrow(actions);
        long seq = TargetIssueResolver.sequenceOrdinalSU1(context.targetIssue);
        String member = ModelStoreSU2.currentChoice(store.readable(), seq);
        boolean incumbent = member.startsWith("XI_INCUMBENT:");
        if (!member.startsWith("SU2:") && !incumbent) return null;
        String genome = member.substring(member.indexOf(':') + 1);
        CertificateSU2.Authority a = CertificateSU2.read(store.readable(), genome, System.currentTimeMillis());
        if (a == null) return null;
        int position, mask; double phat; boolean act;
        try (Cursor c = store.readable().rawQuery("SELECT position,number_mask,act,phat FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND hit IS NULL", new String[]{genome, String.valueOf(seq)})) {
            if (!c.moveToFirst()) return null;
            position = c.getInt(0); mask = c.getInt(1); act = c.getInt(2) == 1; phat = c.isNull(3) ? Double.NaN : c.getDouble(3);
        }
        if (!act) return null;                                             // the strategy waits for its window
        float[][] P = incumbent ? IncumbentLoopSU2.pendingTable(store.readable(),seq) : LivePoolR26.frozenTable(store.readable(),genome,seq);
        if(P==null&&!incumbent)P=ModelLiveLoopSU2.pendingTable(genome,seq);
        if (P == null) return null;                                         // never re-derive after the freeze
        int[] k = new int[3]; int j = 0; for (int n = 0; n < 10; n++) if ((mask & (1 << n)) != 0) k[j++] = n + 1;
        int headOrdinal = K3ActionSpaceEngine.globalOrdinal(position, k[0], k[1], k[2]);
        double[] mass = new double[1200]; int headIndex = -1;
        for (int i = 0; i < 1200; i++) {
            V51Models.PositionAction x = actions[i];
            if (K3ActionSpaceEngine.globalOrdinal(x.position, x.n1, x.n2, x.n3) == headOrdinal) headIndex = i;
            float[] p = P[x.position - 1];
            mass[i] = Math.rint(((double) p[x.n1 - 1] + p[x.n2 - 1] + p[x.n3 - 1]) * 1e9) / 1e9;
        }
        if (headIndex < 0) throw new IllegalStateException("SU2_HEAD_NOT_IN_ACTION_SPACE");
        final int hi = headIndex; Integer[] order = new Integer[1200]; for (int i = 0; i < 1200; i++) order[i] = i;
        Arrays.sort(order, (x, y) -> { if (x == hi) return y == hi ? 0 : -1; if (y == hi) return 1; int d = Double.compare(mass[y], mass[x]);
            return d != 0 ? d : Integer.compare(K3ActionSpaceEngine.globalOrdinal(actions[x].position, actions[x].n1, actions[x].n2, actions[x].n3), K3ActionSpaceEngine.globalOrdinal(actions[y].position, actions[y].n1, actions[y].n2, actions[y].n3)); });
        double p = Double.isNaN(phat) ? 0.30 : new SelectiveCalibrationSU2.Platt(a.a, a.b).apply(EvidenceBlockR26.quantized(phat));
        String recipe = "{\"contract\":\"" + CONTRACT + "\",\"certificate\":\"" + a.certificate + "\",\"genome\":\"" + genome + "\",\"program\":\"" + a.program + "\",\"target\":\"" + context.targetIssue + "\",\"position\":" + position + ",\"mask\":" + mask + "}";
        String digest = HashingSU1.text(recipe);
        V51Models.PositionAction[] out = new V51Models.PositionAction[1200];
        for (int rank = 0; rank < 1200; rank++) {
            int i = order[rank]; V51Models.PositionAction x = actions[i];
            out[i] = new V51Models.PositionAction(x.position, x.n1, x.n2, x.n3, x.n4, mass[i], mass[i], rank == 1199 ? 0 : Math.max(0, mass[i] - mass[order[rank + 1]]), x.thirdMargin, x.entropy, x.stability, x.metaFeatures, x.fingerprint, rank + 1,
                V51ChecksumEngine.sha256("CERTIFIED_EXACT_K3_R974|" + x.checksum + '|' + digest + '|' + i + '|' + rank + '|' + mass[i]));
        }
        K3ActionSpaceEngine.validateOrThrow(out);
        return new Prepared(a, out, out[headIndex], recipe, p);
    }
}
