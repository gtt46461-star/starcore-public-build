package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * R930 batch-4 research lifecycle, fixed paired windows, structural/behavior de-duplication and
 * 8/24/64 fair predraw scheduler.  Scheduling never reads the current target label.
 */
public final class ResearchLifecycleV3R930 {
    public static final String REVISION="R930_LIFECYCLE_V3";
    public static final int TIER_A_CAP=8,TIER_B_CAP=24,TIER_C_CAP=64;
    public static final long DEFAULT_RESEARCH_BUDGET_MS=700L;
    public static final long MAX_SLICE_MS=250L;
    public static final int MAX_CHALLENGERS_PER_FAMILY=3;
    public static final int MIN_RESIDUAL_SUPPORT=120;
    public static final double HOMOGENEOUS_RESIDUAL_ABS_CORR=0.95d;
    private ResearchLifecycleV3R930(){}

    public enum State { IDEA,NURSERY,OBSERVING,EVIDENCE_READY,CHALLENGER,SHADOW,INFLUENCE,CHAMPION,PAUSED,QUARANTINED,QUALITY_PRUNED }
    public enum Tier { A_FULL_1200,B_SHARED_FULL_1200,C_PROXY_NURSERY }
    public enum Novelty { PARAMETER_VARIANT,STRUCTURAL_CHANGE,NEW_FAMILY }

    public static final class Candidate {
        public final String id,familyId,statisticalFamilyId,structureHash,behaviorHash,domainId,parentId;
        public final State state; public final int futureN,pairedN,ageIssues; public final double priority;
        public Candidate(String id,String familyId,String statisticalFamilyId,String structureHash,String behaviorHash,
                         String domainId,String parentId,State state,int futureN,int pairedN,int ageIssues,double priority){
            this.id=required(id);this.familyId=required(familyId);this.statisticalFamilyId=required(statisticalFamilyId);
            this.structureHash=hash(structureHash);this.behaviorHash=hash(behaviorHash);this.domainId=required(domainId);
            this.parentId=parentId==null?"":parentId.trim();this.state=state==null?State.IDEA:state;
            this.futureN=Math.max(0,futureN);this.pairedN=Math.max(0,pairedN);this.ageIssues=Math.max(0,ageIssues);
            this.priority=finite(priority)?priority:0.0d;
        }
        public boolean runnable(){return state!=State.QUARANTINED&&state!=State.QUALITY_PRUNED&&state!=State.CHAMPION;}
    }

    public static final class FixedWindow {
        public final String windowId,candidateHash,championBundleHash,selectionPolicyHash,domainSchemaHash;
        public final String startIssue,endIssue; public final int requiredPairs,lookId; public final boolean consumed;
        public FixedWindow(String candidateHash,String championBundleHash,String selectionPolicyHash,String domainSchemaHash,
                           String startIssue,String endIssue,int requiredPairs,int lookId,boolean consumed){
            this.candidateHash=hash(candidateHash);this.championBundleHash=hash(championBundleHash);
            this.selectionPolicyHash=hash(selectionPolicyHash);this.domainSchemaHash=hash(domainSchemaHash);
            this.startIssue=required(startIssue);this.endIssue=required(endIssue);this.requiredPairs=requiredPairs;this.lookId=lookId;this.consumed=consumed;
            if(requiredPairs<=0||lookId!=1||this.startIssue.compareTo(this.endIssue)>=0)throw new IllegalArgumentException("R930_WINDOW_CONTRACT");
            windowId=V51ChecksumEngine.sha256(REVISION+"|WINDOW|"+this.candidateHash+'|'+this.championBundleHash+'|'+this.selectionPolicyHash+'|'+this.domainSchemaHash+'|'+this.startIssue+'|'+this.endIssue+"|pairs="+requiredPairs+"|look=1");
        }
        public void requireCompatible(String candidate,String champion,String selection,String domain){
            if(!candidateHash.equals(hash(candidate))||!championBundleHash.equals(hash(champion))||!selectionPolicyHash.equals(hash(selection))||!domainSchemaHash.equals(hash(domain)))throw new IllegalStateException("R930_WINDOW_IDENTITY_CHANGED_NEW_WINDOW_REQUIRED");
        }
        public void requireSingleFinalLook(int currentPairs,String currentIssue){
            if(consumed)throw new IllegalStateException("R930_WINDOW_ALREADY_CONSUMED");
            if(currentPairs<requiredPairs||required(currentIssue).compareTo(endIssue)<0)throw new IllegalStateException("R930_PREMATURE_LOOK_FORBIDDEN");
        }
    }

