package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/** R27-025 fusion F_STACK (docs/05 5.4): 2–5 archive members with pairwise prediction correlation &lt; 0.6, combined
 * by (a) weighted standardized scores with weights fitted on H_TRAIN, (b) a regime gate, or (c) rank voting whose
 * temperature is then fitted on H_TRAIN. The product is a new policy (new hash); members are unchanged and recorded
 * as parents (multi-parent lineage edges). */
public final class FusionOpsR27 {
    private FusionOpsR27() {}
    public static final double MAX_CORRELATION = 0.6;
    static final double[] WEIGHT_GRID = {0, 0.5, 1, 2};

    public static PolicySpecR27 fuse(List<PolicySpecR27> archive,EvalContextR27 ctx,StatsR27.SplitMix r,int gen){return fuse(archive,ctx,r,gen,null);}
    public static PolicySpecR27 fuse(List<PolicySpecR27> archive, EvalContextR27 ctx, StatsR27.SplitMix r, int gen,ResearchContextR30R1 research) {
        if(research!=null){research.begin();archive=research.fusionParents(archive,r);}
        if (archive.size() < 2) return null;
        PolicySpecR27 first = archive.get(r.nextInt(archive.size()));
        if(SetIntersectionPolicyR30.exact(first))return null;
        FailureProfileR30 profile = ctx.failureProfile(first);
        int tp = first.position; List<PolicySpecR27> members = new ArrayList<>(); members.add(first);
        int want = 2 + r.nextInt(4);
        for (int attempt = 0; attempt < 24 && members.size() < want; attempt++) {
            PolicySpecR27 c = archive.get(r.nextInt(archive.size())); boolean ok = c.position==tp&&!SetIntersectionPolicyR30.exact(c);
            if(!ok)continue;
            for (PolicySpecR27 m : members) if (m.policyHash().equals(c.policyHash()) || !FusionStudyR30R1.pair(m,c,ctx).complementary()) { ok = false; break; }
            if (ok) members.add(c);
        }
        if (members.size() < 2) return null;
        List<String> nominated=new ArrayList<>(),parents=new ArrayList<>();
        java.util.Map<String,String> excluded=new java.util.LinkedHashMap<>();
        for(PolicySpecR27 member:members)nominated.add(member.policyHash());
        double mode=r.nextDouble();
        double[] weights=null;
        String how;
        if(mode<0.5){
            weights=new double[members.size()];java.util.Arrays.fill(weights,1);
            for(int sweep=0;sweep<2;sweep++)for(int i=0;i<weights.length;i++){
                double best=weights[i],bestLoss=Double.POSITIVE_INFINITY;
                for(double candidate:WEIGHT_GRID){
                    weights[i]=candidate;
                    double loss=ctx.trainLogLoss(weighted(members,weights,first),1.0);
                    if(loss<bestLoss){bestLoss=loss;best=candidate;}
                }
                weights[i]=best;
            }
            List<PolicySpecR27> retained=new ArrayList<>();double[] active=new double[weights.length];
            for(int i=0;i<weights.length;i++){
                PolicySpecR27 member=members.get(i);
                if(weights[i]==0){excluded.put(member.policyHash(),"FITTED_ZERO_WEIGHT");continue;}
                active[retained.size()]=weights[i];retained.add(member);
            }
            members=retained;weights=java.util.Arrays.copyOf(active,members.size());
            how="TRAIN_WEIGHTED_ZSCORE";
        }else if(mode<0.75){
            for(int i=2;i<members.size();i++)excluded.put(members.get(i).policyHash(),"OUTSIDE_TWO_BRANCH_PROGRAM");
            members=new ArrayList<>(members.subList(0,2));how="REGIME_TWO_BRANCH";
        }else how="RANK_VOTE";
        if(members.size()<2)return null;
        for(PolicySpecR27 member:members)parents.add(member.policyHash());
        PolicyAstR27.Node ast=combineR341(members,weights,how,first);
        PolicySpecR27.Meta meta=new PolicySpecR27.Meta("I11","F_STACK",parents,gen,r.state,how+" | "+profile.reason());
        PolicySpecR27 child=versionR341(first,ast,meta);
        if(!FailureProfileR30.legal(child))return null;
        java.util.Map<String,Object> evidence=FusionStudyR30R1.comparison(members,child,ctx);
        if(evidence==null)return null;
        List<Object> ablation=new ArrayList<>();
        for(int i=0;i<members.size();i++){
            List<PolicySpecR27> subset=new ArrayList<>(members);
            String removed=subset.remove(i).policyHash();
            double[] remaining=null;
            if(weights!=null){
                remaining=new double[weights.length-1];
                System.arraycopy(weights,0,remaining,0,i);
                System.arraycopy(weights,i+1,remaining,i,remaining.length-i);
            }
            PolicySpecR27 without=versionR341(child,combineR341(subset,remaining,how,first),null);
            java.util.Map<String,Object> effect=FusionStudyR30R1.componentAblationR341(child,without,ctx);
            if(((Number)effect.get("selectionChanges")).intValue()==0)return null;
            effect.put("removedParent",removed);ablation.add(effect);
        }
        evidence.put("nominatedParents",nominated);evidence.put("executableParents",parents);
        evidence.put("excludedParents",excluded);evidence.put("fitWeights",weights);
        evidence.put("pairedComponentRemovals",ablation);
        PolicySpecR27 accepted=child.withMeta(new PolicySpecR27.Meta(meta.idea,meta.operator,meta.parents,meta.generation,
            meta.seed,meta.reason+" PAIRED_SELECT_R34="+JsonR27.write(evidence)));
        return research==null?accepted:research.annotate(accepted);
    }

    private static PolicySpecR27 versionR341(PolicySpecR27 basis,PolicyAstR27.Node program,PolicySpecR27.Meta meta){
        return new PolicySpecR27(program,basis.position,basis.rule,basis.threshold,basis.rate,1.0,true,basis.lr,basis.forget,meta);
    }

    private static PolicyAstR27.Node combineR341(List<PolicySpecR27> members,double[] weights,String how,PolicySpecR27 first){
        if(weights!=null)return weighted(members,weights,first).ast;
        if("REGIME_TWO_BRANCH".equals(how)){
            PolicyAstR27.Node left=PolicyAstR27.op("zscore",members.get(0).ast);
            if(members.size()==1)return left;
            return PolicyAstR27.op("if",PolicyAstR27.leaf("regime",128),PolicyAstR27.constant(0.1),left,
                    PolicyAstR27.op("zscore",members.get(1).ast));
        }
        PolicyAstR27.Node sum=PolicyAstR27.op("rank",members.get(0).ast);
        for(int i=1;i<members.size();i++)sum=PolicyAstR27.op("add",sum,PolicyAstR27.op("rank",members.get(i).ast));
        return sum;
    }

    static PolicySpecR27 weighted(List<PolicySpecR27> members, double[] w, PolicySpecR27 first) {
        PolicyAstR27.Node sum = null;
        for (int i = 0; i < members.size(); i++) {
            if (w[i] == 0) continue;
            PolicyAstR27.Node term = PolicyAstR27.op("mul", PolicyAstR27.constant(w[i]), PolicyAstR27.op("zscore", members.get(i).ast));
            sum = sum == null ? term : PolicyAstR27.op("add", sum, term);
        }
        if (sum == null) sum = PolicyAstR27.op("zscore", members.get(0).ast);
        return new PolicySpecR27(sum, first.position, first.rule, first.threshold, first.rate, 1.0, true, first.lr, first.forget, null);
    }
}
