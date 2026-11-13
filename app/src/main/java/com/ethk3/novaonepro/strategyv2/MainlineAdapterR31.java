package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.*;
import java.util.*;

/** Adapts only the immutable R27 ACT that was actually frozen. The original
 * policy readout declares the other positions uniform; they convey no additional
 * certificate quality. This never predicts again or chooses a replacement K3. */
public final class MainlineAdapterR31 {
    private MainlineAdapterR31() {}
    public static final class Prepared {
        public final MainlineFreezeProofR31.Authority authority;public final V51Models.PositionAction[] actions;public final V51Models.PositionAction rankOne;public final String recipe;
        Prepared(MainlineFreezeProofR31.Authority authority,V51Models.PositionAction[] actions,V51Models.PositionAction head){this.authority=authority;this.actions=actions;rankOne=head;recipe=authority.recipe;}
    }
    public static Prepared prepare(DbStore store,V51Models.PredictionContext context,V51Models.PositionAction[] actions,String candidate,String certificate){
        if(store==null||context==null||!MainlineFreezeProofR31.candidate(candidate))return null;
        MainlineFreezeProofR31.Authority proof=MainlineFreezeProofR31.read(store.readable(),TargetIssueResolver.sequenceOrdinalSU1(context.targetIssue),System.currentTimeMillis());
        if(proof==null||!proof.candidate.equals(candidate)||!proof.certificate.equals(certificate)||!MainlineFreezeProofR31.allows(store,proof,context.targetIssue,proof.deadlineAt)||!TargetIssueResolver.exact(context.baseIssue,context.targetIssue).valid)return null;
        return rank(proof,actions);
    }
    static Prepared rank(MainlineFreezeProofR31.Authority proof,V51Models.PositionAction[] actions){
        K3ActionSpaceEngine.validateOrThrow(actions);double[] p=proof.probabilities();double[] mass=new double[actions.length];Integer[] order=new Integer[actions.length];int head=-1;
        int selected=K3ActionSpaceEngine.globalOrdinal(proof.position,proof.n1,proof.n2,proof.n3);
        for(int i=0;i<actions.length;i++){V51Models.PositionAction a=actions[i];order[i]=i;mass[i]=a.position==proof.position?p[a.n1-1]+p[a.n2-1]+p[a.n3-1]:.30d;if(K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3)==selected)head=i;}
        if(head<0)throw new IllegalStateException("R31_R27_FROZEN_HEAD_ABSENT");final int chosen=head;
        Arrays.sort(order,(a,b)->{if(a==chosen)return b==chosen?0:-1;if(b==chosen)return 1;int cmp=Double.compare(mass[b],mass[a]);return cmp!=0?cmp:Integer.compare(K3ActionSpaceEngine.globalOrdinal(actions[a].position,actions[a].n1,actions[a].n2,actions[a].n3),K3ActionSpaceEngine.globalOrdinal(actions[b].position,actions[b].n1,actions[b].n2,actions[b].n3));});
        V51Models.PositionAction[] out=new V51Models.PositionAction[actions.length];
        for(int rank=0;rank<order.length;rank++){int index=order[rank];V51Models.PositionAction a=actions[index];double margin=rank+1<order.length?Math.max(0,mass[index]-mass[order[rank+1]]):0;
            out[index]=new V51Models.PositionAction(a.position,a.n1,a.n2,a.n3,a.n4,mass[index],mass[index],margin,a.thirdMargin,a.entropy,a.stability,a.metaFeatures,a.fingerprint,rank+1,V51ChecksumEngine.sha256("R31_R27_EXACT|"+proof.proofHash+'|'+a.checksum+'|'+index+'|'+rank+'|'+Double.toHexString(mass[index])));
        }
        K3ActionSpaceEngine.validateOrThrow(out);return new Prepared(proof,out,out[chosen]);
    }
    public static boolean verifyFrozen(DbStore store,CertifiedPredictionR974.Proof proof,V51Models.PositionAction action){
        if(proof==null||!MainlineFreezeProofR31.candidate(proof.candidate))return false;
        MainlineFreezeProofR31.Authority current=MainlineFreezeProofR31.read(store.readable(),TargetIssueResolver.sequenceOrdinalSU1(proof.target),System.currentTimeMillis());
        if(current==null||!MainlineFreezeProofR31.allows(store,current,proof.target,proof.deadlineAt)||!current.candidate.equals(proof.candidate)||!current.policy.equals(proof.structure)||!current.certificate.equals(proof.certificate)||!current.proofHash.equals(proof.evidence)||!current.modelHash.equals(proof.modelHash)||!current.recipe.equals(proof.recipe)||!MainlineFreezeProofR31.SELECTOR.equals(proof.selectionHash)||!current.calibrationHash.equals(proof.calibrationHash)||Double.compare(current.selectedHitP,proof.selectedHitP)!=0)return false;
        return action==null||action.position==current.position&&action.n1==current.n1&&action.n2==current.n2&&action.n3==current.n3&&action.rank==1;
    }
}