    public static final class PairAccounting {
        public int both,candidateOnly,championOnly,bothMissing;
        public void add(boolean candidateFrozen,boolean championFrozen){if(candidateFrozen&&championFrozen)both++;else if(candidateFrozen)candidateOnly++;else if(championFrozen)championOnly++;else bothMissing++;}
        public int opportunities(){return both+candidateOnly+championOnly+bothMissing;}
        public String canonical(){return "both="+both+"|candidateOnly="+candidateOnly+"|championOnly="+championOnly+"|bothMissing="+bothMissing+"|opportunities="+opportunities();}
    }

    public static final class Scheduled {
        public final Candidate candidate; public final Tier tier; public final long sliceBudgetMs; public final String reason;
        Scheduled(Candidate c,Tier t,long slice,String reason){candidate=c;tier=t;sliceBudgetMs=slice;this.reason=reason;}
    }
    public static final class Schedule {
        public final String targetIssue,freezeHash; public final List<Scheduled> entries; public final long totalBudgetMs;
        Schedule(String issue,List<Scheduled> entries,long budget){targetIssue=issue;this.entries=Collections.unmodifiableList(entries);totalBudgetMs=budget;StringBuilder b=new StringBuilder(4096).append(REVISION).append('|').append(issue).append('|').append(budget);for(Scheduled s:entries)b.append('|').append(s.tier).append(':').append(s.candidate.id).append(':').append(s.sliceBudgetMs);freezeHash=V51ChecksumEngine.sha256(b.toString());}
    }

    /** Fair, deterministic, label-free schedule. Age/sample deficit/state/family quota are the only ranking inputs. */
    public static Schedule schedule(String targetIssue,List<Candidate> candidates,long requestedBudgetMs){
        String issue=required(targetIssue);long budget=Math.max(0,Math.min(DEFAULT_RESEARCH_BUDGET_MS,requestedBudgetMs<=0?DEFAULT_RESEARCH_BUDGET_MS:requestedBudgetMs));
        ArrayList<Candidate> pool=new ArrayList<Candidate>();if(candidates!=null)for(Candidate c:candidates)if(c!=null&&c.runnable())pool.add(c);
        Collections.sort(pool,new Comparator<Candidate>(){public int compare(Candidate a,Candidate b){double sa=scheduleScore(a),sb=scheduleScore(b);int c=Double.compare(sb,sa);if(c!=0)return c;c=a.statisticalFamilyId.compareTo(b.statisticalFamilyId);if(c!=0)return c;return a.id.compareTo(b.id);}});
        HashMap<String,Integer> familyUsed=new HashMap<String,Integer>();Set<String> seenStructure=new HashSet<String>();Set<String> seenBehavior=new HashSet<String>();
        ArrayList<Scheduled> out=new ArrayList<Scheduled>();int a=0,bb=0,c=0;long used=0;
        for(Candidate candidate:pool){
            if(seenStructure.contains(candidate.structureHash)||seenBehavior.contains(candidate.behaviorHash))continue;
            int usedFamily=familyUsed.containsKey(candidate.familyId)?familyUsed.get(candidate.familyId):0;
            if(usedFamily>MAX_CHALLENGERS_PER_FAMILY)continue;
            Tier tier=chooseTier(candidate,a,bb,c);if(tier==null)continue;
            long slice=Math.min(MAX_SLICE_MS,Math.max(40L,budget/Math.max(1,Math.min(pool.size(),TIER_A_CAP+TIER_B_CAP+TIER_C_CAP))));
            if(used+slice>budget)break;
            out.add(new Scheduled(candidate,tier,slice,"AGE_SAMPLE_DEFICIT_STATE_FAMILY_FAIRNESS"));used+=slice;
            if(tier==Tier.A_FULL_1200)a++;else if(tier==Tier.B_SHARED_FULL_1200)bb++;else c++;
            familyUsed.put(candidate.familyId,usedFamily+1);seenStructure.add(candidate.structureHash);seenBehavior.add(candidate.behaviorHash);
        }
        return new Schedule(issue,out,budget);
    }
    private static Tier chooseTier(Candidate x,int a,int b,int c){
        if((x.state==State.EVIDENCE_READY||x.state==State.CHALLENGER||x.state==State.SHADOW||x.state==State.INFLUENCE)&&a<TIER_A_CAP)return Tier.A_FULL_1200;
        if((x.state==State.OBSERVING||x.state==State.EVIDENCE_READY||x.state==State.CHALLENGER)&&b<TIER_B_CAP)return Tier.B_SHARED_FULL_1200;
        return c<TIER_C_CAP?Tier.C_PROXY_NURSERY:null;
    }
    private static double scheduleScore(Candidate c){
        double state=0;switch(c.state){case INFLUENCE:state=8;break;case SHADOW:state=7;break;case CHALLENGER:state=6;break;case EVIDENCE_READY:state=5;break;case OBSERVING:state=4;break;case NURSERY:state=3;break;default:state=1;}
        double deficit=Math.max(0,360-c.futureN)/360.0d;return c.ageIssues*.05d+deficit*4+state+c.priority;
    }

    /** Proxy C evidence cannot move a lifecycle beyond NURSERY. */
    public static State transition(State current,Tier evidenceTier,int futureN,int pairedN,boolean hardPass,boolean certificatePass){
        State s=current==null?State.IDEA:current;if(s==State.QUARANTINED||s==State.QUALITY_PRUNED)return s;
        if(!hardPass)return State.PAUSED;if(evidenceTier==Tier.C_PROXY_NURSERY)return s.ordinal()<State.NURSERY.ordinal()?State.NURSERY:s;
        if(futureN<120)return State.OBSERVING;if(!certificatePass)return State.EVIDENCE_READY;if(futureN<360||pairedN<240)return State.CHALLENGER;if(futureN<600)return State.SHADOW;if(futureN<1200)return State.INFLUENCE;return State.CHAMPION;
    }

    public static final class RepairChild {
        public final String childId,parentId,repairTarget,changedModules,atoms,expectedEffect,falsification;
        public final int inheritedFutureN=0,inheritedPairedN=0; public final String structureHash;
        RepairChild(String childId,String parentId,String repairTarget,String changedModules,String atoms,String expectedEffect,String falsification,String structureHash){this.childId=childId;this.parentId=parentId;this.repairTarget=repairTarget;this.changedModules=changedModules;this.atoms=atoms;this.expectedEffect=expectedEffect;this.falsification=falsification;this.structureHash=structureHash;}
    }
    public static RepairChild freshChild(String parentId,String parentStructure,String repairTarget,String changedModules,String atoms,String expectedEffect,String falsification){
        String canonical=REVISION+"|PARENT="+required(parentId)+"|BASE="+hash(parentStructure)+"|TARGET="+required(repairTarget)+"|MODULES="+required(changedModules)+"|ATOMS="+required(atoms)+"|EXPECT="+required(expectedEffect)+"|FALSIFY="+required(falsification);
        String structure=V51ChecksumEngine.sha256(canonical);return new RepairChild("CHILD-"+structure.substring(0,20),parentId,repairTarget,changedModules,atoms,expectedEffect,falsification,structure);
    }

    public static boolean structuralDuplicate(Candidate a,Candidate b){return a!=null&&b!=null&&a.structureHash.equals(b.structureHash);}
    public static boolean behaviorDuplicate(Candidate a,Candidate b){return a!=null&&b!=null&&a.behaviorHash.equals(b.behaviorHash);}
    public static boolean homogeneousResidual(double correlation,int commonFutureN,String leftDomain,String rightDomain){return commonFutureN>=MIN_RESIDUAL_SUPPORT&&finite(correlation)&&Math.abs(correlation)>HOMOGENEOUS_RESIDUAL_ABS_CORR&&required(leftDomain).equals(required(rightDomain));}

    /** Canonical DSL identity ignores cosmetic whitespace/case but preserves executable token order. */
    public static String structuralHash(String executableDsl){String s=required(executableDsl).replaceAll("\\s+","").toUpperCase(Locale.US);return V51ChecksumEngine.sha256("R930_DSL|"+s);}
    public static String behaviorHash(int[] rankOrdinals,String selectedHash){if(rankOrdinals==null||rankOrdinals.length!=1200)throw new IllegalArgumentException("R930_BEHAVIOR_1200");StringBuilder b=new StringBuilder(8192).append("R930_BEHAVIOR|").append(hash(selectedHash));boolean[] seen=new boolean[1200];for(int v:rankOrdinals){if(v<0||v>=1200||seen[v])throw new IllegalArgumentException("R930_BEHAVIOR_PERM");seen[v]=true;b.append(',').append(v);}return V51ChecksumEngine.sha256(b.toString());}

    public static String firstPruneReason(DbStore store,String candidateId,String proposed){if(store==null)return required(proposed);String key="r930_first_prune_"+V51ChecksumEngine.sha256(required(candidateId)).substring(0,20);String prior=store.getState(key,"");if(prior.length()>0)return prior;String reason=required(proposed);store.setState(key,reason);return reason;}
    public static String transitionEvent(DbStore store,String candidateId,State from,State to,String issue,String evidenceHash){String canonical=REVISION+"|"+required(candidateId)+'|'+from+'|'+to+'|'+required(issue)+'|'+hash(evidenceHash);String event=V51ChecksumEngine.sha256(canonical);if(store!=null){String key="r930_life_evt_"+event.substring(0,24);String prior=store.getState(key,"");if(prior.length()==0)store.setState(key,canonical);else if(!prior.equals(canonical))throw new IllegalStateException("R930_LIFECYCLE_EVENT_DRIFT");}return event;}

    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R930_REQUIRED");return v;}
    private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R930_HASH");return v;}
    private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
}
